#!/usr/bin/env bash
# Read-only dry run against restored cnh_legacy (+ optional target counts).
# Canonical dump: <workspace>/postgres_19_11_2026.dump
# Restore first: ./scripts/restore_legacy_dump.sh
# Never writes.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
SQL="$ROOT/sql"
WORKSPACE="$(cd "$ROOT/../../.." && pwd)"
LEGACY_DUMP_PATH="${LEGACY_DUMP_PATH:-$WORKSPACE/postgres_19_11_2026.dump}"

if [[ -z "${LEGACY_DATABASE_URL:-}" ]]; then
  echo "LEGACY_DATABASE_URL is required (restored cnh_legacy)." >&2
  echo "1) export LEGACY_ADMIN_URL=postgresql://.../postgres" >&2
  echo "2) ./scripts/restore_legacy_dump.sh" >&2
  echo "3) export LEGACY_DATABASE_URL=.../cnh_legacy (printed by restore)" >&2
  exit 1
fi

echo "== Legacy source =="
echo "  dump (canonical): $LEGACY_DUMP_PATH"
if [[ -f "$LEGACY_DUMP_PATH" ]]; then
  ls -lh "$LEGACY_DUMP_PATH"
else
  echo "  warning: dump file not found at path above (DB may still be restored)."
fi
echo "  LEGACY_DATABASE_URL: (set)"

echo "== Legacy extract counts (cnh_legacy) =="
psql "$LEGACY_DATABASE_URL" -v ON_ERROR_STOP=1 -f "$SQL/01_extract_counts.sql"

if [[ -n "${TARGET_DATABASE_URL:-}" ]]; then
  echo "== Target current counts (read-only) =="
  psql "$TARGET_DATABASE_URL" -v ON_ERROR_STOP=1 -f "$SQL/90_validate_counts.sql"
else
  echo "TARGET_DATABASE_URL unset — skipped target counts."
fi

echo "Dry run complete. No data was written."
echo "For stage loads: ./scripts/link_legacy_fdw.sh then ./scripts/run_stage.sh 10"
