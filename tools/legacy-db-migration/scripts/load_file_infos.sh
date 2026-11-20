#!/usr/bin/env bash
# Create staging table legacy_media_sync from matched manifest rows, then run sql/50_file_infos.sql.
# Requires TARGET_DATABASE_URL. Type YES to confirm writes.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
OUT_DIR="${OUT_DIR:-$ROOT/out}"
MANIFEST_CSV="${MANIFEST_CSV:-$OUT_DIR/files_sync_manifest.csv}"
STAGING_CSV="$OUT_DIR/legacy_media_sync_matched.csv"
SQL="$ROOT/sql/50_file_infos.sql"

if [[ -z "${TARGET_DATABASE_URL:-}" ]]; then
  echo "TARGET_DATABASE_URL is required." >&2
  exit 1
fi

if [[ ! -f "$MANIFEST_CSV" ]]; then
  echo "Missing $MANIFEST_CSV — run build_files_manifest.sh first." >&2
  exit 1
fi

echo "About to load matched file_infos into TARGET. Type YES to continue:"
read -r CONFIRM
if [[ "$CONFIRM" != "YES" ]]; then
  echo "Aborted."
  exit 1
fi

export MANIFEST_CSV STAGING_CSV
python3 - <<'PY'
import csv, os
from pathlib import Path

src = Path(os.environ["MANIFEST_CSV"])
dst = Path(os.environ["STAGING_CSV"])
fields = [
    "match_status", "id", "file_name", "s3_key", "attachment_type", "category",
    "content_type", "file_size_bytes", "payment_request_id",
    "warehouse_inbound_receipt_id", "created_at", "updated_at", "is_deleted",
]
with src.open(newline="", encoding="utf-8") as fin, dst.open("w", newline="", encoding="utf-8") as fout:
    reader = csv.DictReader(fin)
    writer = csv.DictWriter(fout, fieldnames=fields)
    writer.writeheader()
    n = 0
    for r in reader:
        if r.get("match_status") != "matched":
            continue
        row = {k: (r.get(k) or "") for k in fields}
        # empty UUID → leave blank for COPY NULL
        for uk in ("payment_request_id", "warehouse_inbound_receipt_id"):
            if not row[uk].strip():
                row[uk] = ""
        writer.writerow(row)
        n += 1
print(f"Staging CSV rows: {n} → {dst}")
PY

psql "$TARGET_DATABASE_URL" -v ON_ERROR_STOP=1 <<SQL
DROP TABLE IF EXISTS legacy_media_sync;
CREATE TABLE legacy_media_sync (
  match_status TEXT,
  id UUID PRIMARY KEY,
  file_name TEXT NOT NULL,
  s3_key TEXT NOT NULL,
  attachment_type TEXT NOT NULL,
  category TEXT,
  content_type TEXT,
  file_size_bytes BIGINT,
  payment_request_id UUID,
  warehouse_inbound_receipt_id UUID,
  created_at TIMESTAMPTZ,
  updated_at TIMESTAMPTZ,
  is_deleted BOOLEAN DEFAULT false
);
SQL

psql "$TARGET_DATABASE_URL" -v ON_ERROR_STOP=1 -c "\\copy legacy_media_sync (
  match_status, id, file_name, s3_key, attachment_type, category,
  content_type, file_size_bytes, payment_request_id, warehouse_inbound_receipt_id,
  created_at, updated_at, is_deleted
) FROM '$STAGING_CSV' WITH (FORMAT csv, HEADER true, NULL '')"

psql "$TARGET_DATABASE_URL" -v ON_ERROR_STOP=1 -f "$SQL"
echo "Stage 50 file_infos load finished."
