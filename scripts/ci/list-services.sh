#!/usr/bin/env bash
# Lists deployable services: top-level folders with a non-empty Dockerfile.
# A service joins CI/CD automatically once its Dockerfile has content.
#
# Usage:
#   scripts/ci/list-services.sh                          # one per line
#   scripts/ci/list-services.sh --json                   # JSON array (for workflow matrices)
#   scripts/ci/list-services.sh --changed-since <ref>    # only services with changes since <ref>
#
# Changes to shared CI/infra files (.github/, scripts/ci/, infra/) select every service.
set -euo pipefail
cd "$(git rev-parse --show-toplevel)"

json=false
since=""
while [[ $# -gt 0 ]]; do
  case "$1" in
    --json) json=true; shift ;;
    --changed-since) since="$2"; shift 2 ;;
    *) echo "unknown argument: $1" >&2; exit 2 ;;
  esac
done

services=()
for dir in */; do
  dir=${dir%/}
  [[ -s "$dir/Dockerfile" ]] && services+=("$dir")
done

if [[ -n $since ]]; then
  changed=$(git diff --name-only "$since"...HEAD)
  if ! grep -qE '^(\.github/|scripts/ci/|infra/)' <<<"$changed"; then
    selected=()
    for svc in "${services[@]}"; do
      grep -q "^$svc/" <<<"$changed" && selected+=("$svc")
    done
    services=(${selected[@]+"${selected[@]}"})
  fi
fi

[[ ${#services[@]} -eq 0 ]] && services=("")
if $json; then
  printf '%s\n' "${services[@]}" | jq -R . | jq -cs 'map(select(length > 0))'
else
  printf '%s\n' "${services[@]}" | sed '/^$/d'
fi
