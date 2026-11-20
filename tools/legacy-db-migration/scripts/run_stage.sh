#!/usr/bin/env bash
# Run one ETL stage against TARGET. Stages 10–40 need legacy_core/legacy_ies FDW.
# Prerequisite: restore_legacy_dump.sh → link_legacy_fdw.sh
# Usage: ./run_stage.sh <10|20|30|40|50|90|91>
# Stage 50 prefers ./scripts/load_file_infos.sh (CSV staging); this path only
# re-runs sql/50_file_infos.sql if legacy_media_sync already exists.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
SQL="$ROOT/sql"
STAGE="${1:-}"

usage() {
  echo "Usage: $0 <10|20|30|40|50|90|91>" >&2
  echo "Stages 10–40 require foreign schemas legacy_core/legacy_ies on TARGET." >&2
  echo "Stage 50: prefer ./scripts/load_file_infos.sh after S3 sync." >&2
  exit 1
}

[[ -n "$STAGE" ]] || usage

case "$STAGE" in
  10) FILE="10_master_data.sql" ;;
  20) FILE="20_orders_po.sql" ;;
  30) FILE="30_payments.sql" ;;
  40) FILE="40_warehouse.sql" ;;
  50) FILE="50_file_infos.sql" ;;
  90) FILE="90_validate_counts.sql" ;;
  91) FILE="91_validate_orphans.sql" ;;
  *) usage ;;
esac

if [[ -z "${TARGET_DATABASE_URL:-}" ]]; then
  echo "TARGET_DATABASE_URL is required." >&2
  exit 1
fi

if [[ "$STAGE" == "50" ]]; then
  echo "Stage 50 expects table legacy_media_sync. Prefer: ./scripts/load_file_infos.sh"
fi

if [[ "$STAGE" =~ ^(10|20|30|40|50)$ ]]; then
  if [[ "$STAGE" =~ ^(10|20|30|40)$ ]]; then
    MISSING="$(psql "$TARGET_DATABASE_URL" -Atqc \
      "SELECT COUNT(*) FROM information_schema.schemata WHERE schema_name IN ('legacy_core','legacy_ies')" \
      || echo 0)"
    if [[ "$MISSING" != "2" ]]; then
      echo "Missing legacy_core/legacy_ies on TARGET. Run ./scripts/link_legacy_fdw.sh" >&2
      exit 1
    fi
  fi
  echo "About to run WRITE stage $STAGE ($FILE) on TARGET_DATABASE_URL."
  echo "Type YES to continue:"
  read -r CONFIRM
  if [[ "$CONFIRM" != "YES" ]]; then
    echo "Aborted."
    exit 1
  fi
fi

psql "$TARGET_DATABASE_URL" -v ON_ERROR_STOP=1 -f "$SQL/$FILE"
echo "Stage $STAGE finished."
