# Files map: disk `files/` → SYS_MEDIA → S3 → `file_infos`

## Local inventory

Workspace path: `files/` (~1.6GB)

| Folder | Approx role (legacy `E_UPLOAD_TYPE`) |
|--------|--------------------------------------|
| `PAPERWORK` | Payment / general paperwork |
| `BANK_NOTE` | Bank transfer proof |
| `QUOTE` | Quote / báo giá attachments |

## Join key

**Disk basename == `core.SYS_MEDIA.name`** (unique in legacy).

Old naming: sanitized original filename + `_` + epoch millis + extension.

## Folder → new columns / S3

| Disk folder | `attachment_type` | `category` | S3 key |
|-------------|-------------------|------------|--------|
| `BANK_NOTE` | `BANK_NOTE` | `bank-note` | `payment-requests/bank-note/{name}` |
| `PAPERWORK` | `PAPER` | `papers` | `payment-requests/papers/{name}` |
| `QUOTE` | `PAPER` | `quote` | `payment-requests/quote/{name}` |

IAM on the app bucket allows `payment-requests/*` (not `legacy/*`).

## Row mapping (`SYS_MEDIA` → `file_infos`)

| Target column | Source |
|---------------|--------|
| `id` | `SYS_MEDIA.id` (preserve UUID) |
| `file_name` | `SYS_MEDIA.name` |
| `file_path` | S3 key above (not old `uri`) |
| `content_type` | `mime` |
| `file_size_bytes` | disk size (prefer) else `size` |
| `category` | from folder table |
| `attachment_type` | from folder table |
| `payment_request_id` | `"banknote_paymentRequest_id"` if BANK_NOTE else `req_payment_id` |
| `warehouse_inbound_receipt_id` | `import_request_document_id` only if UUID exists in target receipts; else NULL |
| `is_deleted` | `deleted_at IS NOT NULL` |
| `created_at` / `updated_at` | copy timestamps |

## Pipeline scripts

1. `scripts/inventory_files.sh` → `out/files_disk_index.csv`
2. `sql/02_extract_media.sql` on `LEGACY_DATABASE_URL` → `out/sys_media.csv`
3. `scripts/build_files_manifest.sh` → `out/files_sync_manifest.csv` (+ match summary)
4. `scripts/sync_files_to_s3.sh` — dry-run default; `CONFIRM=YES` uploads matched rows
5. `scripts/load_file_infos.sh` — staging + `sql/50_file_infos.sql` on target

## Unmatched rows

- **Disk-only** / **DB-only**: listed in summary; **not** uploaded or inserted in this phase.
- Do not invent `file_infos` for disk orphans without `SYS_MEDIA`.

## Env

```bash
export FILES_ROOT="/Users/nghiatran/Desktop/CNH/workspace/files"   # default: workspace/files
export LEGACY_DATABASE_URL="postgresql://.../cnh_legacy"
export TARGET_DATABASE_URL="postgresql://.../cnh_wms"
# S3: sync_files_to_s3.sh sources cnh-be-service-java/.env via load_be_env.sh
# maps AWS_ACCESS_KEY → AWS_ACCESS_KEY_ID, AWS_SECRET_KEY → AWS_SECRET_ACCESS_KEY
```
