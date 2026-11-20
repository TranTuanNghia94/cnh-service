#!/usr/bin/env bash
# Walk workspace files/{PAPERWORK,BANK_NOTE,QUOTE} → out/files_disk_index.csv
# Optional: HASH=1 to compute sha256 (slow on ~1.6GB).
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
WORKSPACE="$(cd "$ROOT/../../.." && pwd)"
FILES_ROOT="${FILES_ROOT:-$WORKSPACE/files}"
OUT_DIR="${OUT_DIR:-$ROOT/out}"
OUT_CSV="$OUT_DIR/files_disk_index.csv"
HASH="${HASH:-0}"

mkdir -p "$OUT_DIR"

if [[ ! -d "$FILES_ROOT" ]]; then
  echo "FILES_ROOT not found: $FILES_ROOT" >&2
  exit 1
fi

echo "== Inventory $FILES_ROOT =="
export FILES_ROOT OUT_CSV HASH
python3 - <<'PY'
import csv, os, hashlib
from pathlib import Path

files_root = Path(os.environ["FILES_ROOT"])
out_csv = Path(os.environ["OUT_CSV"])
do_hash = os.environ.get("HASH", "0") == "1"
folders = ("PAPERWORK", "BANK_NOTE", "QUOTE")

rows = []
for folder in folders:
    d = files_root / folder
    if not d.is_dir():
        print(f"  skip missing folder: {folder}")
        continue
    for p in d.rglob("*"):
        if not p.is_file():
            continue
        if p.name.startswith("."):
            continue
        st = p.stat()
        sha = ""
        if do_hash:
            h = hashlib.sha256()
            with p.open("rb") as f:
                for chunk in iter(lambda: f.read(1024 * 1024), b""):
                    h.update(chunk)
            sha = h.hexdigest()
        rows.append({
            "folder": folder,
            "basename": p.name,
            "rel_path": str(p.relative_to(files_root)),
            "abs_path": str(p.resolve()),
            "size_bytes": st.st_size,
            "sha256": sha,
        })

rows.sort(key=lambda r: (r["folder"], r["basename"]))
with out_csv.open("w", newline="", encoding="utf-8") as f:
    w = csv.DictWriter(
        f,
        fieldnames=["folder", "basename", "rel_path", "abs_path", "size_bytes", "sha256"],
    )
    w.writeheader()
    w.writerows(rows)

from collections import Counter
c = Counter(r["folder"] for r in rows)
print(f"Wrote {len(rows)} rows → {out_csv}")
for k in folders:
    print(f"  {k}: {c.get(k, 0)}")
PY
