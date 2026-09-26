#!/usr/bin/env bash
# GCP setup for FoC CI/CD and Cloud Run. Idempotent: it only creates what is missing.
# Re-run it whenever a service is added: the service lists are derived from the repository,
# and CI's "Cloud infrastructure" check (scripts/ci/check-infra.sh) fails until it has run.
#
# Usage (with an owner-level gcloud account active):
#   infra/gcp/bootstrap.sh
#
# Creates, per docs/ci-cd.md:
#   - Artifact Registry docker repo                     ${AR_REPO}
#   - one runtime service account per service           foc-<service>@
#   - one Firestore database per service per env         <name>-<env>, only readable by foc-<service>@
#   - one config bucket per env (mounted into services)  ${PROJECT_ID}-foc-config-<env>
#   - deployer service account used by GitHub Actions    foc-deployer@
#   - read-only custom role focInfraReader (deployer), used by the CI infrastructure check
#   - Workload Identity Federation for the GitHub repo  (keyless: no JSON keys in GitHub)
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
# shellcheck source=SCRIPTDIR/project.env
source "$ROOT/infra/gcp/project.env"

ENVIRONMENTS=(staging production)
# Derived from the repository so nobody has to maintain lists (override with env vars):
# every service folder (it has a Dockerfile, even an empty placeholder) gets a runtime
# identity, and every service whose build uses the Firestore client gets its databases.
default_runtime=$(for dir in "$ROOT"/*/; do [[ -f "$dir/Dockerfile" ]] && basename "$dir"; done | tr '\n' ' ')
default_firestore=$("$ROOT/scripts/ci/list-services.sh" --firestore | tr '\n' ' ')
read -r -a RUNTIME_SERVICES <<<"${RUNTIME_SERVICES:-$default_runtime}"
read -r -a FIRESTORE_SERVICES <<<"${FIRESTORE_SERVICES:-$default_firestore}"
echo "Runtime services:   ${RUNTIME_SERVICES[*]}"
echo "Firestore services: ${FIRESTORE_SERVICES[*]:-none}"

POOL_ID=github
PROVIDER_ID=github-actions
DEPLOYER_ID=${DEPLOYER_SA%%@*}

gc() { gcloud --project "$PROJECT_ID" --quiet "$@"; }
exists() { "$@" >/dev/null 2>&1; }
log() { printf '\n==> %s\n' "$*"; }
runtime_sa() { echo "foc-$1@${PROJECT_ID}.iam.gserviceaccount.com"; }
# Retries transient IAM failures: per-minute service-account quotas and concurrent
# policy edits (ETag conflicts) are both common right after APIs/databases are created.
retry() {
  local attempt
  for attempt in 1 2 3 4 5; do
    "$@" && return 0
    echo "  retrying in $((attempt * 15))s (attempt $attempt/5)"
    sleep $((attempt * 15))
  done
  return 1
}
create_sa() { retry gc iam service-accounts create "$@"; }
project_binding() { retry gc projects add-iam-policy-binding "$PROJECT_ID" "$@" >/dev/null; }
config_bucket() { echo "${PROJECT_ID}-foc-config-$1"; }

log "Enabling APIs"
gc services enable \
  run.googleapis.com artifactregistry.googleapis.com firestore.googleapis.com \
  iam.googleapis.com iamcredentials.googleapis.com sts.googleapis.com \
  secretmanager.googleapis.com cloudresourcemanager.googleapis.com storage.googleapis.com

log "Artifact Registry repository: $AR_REPO ($REGION)"
exists gc artifacts repositories describe "$AR_REPO" --location "$REGION" ||
  gc artifacts repositories create "$AR_REPO" --location "$REGION" \
    --repository-format docker --description "FoC container images"

log "Runtime service accounts"
for svc in "${RUNTIME_SERVICES[@]}"; do
  exists gc iam service-accounts describe "$(runtime_sa "$svc")" ||
    create_sa "foc-$svc" --display-name "FoC $svc (Cloud Run runtime)"
done

log "Firestore databases (database-per-service)"
for svc in ${FIRESTORE_SERVICES[@]+"${FIRESTORE_SERVICES[@]}"}; do
  name=${svc%-service}
  for env in "${ENVIRONMENTS[@]}"; do
    db="$name-$env"
    if ! exists gc firestore databases describe --database "$db"; then
      protection=()
      [[ $env == production ]] && protection=(--delete-protection)
      gc firestore databases create --database "$db" --location "$REGION" --type firestore-native ${protection[@]+"${protection[@]}"}
    fi
    # Only this service's identity may read/write its database.
    project_binding --member "serviceAccount:$(runtime_sa "$svc")" --role roles/datastore.user \
      --condition "expression=resource.name == \"projects/$PROJECT_ID/databases/$db\",title=firestore-$db"
  done
done

log "Config buckets (files mounted into Cloud Run, e.g. seed data)"
for env in "${ENVIRONMENTS[@]}"; do
  bucket=$(config_bucket "$env")
  exists gcloud storage buckets describe "gs://$bucket" ||
    gcloud storage buckets create "gs://$bucket" --project "$PROJECT_ID" --location "$REGION" \
      --uniform-bucket-level-access --public-access-prevention
  for svc in "${RUNTIME_SERVICES[@]}"; do
    gcloud storage buckets add-iam-policy-binding "gs://$bucket" \
      --member "serviceAccount:$(runtime_sa "$svc")" --role roles/storage.objectViewer >/dev/null
  done
done

log "Deployer service account: $DEPLOYER_SA"
exists gc iam service-accounts describe "$DEPLOYER_SA" ||
  create_sa "$DEPLOYER_ID" --display-name "FoC GitHub Actions deployer"
project_binding --member "serviceAccount:$DEPLOYER_SA" --role roles/run.admin --condition None
# repoAdmin (not just writer): promoting a build moves the <environment> tag, which deletes the old tag.
gc artifacts repositories add-iam-policy-binding "$AR_REPO" --location "$REGION" \
  --member "serviceAccount:$DEPLOYER_SA" --role roles/artifactregistry.repoAdmin >/dev/null
for svc in "${RUNTIME_SERVICES[@]}"; do
  # Lets the deployer run Cloud Run revisions as each runtime identity.
  gc iam service-accounts add-iam-policy-binding "$(runtime_sa "$svc")" \
    --member "serviceAccount:$DEPLOYER_SA" --role roles/iam.serviceAccountUser >/dev/null
done
for env in "${ENVIRONMENTS[@]}"; do
  gcloud storage buckets add-iam-policy-binding "gs://$(config_bucket "$env")" \
    --member "serviceAccount:$DEPLOYER_SA" --role roles/storage.objectAdmin >/dev/null
done

log "Read-only infrastructure role for the CI check (scripts/ci/check-infra.sh)"
reader_permissions="iam.serviceAccounts.get,iam.serviceAccounts.getIamPolicy,datastore.databases.getMetadata,datastore.databases.list,resourcemanager.projects.getIamPolicy,storage.buckets.get"
if exists gc iam roles describe focInfraReader; then
  gc iam roles update focInfraReader --permissions "$reader_permissions" >/dev/null
else
  gc iam roles create focInfraReader --title "FoC infrastructure reader" \
    --description "Read-only checks that every service's cloud resources exist" \
    --permissions "$reader_permissions" --stage GA >/dev/null
fi
project_binding --member "serviceAccount:$DEPLOYER_SA" --role "projects/$PROJECT_ID/roles/focInfraReader" --condition None

log "Workload Identity Federation for GitHub repo $GITHUB_REPO"
exists gc iam workload-identity-pools describe "$POOL_ID" --location global ||
  gc iam workload-identity-pools create "$POOL_ID" --location global --display-name "GitHub Actions"
exists gc iam workload-identity-pools providers describe "$PROVIDER_ID" --location global --workload-identity-pool "$POOL_ID" ||
  gc iam workload-identity-pools providers create-oidc "$PROVIDER_ID" --location global \
    --workload-identity-pool "$POOL_ID" --display-name "FoC GitHub repository" \
    --issuer-uri "https://token.actions.githubusercontent.com" \
    --attribute-mapping "google.subject=assertion.sub,attribute.repository=assertion.repository,attribute.ref=assertion.ref,attribute.environment=assertion.environment" \
    --attribute-condition "assertion.repository == '$GITHUB_REPO'"
gc iam service-accounts add-iam-policy-binding "$DEPLOYER_SA" --role roles/iam.workloadIdentityUser \
  --member "principalSet://iam.googleapis.com/projects/$PROJECT_NUMBER/locations/global/workloadIdentityPools/$POOL_ID/attribute.repository/$GITHUB_REPO" \
  >/dev/null

log "Done"
cat <<EOF
Workload identity provider: projects/$PROJECT_NUMBER/locations/global/workloadIdentityPools/$POOL_ID/providers/$PROVIDER_ID
Deployer service account:   $DEPLOYER_SA
Registry:                   $REGION-docker.pkg.dev/$PROJECT_ID/$AR_REPO
(These match infra/gcp/project.env; update that file if you changed any defaults.)
EOF
