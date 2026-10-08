#!/usr/bin/env bash
# Verifies that every deployable service has the cloud resources it needs, so a missing piece
# fails a pull request instead of a deploy. Read-only: it never creates or changes anything.
#
# Usage: scripts/ci/check-infra.sh [environment ...]   (default: staging production)
#
# Per service (scripts/ci/list-services.sh):
#   - runtime identity foc-<service>@ exists, and the deployer may deploy as it
# Per Firestore service (list-services.sh --firestore) and environment:
#   - database <name>-<environment> exists
#   - foc-<service>@ has datastore.user on exactly that database
# Per environment:
#   - config bucket ${PROJECT_ID}-foc-config-<environment> exists
# Order Service Cloud SQL:
#   - shared instance ${CLOUD_SQL_INSTANCE} exists
#   - databases ${CLOUD_SQL_STAGING_DATABASE} and ${CLOUD_SQL_PRODUCTION_DATABASE} exist
#   - order-service runtime identity has roles/cloudsql.client
# Credit Service Cloud SQL:
#   - per-environment databases and users exist on the shared instance
#   - credit-service runtime identity can connect and read its password secrets
#
# Anything missing is fixed by the CI/CD owner re-running infra/gcp/bootstrap.sh, which
# derives the same service lists from the repository.
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "$ROOT"
# shellcheck source=SCRIPTDIR/../../infra/gcp/project.env
source infra/gcp/project.env

