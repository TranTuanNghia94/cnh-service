#!/usr/bin/env bash
# Restore workspace postgres custom dump into local DB cnh_legacy.
# Does NOT touch cnh_wms / TARGET.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
WORKSPACE="$(cd "$ROOT/../../.." && pwd)"

LEGACY_DUMP_PATH="${LEGACY_DUMP_PATH:-$WORKSPACE/postgres_19_11_2026.dump}"
LEGACY_DB_NAME="${LEGACY_DB_NAME:-cnh_legacy}"

if ! command -v pg_restore >/dev/null 2>&1; then
  echo "pg_restore not found. Install PostgreSQL client tools." >&2
  exit 1
fi

if ! command -v psql >/dev/null 2>&1; then
  echo "psql not found. Install PostgreSQL client tools." >&2
  exit 1
fi

if [[ ! -f "$LEGACY_DUMP_PATH" ]]; then
  echo "Dump not found: $LEGACY_DUMP_PATH" >&2
  echo "Set LEGACY_DUMP_PATH to postgres_19_11_2026.dump" >&2
  exit 1
fi

if [[ -z "${LEGACY_ADMIN_URL:-}" ]]; then
  echo "LEGACY_ADMIN_URL is required (postgres URL to maintenance DB, usually .../postgres)." >&2
  echo "Example: export LEGACY_ADMIN_URL=postgresql://postgres:PASS@localhost:5432/postgres" >&2
  exit 1
fi

echo "== Dump =="
echo "  path: $LEGACY_DUMP_PATH"
file "$LEGACY_DUMP_PATH" 2>/dev/null || true

echo "== Ensuring database $LEGACY_DB_NAME =="
EXISTS="$(psql "$LEGACY_ADMIN_URL" -Atqc "SELECT 1 FROM pg_database WHERE datname = '$LEGACY_DB_NAME'" || true)"
if [[ "$EXISTS" != "1" ]]; then
  psql "$LEGACY_ADMIN_URL" -v ON_ERROR_STOP=1 -c "CREATE DATABASE $LEGACY_DB_NAME;"
else
  echo "  database already exists"
fi

# Build URL for cnh_legacy from admin URL (replace final path segment)
export LEGACY_ADMIN_URL LEGACY_DB_NAME
LEGACY_DATABASE_URL="$(python3 - <<'PY'
from urllib.parse import urlparse, urlunparse
import os
u = urlparse(os.environ["LEGACY_ADMIN_URL"])
path = "/" + os.environ.get("LEGACY_DB_NAME", "cnh_legacy")
print(urlunparse((u.scheme, u.netloc, path, "", "", "")))
PY
)"

echo "== pg_restore into $LEGACY_DB_NAME =="
# --clean --if-exists: idempotent re-restore; ignore non-fatal ACL/extension noise via exit check
set +e
pg_restore \
  --clean \
  --if-exists \
  --no-owner \
  --no-acl \
  -d "$LEGACY_DATABASE_URL" \
  "$LEGACY_DUMP_PATH"
RC=$?
set -e
# pg_restore returns 1 for warnings (e.g. extension already exists); 0 ok; >1 hard fail
if [[ $RC -gt 1 ]]; then
  echo "pg_restore failed with exit code $RC" >&2
  exit "$RC"
fi

echo
echo "Restore complete."
echo "export LEGACY_DATABASE_URL=\"$LEGACY_DATABASE_URL\""
echo "Next: ./scripts/dry_run.sh"
echo "Then (for stage loads on target): ./scripts/link_legacy_fdw.sh"
