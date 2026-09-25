#!/usr/bin/env bash
# Deploys services at one image tag to one environment on Cloud Run.
#
# Usage: scripts/ci/deploy.sh <staging|production> <image-tag> [service ...]
#   Default services: every deployable service (scripts/ci/list-services.sh).
#
# Per service (all files optional, see docs/ci-cd.md):
#   <service>/deploy/service.conf    bash: HEALTH_PATH, EXTRA_FLAGS=( ... )
#   <service>/deploy/env.yaml        Cloud Run env vars; ${VARS} rendered with envsubst
#   <service>/deploy/pre-deploy.sh   hook run before deploying (e.g. upload seed files)
#
# Safe rollout: an existing service gets the new revision with no traffic, the revision is
# health-checked on its own tagged URL, and only then receives 100% of traffic. If the
# check fails the script exits non-zero and the previous revision keeps serving.
set -euo pipefail

(( BASH_VERSINFO[0] >= 4 )) || { echo "deploy.sh needs bash >= 4 (macOS: brew install bash)" >&2; exit 2; }

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "$ROOT"

ENVIRONMENT=${1:?environment (staging|production) required}
IMAGE_TAG=${2:?image tag required}
shift 2

[[ -f "infra/environments/$ENVIRONMENT.env" ]] || { echo "unknown environment: $ENVIRONMENT" >&2; exit 2; }

set -a
# shellcheck source=SCRIPTDIR/../../infra/gcp/project.env
source infra/gcp/project.env
# shellcheck source=/dev/null
source "infra/environments/$ENVIRONMENT.env"
set +a

REGISTRY="$REGION-docker.pkg.dev/$PROJECT_ID/$AR_REPO"
REVISION_TAG="sha-${IMAGE_TAG:0:7}"

