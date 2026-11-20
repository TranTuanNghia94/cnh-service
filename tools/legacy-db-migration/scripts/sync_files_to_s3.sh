#!/usr/bin/env bash
# Upload matched manifest rows to S3. Dry-run by default.
# Loads AWS settings from cnh-be-service-java/.env.
# Live upload: CONFIRM=YES ./scripts/sync_files_to_s3.sh
# Optional: WORKERS=16 (default) for parallel uploads.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
# shellcheck disable=SC1091
source "$ROOT/scripts/load_be_env.sh"

OUT_DIR="${OUT_DIR:-$ROOT/out}"
MANIFEST_CSV="${MANIFEST_CSV:-$OUT_DIR/files_sync_manifest.csv}"
CONFIRM="${CONFIRM:-}"
WORKERS="${WORKERS:-16}"

if [[ ! -f "$MANIFEST_CSV" ]]; then
  echo "Missing $MANIFEST_CSV — run ./scripts/build_files_manifest.sh first." >&2
  exit 1
fi

if [[ -z "${AWS_S3_BUCKET:-}" ]]; then
  echo "AWS_S3_BUCKET missing in .env" >&2
  exit 1
fi

if [[ -z "${AWS_ACCESS_KEY_ID:-}" || -z "${AWS_SECRET_ACCESS_KEY:-}" ]]; then
  echo "AWS credentials missing in .env (AWS_ACCESS_KEY / AWS_SECRET_KEY)." >&2
  exit 1
fi

REGION="${AWS_REGION:-${AWS_DEFAULT_REGION:-}}"

if ! command -v aws >/dev/null 2>&1; then
  echo "aws CLI not found." >&2
  exit 1
fi

export MANIFEST_CSV AWS_S3_BUCKET CONFIRM REGION WORKERS
export AWS_ACCESS_KEY_ID AWS_SECRET_ACCESS_KEY AWS_DEFAULT_REGION

python3 - <<'PY'
import csv, os, subprocess, sys
from concurrent.futures import ThreadPoolExecutor, as_completed
from pathlib import Path

manifest = Path(os.environ["MANIFEST_CSV"])
bucket = os.environ["AWS_S3_BUCKET"]
confirm = os.environ.get("CONFIRM", "") == "YES"
region = os.environ.get("REGION", "")
workers = int(os.environ.get("WORKERS", "16"))

aws_base = ["aws"]
if region:
    aws_base += ["--region", region]
aws_base += ["s3", "cp"]

rows = []
with manifest.open(newline="", encoding="utf-8") as f:
    for r in csv.DictReader(f):
        if r.get("match_status") == "matched" and r.get("abs_path") and r.get("s3_key"):
            rows.append(r)

print(f"Matched rows to sync: {len(rows)}")
print(f"Bucket: s3://{bucket}/")
print(f"Region: {region or '(default)'}")
print(f"Workers: {workers}")
print(f"Mode: {'UPLOAD' if confirm else 'DRY-RUN'}")

def upload(r):
    local = r["abs_path"]
    key = r["s3_key"]
    dest = f"s3://{bucket}/{key}"
    cmd = aws_base + [local, dest]
    if not confirm:
        return ("dry", key, None)
    try:
        subprocess.run(cmd, check=True, stdout=subprocess.DEVNULL, stderr=subprocess.PIPE, text=True)
        return ("ok", key, None)
    except subprocess.CalledProcessError as e:
        err = (e.stderr or str(e)).strip().splitlines()[-1] if e.stderr else str(e)
        return ("fail", key, err)

ok = fail = 0
if not confirm:
    for r in rows[:20]:
        print("DRY:", " ".join(aws_base + [r["abs_path"], f"s3://{bucket}/{r['s3_key']}"]))
    if len(rows) > 20:
        print(f"... and {len(rows) - 20} more")
    print(f"Done. dry={len(rows)}")
    print("Re-run with CONFIRM=YES to upload.")
    sys.exit(0)

with ThreadPoolExecutor(max_workers=workers) as ex:
    futs = [ex.submit(upload, r) for r in rows]
    for i, fut in enumerate(as_completed(futs), 1):
        status, key, err = fut.result()
        if status == "ok":
            ok += 1
        else:
            fail += 1
            print(f"FAIL {key}: {err}", file=sys.stderr)
        if i % 100 == 0 or i == len(futs):
            print(f"progress {i}/{len(futs)} ok={ok} fail={fail}", flush=True)

print(f"Done. ok={ok} fail={fail}")
sys.exit(1 if fail else 0)
PY
