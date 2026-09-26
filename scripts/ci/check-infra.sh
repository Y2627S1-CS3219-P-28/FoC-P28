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
