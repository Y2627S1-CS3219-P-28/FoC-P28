#!/usr/bin/env bash
# One-time GCP setup for FoC CI/CD and Cloud Run. Idempotent: it only creates what is
# missing, so re-run it after adding a service to FIRESTORE_SERVICES / RUNTIME_SERVICES.
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
#   - Workload Identity Federation for the GitHub repo  (keyless: no JSON keys in GitHub)
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
# shellcheck source=SCRIPTDIR/project.env
source "$ROOT/infra/gcp/project.env"

ENVIRONMENTS=(staging production)
# Services that own a Firestore database. Add yours here, then re-run.
read -r -a FIRESTORE_SERVICES <<<"${FIRESTORE_SERVICES:-supplier-service}"
# Services deployed to Cloud Run (each gets a least-privilege runtime identity).
read -r -a RUNTIME_SERVICES <<<"${RUNTIME_SERVICES:-gateway frontend user-service supplier-service order-service credit-service admin-service}"

POOL_ID=github
PROVIDER_ID=github-actions
DEPLOYER_ID=${DEPLOYER_SA%%@*}

gc() { gcloud --project "$PROJECT_ID" --quiet "$@"; }
exists() { "$@" >/dev/null 2>&1; }
log() { printf '\n==> %s\n' "$*"; }
runtime_sa() { echo "foc-$1@${PROJECT_ID}.iam.gserviceaccount.com"; }
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
    gc iam service-accounts create "foc-$svc" --display-name "FoC $svc (Cloud Run runtime)"
done

log "Firestore databases (database-per-service)"
for svc in "${FIRESTORE_SERVICES[@]}"; do
  name=${svc%-service}
  for env in "${ENVIRONMENTS[@]}"; do
    db="$name-$env"
    if ! exists gc firestore databases describe --database "$db"; then
      protection=()
      [[ $env == production ]] && protection=(--delete-protection)
      gc firestore databases create --database "$db" --location "$REGION" --type firestore-native "${protection[@]}"
    fi
    # Only this service's identity may read/write its database.
    gc projects add-iam-policy-binding "$PROJECT_ID" \
      --member "serviceAccount:$(runtime_sa "$svc")" --role roles/datastore.user \
      --condition "expression=resource.name == \"projects/$PROJECT_ID/databases/$db\",title=firestore-$db" \
      >/dev/null
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
  gc iam service-accounts create "$DEPLOYER_ID" --display-name "FoC GitHub Actions deployer"
gc projects add-iam-policy-binding "$PROJECT_ID" \
  --member "serviceAccount:$DEPLOYER_SA" --role roles/run.admin --condition None >/dev/null
gc artifacts repositories add-iam-policy-binding "$AR_REPO" --location "$REGION" \
  --member "serviceAccount:$DEPLOYER_SA" --role roles/artifactregistry.writer >/dev/null
for svc in "${RUNTIME_SERVICES[@]}"; do
  # Lets the deployer run Cloud Run revisions as each runtime identity.
  gc iam service-accounts add-iam-policy-binding "$(runtime_sa "$svc")" \
    --member "serviceAccount:$DEPLOYER_SA" --role roles/iam.serviceAccountUser >/dev/null
done
for env in "${ENVIRONMENTS[@]}"; do
  gcloud storage buckets add-iam-policy-binding "gs://$(config_bucket "$env")" \
    --member "serviceAccount:$DEPLOYER_SA" --role roles/storage.objectAdmin >/dev/null
done

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
