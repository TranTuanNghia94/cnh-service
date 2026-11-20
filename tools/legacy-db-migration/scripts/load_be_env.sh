#!/usr/bin/env bash
# Source cnh-be-service-java/.env into the current shell (safe-ish: KEY=VALUE lines only).
# Maps app AWS_* names to aws CLI expected names.
# Usage: source "$(dirname "$0")/load_be_env.sh"

_MIGRATE_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
_BE_ROOT="$(cd "$_MIGRATE_ROOT/../.." && pwd)"
_BE_ENV="${CNH_BE_ENV_FILE:-$_BE_ROOT/.env}"

if [[ ! -f "$_BE_ENV" ]]; then
  echo "BE .env not found: $_BE_ENV" >&2
  echo "Set CNH_BE_ENV_FILE or create cnh-be-service-java/.env" >&2
  return 1 2>/dev/null || exit 1
fi

set -a
# shellcheck disable=SC1090
source "$_BE_ENV"
set +a

# App uses AWS_ACCESS_KEY / AWS_SECRET_KEY; aws CLI expects *_ID / AWS_SECRET_ACCESS_KEY
if [[ -n "${AWS_ACCESS_KEY:-}" && -z "${AWS_ACCESS_KEY_ID:-}" ]]; then
  export AWS_ACCESS_KEY_ID="$AWS_ACCESS_KEY"
fi
if [[ -n "${AWS_SECRET_KEY:-}" && -z "${AWS_SECRET_ACCESS_KEY:-}" ]]; then
  export AWS_SECRET_ACCESS_KEY="$AWS_SECRET_KEY"
fi
if [[ -n "${AWS_REGION:-}" && -z "${AWS_DEFAULT_REGION:-}" ]]; then
  export AWS_DEFAULT_REGION="$AWS_REGION"
fi

echo "Loaded env from $_BE_ENV (bucket=${AWS_S3_BUCKET:-unset}, region=${AWS_REGION:-unset})"
unset _MIGRATE_ROOT _BE_ROOT _BE_ENV
