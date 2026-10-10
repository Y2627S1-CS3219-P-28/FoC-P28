#!/usr/bin/env bash
# Provisions the Order Service's Pub/Sub topics for one environment and lets the Order Service
# publish to them. Idempotent. Run by the CI/CD owner (needs an owner-level account), before
# infra/gcp/configure-credit-pubsub.sh, which subscribes the Credit Service to these topics.
#
# Usage: infra/gcp/configure-order-pubsub.sh <staging|production>
#
# Each environment has its own topics (infra/environments/<env>.env), so local runs, staging and
# production never deliver events to each other. Local runs publish to the *-dev-v1 topics, which
# no cloud environment subscribes to.
set -euo pipefail
(( BASH_VERSINFO[0] >= 4 )) || { echo "bash 4+ is required" >&2; exit 2; }

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
environment=${1:?environment (staging|production) required}
[[ $environment == staging || $environment == production ]] || {
  echo "unknown environment: $environment" >&2
  exit 2
}

set -a
# shellcheck source=SCRIPTDIR/project.env
source "$ROOT/infra/gcp/project.env"
# shellcheck source=/dev/null
source "$ROOT/infra/environments/$environment.env"
set +a

gc() { gcloud --project "$PROJECT_ID" --quiet "$@"; }
exists() { "$@" >/dev/null 2>&1; }
publisher="serviceAccount:foc-order-service@${PROJECT_ID}.iam.gserviceaccount.com"

topics=(
  "$ORDER_COMPLETION_TOPIC"
  "$ORDER_OPEN_REFUND_TOPIC"
  "$ORDER_ACCEPTED_CANCELLATION_TOPIC"
)

echo "Configuring Order Service Pub/Sub topics for $environment"
gc services enable pubsub.googleapis.com
for topic in "${topics[@]}"; do
  exists gc pubsub topics describe "$topic" ||
    gc pubsub topics create "$topic" --labels "service=order-service,environment=$environment"
  gc pubsub topics add-iam-policy-binding "$topic" \
    --member "$publisher" --role roles/pubsub.publisher >/dev/null
  echo "  $topic (order-service may publish)"
done