environments=("$@")
[[ ${#environments[@]} -gt 0 ]] || environments=(staging production)

services=()
while IFS= read -r svc; do [[ -n $svc ]] && services+=("$svc"); done < <(scripts/ci/list-services.sh)
firestore_services=()
while IFS= read -r svc; do [[ -n $svc ]] && firestore_services+=("$svc"); done < <(scripts/ci/list-services.sh --firestore)

problems=()
problem() { problems+=("$1"); echo "  ✗ $1"; }
ok() { echo "  ✓ $1"; }
runtime_sa() { echo "foc-$1@${PROJECT_ID}.iam.gserviceaccount.com"; }
sql_database() { [[ $1 == staging ]] && echo "$CLOUD_SQL_STAGING_DATABASE" || echo "$CLOUD_SQL_PRODUCTION_DATABASE"; }
sql_secret() { [[ $1 == staging ]] && echo "$CLOUD_SQL_STAGING_SECRET" || echo "$CLOUD_SQL_PRODUCTION_SECRET"; }
credit_sql_database() { [[ $1 == staging ]] && echo "$CREDIT_SQL_STAGING_DATABASE" || echo "$CREDIT_SQL_PRODUCTION_DATABASE"; }
credit_sql_user() { [[ $1 == staging ]] && echo "$CREDIT_SQL_STAGING_USER" || echo "$CREDIT_SQL_PRODUCTION_USER"; }
credit_sql_secret() { [[ $1 == staging ]] && echo "$CREDIT_SQL_STAGING_SECRET" || echo "$CREDIT_SQL_PRODUCTION_SECRET"; }

echo "Service identities"
for svc in "${services[@]}"; do
  sa=$(runtime_sa "$svc")
  if ! gcloud iam service-accounts describe "$sa" --project "$PROJECT_ID" >/dev/null 2>&1; then
    problem "$svc: runtime identity $sa does not exist"
    continue
  fi
  if gcloud iam service-accounts get-iam-policy "$sa" --project "$PROJECT_ID" --format json |
    jq -e --arg m "serviceAccount:$DEPLOYER_SA" \
      '.bindings // [] | any(.role == "roles/iam.serviceAccountUser" and (.members | index($m)))' >/dev/null; then
    ok "$svc: $sa (deployer may deploy as it)"
  else
    problem "$svc: the deployer is not allowed to deploy as $sa"
  fi
done

policy=$(gcloud projects get-iam-policy "$PROJECT_ID" --format json)
echo "Firestore databases"
for svc in ${firestore_services[@]+"${firestore_services[@]}"}; do
  name=${svc%-service}
  for env in "${environments[@]}"; do
    db="$name-$env"
    if ! gcloud firestore databases describe --database "$db" --project "$PROJECT_ID" >/dev/null 2>&1; then
      problem "$svc: Firestore database '$db' does not exist"
      continue
    fi
    if jq -e --arg m "serviceAccount:$(runtime_sa "$svc")" --arg db "projects/$PROJECT_ID/databases/$db" \
      '.bindings // [] | any(.role == "roles/datastore.user" and (.members | index($m))
        and ((.condition.expression // "") | contains($db)))' <<<"$policy" >/dev/null; then
      ok "$svc: $db (access granted)"
    else
      problem "$svc: $(runtime_sa "$svc") has no access to Firestore database '$db'"
    fi
  done
done

echo "Order Service Cloud SQL"
if ! gcloud sql instances describe "$CLOUD_SQL_INSTANCE" --project "$PROJECT_ID" >/dev/null 2>&1; then
  problem "order-service: Cloud SQL instance '$CLOUD_SQL_INSTANCE' does not exist"
else
  ok "order-service: Cloud SQL instance '$CLOUD_SQL_INSTANCE'"
  for env in staging production; do
    db=$(sql_database "$env")
    if gcloud sql databases describe "$db" --instance "$CLOUD_SQL_INSTANCE" --project "$PROJECT_ID" >/dev/null 2>&1; then
      ok "order-service: Cloud SQL database '$db'"
    else
      problem "order-service: Cloud SQL database '$db' does not exist"
    fi
  done
  if jq -e --arg m "serviceAccount:$(runtime_sa order-service)" \
    '.bindings // [] | any(.role == "roles/cloudsql.client" and (.members | index($m)))' <<<"$policy" >/dev/null; then
    ok "order-service: $(runtime_sa order-service) has roles/cloudsql.client"
  else
    problem "order-service: $(runtime_sa order-service) has no roles/cloudsql.client"
  fi
  for env in staging production; do
    secret=$(sql_secret "$env")
    if ! gcloud secrets describe "$secret" --project "$PROJECT_ID" >/dev/null 2>&1; then
      problem "order-service: Secret Manager secret '$secret' does not exist"
      continue
    fi
    secret_policy=$(gcloud secrets get-iam-policy "$secret" --project "$PROJECT_ID" --format json)
    if jq -e --arg m "serviceAccount:$(runtime_sa order-service)" \
      '.bindings // [] | any(.role == "roles/secretmanager.secretAccessor" and (.members | index($m)))' <<<"$secret_policy" >/dev/null; then
      ok "order-service: $(runtime_sa order-service) can access secret '$secret'"
    else
      problem "order-service: $(runtime_sa order-service) cannot access secret '$secret'"
    fi
  done
fi

echo "Credit Service Cloud SQL"
if ! gcloud sql instances describe "$CLOUD_SQL_INSTANCE" --project "$PROJECT_ID" >/dev/null 2>&1; then
  problem "credit-service: Cloud SQL instance '$CLOUD_SQL_INSTANCE' does not exist"
else
  ok "credit-service: Cloud SQL instance '$CLOUD_SQL_INSTANCE'"
  for env in "${environments[@]}"; do
    db=$(credit_sql_database "$env")
    if gcloud sql databases describe "$db" --instance "$CLOUD_SQL_INSTANCE" \
      --project "$PROJECT_ID" >/dev/null 2>&1; then
      ok "credit-service: Cloud SQL database '$db'"
    else
      problem "credit-service: Cloud SQL database '$db' does not exist"
    fi
    user=$(credit_sql_user "$env")
    if gcloud sql users list --instance "$CLOUD_SQL_INSTANCE" --project "$PROJECT_ID" \
      --filter="name=$user" --format='value(name)' | grep -Fxq "$user"; then
      ok "credit-service: Cloud SQL user '$user'"
    else
      problem "credit-service: Cloud SQL user '$user' does not exist"
    fi
  done
  if jq -e --arg m "serviceAccount:$(runtime_sa credit-service)" \
    '.bindings // [] | any(.role == "roles/cloudsql.client" and (.members | index($m)))' \
    <<<"$policy" >/dev/null; then
    ok "credit-service: $(runtime_sa credit-service) has roles/cloudsql.client"
  else
    problem "credit-service: $(runtime_sa credit-service) has no roles/cloudsql.client"
  fi
  for env in "${environments[@]}"; do
    secret=$(credit_sql_secret "$env")
    if ! gcloud secrets describe "$secret" --project "$PROJECT_ID" >/dev/null 2>&1; then
      problem "credit-service: Secret Manager secret '$secret' does not exist"
      continue
    fi
    if gcloud secrets versions list "$secret" --project "$PROJECT_ID" \
      --filter='state=ENABLED' --format='value(name)' | grep -q .; then
      ok "credit-service: Secret Manager secret '$secret' has an enabled password version"
    else
      problem "credit-service: Secret Manager secret '$secret' has no enabled password version"
    fi
    secret_policy=$(gcloud secrets get-iam-policy "$secret" --project "$PROJECT_ID" --format json)
    if jq -e --arg m "serviceAccount:$(runtime_sa credit-service)" \
      '.bindings // [] | any(.role == "roles/secretmanager.secretAccessor" and (.members | index($m)))' \
      <<<"$secret_policy" >/dev/null; then
      ok "credit-service: $(runtime_sa credit-service) can access secret '$secret'"
    else
      problem "credit-service: $(runtime_sa credit-service) cannot access secret '$secret'"
    fi
  done
fi

echo "Config buckets"
for env in "${environments[@]}"; do
  bucket="${PROJECT_ID}-foc-config-$env"
  if gcloud storage buckets describe "gs://$bucket" --project "$PROJECT_ID" >/dev/null 2>&1; then
    ok "gs://$bucket"
  else
    problem "config bucket gs://$bucket does not exist"
  fi
done

if [[ ${#problems[@]} -gt 0 ]]; then
  echo
  echo "::error title=Cloud infrastructure incomplete::${#problems[@]} problem(s). Ask the CI/CD owner to run infra/gcp/bootstrap.sh (it provisions every service in this repo), then re-run this check."
  if [[ -n ${GITHUB_STEP_SUMMARY:-} ]]; then
    {
      echo "### Cloud infrastructure incomplete"
      printf -- '- %s\n' "${problems[@]}"
      echo
      echo "Fix: the CI/CD owner runs \`infra/gcp/bootstrap.sh\`, then re-run this job."
    } >>"$GITHUB_STEP_SUMMARY"
  fi
  exit 1
fi
echo
echo "All cloud infrastructure is in place for: ${environments[*]}"
