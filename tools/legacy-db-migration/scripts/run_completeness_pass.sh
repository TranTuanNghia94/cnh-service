#!/usr/bin/env bash
# Idempotent completeness pass: master gaps → commercial/warehouse children → validate.
# Requires: LEGACY_DATABASE_URL, TARGET_DATABASE_URL
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
SCRIPTS="$ROOT/scripts"
VENV="${ROOT}/.venv/bin/python"

if [[ -z "${LEGACY_DATABASE_URL:-}" || -z "${TARGET_DATABASE_URL:-}" ]]; then
  echo "LEGACY_DATABASE_URL and TARGET_DATABASE_URL are required." >&2
  exit 1
fi

if [[ ! -x "$VENV" ]]; then
  echo "Missing venv python at $VENV (create .venv with psycopg2-binary)." >&2
  exit 1
fi

run_py() {
  local script="$1"
  echo ""
  echo "======== $(basename "$script") ========"
  "$VENV" "$script"
}

run_py "$SCRIPTS/run_master_backfill_etl.py"
run_py "$SCRIPTS/run_remaining_etl.py"
run_py "$SCRIPTS/backfill_po_links.py"
run_py "$SCRIPTS/run_stock_transaction_etl.py"
run_py "$SCRIPTS/run_outbound_detail_etl.py"
run_py "$SCRIPTS/backfill_audit_usernames.py"
run_py "$SCRIPTS/validate_etl_completeness.py"

echo ""
echo "Completeness pass OK."