if [[ $# -gt 0 ]]; then
  services=("$@")
else
  mapfile -t services < <(scripts/ci/list-services.sh)
fi

# Cloud Run URLs are deterministic, so every service can be told where the others live
# before they are deployed: SUPPLIER_SERVICE_URL, FRONTEND_URL, ...
service_url() { echo "https://$1-$ENVIRONMENT-$PROJECT_NUMBER.$REGION.run.app"; }
for dir in */; do
  dir=${dir%/}
  [[ -f "$dir/Dockerfile" ]] || continue
  var="$(tr '[:lower:]-' '[:upper:]_' <<<"$dir")_URL"
  export "$var=$(service_url "$dir")"
done

is_backend() { [[ $1 != frontend && $1 != gateway ]]; }

# Deploy order: backends, then the frontend, then the gateway in front of them.
ordered=()
for svc in "${services[@]}"; do is_backend "$svc" && ordered+=("$svc"); done
for svc in frontend gateway; do
  for s in "${services[@]}"; do [[ $s == "$svc" ]] && ordered+=("$svc"); done
done

render_env_file() {
  local svc=$1 out=$2
  : >"$out"
  [[ -f "$svc/deploy/env.yaml" ]] && envsubst <"$svc/deploy/env.yaml" >"$out"

  # Standard runtime contract for backend services (AGENTS.md); a service's env.yaml wins.
  if is_backend "$svc"; then
    local name=${svc%-service}
    local -A defaults=(
      [ENVIRONMENT]="$ENVIRONMENT"
      [LOG_LEVEL]="${LOG_LEVEL:-INFO}"
      [LOGGING_STRUCTURED_FORMAT_CONSOLE]="ecs"
      [FIRESTORE_PROJECT_ID]="$FIRESTORE_PROJECT_ID"
      [FIRESTORE_DATABASE_ID]="$name-$ENVIRONMENT"
      [FIREBASE_AUTH_PROJECT_ID]="$FIREBASE_AUTH_PROJECT_ID"
      [USER_SERVICE_MODE]="$USER_SERVICE_MODE"
      [USER_SERVICE_URL]="${USER_SERVICE_URL:-}"
      [MOCK_ADMIN_EMAILS]="$MOCK_ADMIN_EMAILS"
      [CORS_ORIGINS]="$(service_url gateway)"
    )
    local key
    for key in "${!defaults[@]}"; do
      grep -q "^$key:" "$out" || printf '%s: "%s"\n' "$key" "${defaults[$key]}" >>"$out"
    done
  fi
  [[ -s $out ]] || printf 'ENVIRONMENT: "%s"\n' "$ENVIRONMENT" >"$out"
}

deploy_service() {
  local svc=$1
  local name="$svc-$ENVIRONMENT"
  local image="$REGISTRY/$svc:$IMAGE_TAG"
  local HEALTH_PATH=/actuator/health
  local EXTRA_FLAGS=()
  # shellcheck source=/dev/null
  [[ -f "$svc/deploy/service.conf" ]] && source "$svc/deploy/service.conf"

  echo "::group::Deploy $name ($image)"

  if [[ -x "$svc/deploy/pre-deploy.sh" ]]; then
    ENVIRONMENT=$ENVIRONMENT "$svc/deploy/pre-deploy.sh"
  fi

  local env_file
  env_file=$(mktemp)
  render_env_file "$svc" "$env_file"

  local identity=()
  local sa="foc-$svc@$PROJECT_ID.iam.gserviceaccount.com"
  gcloud iam service-accounts describe "$sa" --project "$PROJECT_ID" >/dev/null 2>&1 && identity=(--service-account "$sa")

  local existing=false rollout=()
  if gcloud run services describe "$name" --project "$PROJECT_ID" --region "$REGION" >/dev/null 2>&1; then
    existing=true
    rollout=(--no-traffic --tag "$REVISION_TAG")
  fi

  gcloud run deploy "$name" \
    --project "$PROJECT_ID" --region "$REGION" --platform managed \
    --image "$image" --port 8080 --allow-unauthenticated \
    --env-vars-file "$env_file" \
    --labels "app=foc,service=$svc,environment=$ENVIRONMENT,commit=${IMAGE_TAG:0:40}" \
    "${identity[@]}" "${rollout[@]}" "${EXTRA_FLAGS[@]}" --quiet
  rm -f "$env_file"

  local url
  if $existing; then
    url=$(gcloud run services describe "$name" --project "$PROJECT_ID" --region "$REGION" --format json |
      jq -r --arg tag "$REVISION_TAG" '.status.traffic[] | select(.tag == $tag) | .url')
  else
    url=$(gcloud run services describe "$name" --project "$PROJECT_ID" --region "$REGION" --format 'value(status.url)')
  fi

  echo "Smoke test: $url$HEALTH_PATH"
  local attempt
  for attempt in $(seq 1 12); do
    if curl -fsS --max-time 10 "$url$HEALTH_PATH" >/dev/null; then
      if $existing; then
        gcloud run services update-traffic "$name" --project "$PROJECT_ID" --region "$REGION" --to-latest --quiet
        gcloud run services update-traffic "$name" --project "$PROJECT_ID" --region "$REGION" --remove-tags "$REVISION_TAG" --quiet
      fi
      echo "$name is healthy and serving $IMAGE_TAG"
      echo "::endgroup::"
      return 0
    fi
    echo "  not healthy yet (attempt $attempt/12)"
    sleep 5
  done

  echo "::endgroup::"
  echo "::error title=Deploy failed::$name did not become healthy; traffic stays on the previous revision."
  return 1
}

for svc in "${ordered[@]}"; do
  deploy_service "$svc"
done

# Record what each environment runs, so production can promote exactly what staging tested.
for svc in "${ordered[@]}"; do
  gcloud artifacts docker tags add "$REGISTRY/$svc:$IMAGE_TAG" "$REGISTRY/$svc:$ENVIRONMENT" --quiet
done

gateway_url=$(service_url gateway)
echo "Deployed ${#ordered[@]} service(s) to $ENVIRONMENT at $IMAGE_TAG: $gateway_url"
[[ -n ${GITHUB_OUTPUT:-} ]] && echo "url=$gateway_url" >>"$GITHUB_OUTPUT"
exit 0
