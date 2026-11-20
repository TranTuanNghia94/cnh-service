#!/usr/bin/env bash
# Join disk index + SYS_MEDIA → out/files_sync_manifest.csv + summary.
# Requires: inventory_files.sh done; LEGACY_DATABASE_URL for media extract.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
WORKSPACE="$(cd "$ROOT/../../.." && pwd)"
OUT_DIR="${OUT_DIR:-$ROOT/out}"
DISK_CSV="${DISK_CSV:-$OUT_DIR/files_disk_index.csv}"
MEDIA_CSV="$OUT_DIR/sys_media.csv"
MANIFEST_CSV="$OUT_DIR/files_sync_manifest.csv"
SUMMARY_TXT="$OUT_DIR/files_sync_summary.txt"

mkdir -p "$OUT_DIR"

if [[ ! -f "$DISK_CSV" ]]; then
  echo "Missing $DISK_CSV — run ./scripts/inventory_files.sh first." >&2
  exit 1
fi

if [[ -z "${LEGACY_DATABASE_URL:-}" ]]; then
  echo "LEGACY_DATABASE_URL is required to extract SYS_MEDIA." >&2
  exit 1
fi

echo "== Extract SYS_MEDIA → $MEDIA_CSV =="
psql "$LEGACY_DATABASE_URL" -v ON_ERROR_STOP=1 -c "\\copy (
  SELECT
    m.id,
    m.name,
    m.ext,
    m.mime,
    m.size,
    m.uri,
    m.req_payment_id,
    m.\"banknote_paymentRequest_id\" AS banknote_payment_request_id,
    m.import_request_document_id,
    m.created_at,
    m.updated_at,
    m.deleted_at,
    (m.deleted_at IS NOT NULL) AS is_deleted
  FROM core.\"SYS_MEDIA\" m
  ORDER BY m.name
) TO STDOUT WITH CSV HEADER" > "$MEDIA_CSV"

echo "== Build manifest =="
export DISK_CSV MEDIA_CSV MANIFEST_CSV SUMMARY_TXT
python3 - <<'PY'
import csv
import os
from collections import Counter
from pathlib import Path

# Prefixes must match IAM-allowed paths used by FileService (payment-requests/...)
FOLDER_MAP = {
    "BANK_NOTE": ("BANK_NOTE", "bank-note", "payment-requests/bank-note"),
    "PAPERWORK": ("PAPER", "papers", "payment-requests/papers"),
    "QUOTE": ("PAPER", "quote", "payment-requests/quote"),
}

disk_csv = Path(os.environ["DISK_CSV"])
media_csv = Path(os.environ["MEDIA_CSV"])
manifest_csv = Path(os.environ["MANIFEST_CSV"])
summary_txt = Path(os.environ["SUMMARY_TXT"])

def read_csv(path):
    with path.open(newline="", encoding="utf-8") as f:
        return list(csv.DictReader(f))

disk_rows = read_csv(disk_csv)
media_rows = read_csv(media_csv)

disk_by_name = {}
for r in disk_rows:
    disk_by_name.setdefault(r["basename"], []).append(r)

media_by_name = {r["name"]: r for r in media_rows}

matched = []
disk_only = []
db_only = []

for name, copies in disk_by_name.items():
    # Prefer first path if duplicates
    d = copies[0]
    m = media_by_name.get(name)
    folder = d["folder"]
    att, cat, prefix = FOLDER_MAP.get(folder, ("PAPER", "papers", "legacy/papers"))
    if m is None:
        disk_only.append({
            "match_status": "disk_only",
            "id": "",
            "file_name": name,
            "folder": folder,
            "abs_path": d.get("abs_path") or "",
            "rel_path": d.get("rel_path") or "",
            "s3_key": "",
            "attachment_type": att,
            "category": cat,
            "content_type": "",
            "file_size_bytes": d.get("size_bytes") or "",
            "payment_request_id": "",
            "warehouse_inbound_receipt_id": "",
            "created_at": "",
            "updated_at": "",
            "is_deleted": "false",
            "legacy_uri": "",
            "disk_dupes": str(len(copies)),
        })
        continue
    if folder == "BANK_NOTE":
        pay_id = m.get("banknote_payment_request_id") or ""
    else:
        pay_id = m.get("req_payment_id") or ""
    s3_key = f"{prefix}/{name}"
    size = d.get("size_bytes") or m.get("size") or ""
    matched.append({
        "match_status": "matched",
        "id": m["id"],
        "file_name": name,
        "folder": folder,
        "abs_path": d["abs_path"],
        "rel_path": d["rel_path"],
        "s3_key": s3_key,
        "attachment_type": att,
        "category": cat,
        "content_type": m.get("mime") or "",
        "file_size_bytes": size,
        "payment_request_id": pay_id,
        "warehouse_inbound_receipt_id": m.get("import_request_document_id") or "",
        "created_at": m.get("created_at") or "",
        "updated_at": m.get("updated_at") or "",
        "is_deleted": m.get("is_deleted") or "false",
        "legacy_uri": m.get("uri") or "",
        "disk_dupes": str(len(copies)),
    })

matched_names = {r["file_name"] for r in matched}
for name, m in media_by_name.items():
    if name not in disk_by_name:
        db_only.append({
            "match_status": "db_only",
            "id": m["id"],
            "file_name": name,
            "folder": "",
            "abs_path": "",
            "rel_path": "",
            "s3_key": "",
            "attachment_type": "",
            "category": "",
            "content_type": m.get("mime") or "",
            "file_size_bytes": m.get("size") or "",
            "payment_request_id": m.get("req_payment_id") or m.get("banknote_payment_request_id") or "",
            "warehouse_inbound_receipt_id": m.get("import_request_document_id") or "",
            "created_at": m.get("created_at") or "",
            "updated_at": m.get("updated_at") or "",
            "is_deleted": m.get("is_deleted") or "false",
            "legacy_uri": m.get("uri") or "",
            "disk_dupes": "0",
        })

fields = [
    "match_status", "id", "file_name", "folder", "abs_path", "rel_path", "s3_key",
    "attachment_type", "category", "content_type", "file_size_bytes",
    "payment_request_id", "warehouse_inbound_receipt_id",
    "created_at", "updated_at", "is_deleted", "legacy_uri", "disk_dupes",
]

all_rows = matched + disk_only + db_only
with manifest_csv.open("w", newline="", encoding="utf-8") as f:
    w = csv.DictWriter(f, fieldnames=fields)
    w.writeheader()
    w.writerows(all_rows)

folder_counts = Counter(r["folder"] for r in matched)
lines = [
    f"disk_files={len(disk_rows)}",
    f"sys_media={len(media_rows)}",
    f"matched={len(matched)}",
    f"disk_only={len(disk_only)}",
    f"db_only={len(db_only)}",
    "matched_by_folder:",
]
for k in ("PAPERWORK", "BANK_NOTE", "QUOTE"):
    lines.append(f"  {k}={folder_counts.get(k, 0)}")
summary_txt.write_text("\n".join(lines) + "\n", encoding="utf-8")
print("\n".join(lines))
print(f"Manifest → {manifest_csv}")
print(f"Summary  → {summary_txt}")
PY
