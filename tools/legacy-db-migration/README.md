# Legacy DB migration (dump → Flyway / Java schema)

Batch ETL from the **canonical legacy dump** into `cnh_wms` (Java / Flyway), plus **local `files/` → S3 → `file_infos`**.

**Source of truth (old data):** workspace file  
`postgres_19_11_2026.dump`  
(Postgres custom archive; schemas `audit` / `core` / `ies`)

**Source of truth (old media files):** workspace  
`files/{PAPERWORK,BANK_NOTE,QUOTE}/`  
(see [`mapping/FILES_MAP.md`](mapping/FILES_MAP.md))

**Not CDC.** Dry-run by default. Do not point scripts at production without an explicit cutover plan.

## Prerequisites

- `psql` + `pg_restore` (PostgreSQL 15+ client)
- Local Postgres where you can create DB `cnh_legacy`
- Non-prod target DB with Flyway V1–V36 applied (`cnh_wms`)
- For file sync: `aws` CLI, `python3`, access to `${AWS_S3_BUCKET}`
- Env vars (never commit secrets):

```bash
# Admin URL used only to CREATE DATABASE cnh_legacy
export LEGACY_ADMIN_URL="postgresql://postgres:PASS@localhost:5432/postgres"

# After restore (script prints this):
export LEGACY_DATABASE_URL="postgresql://postgres:PASS@localhost:5432/cnh_legacy"

# Target (Java app DB)
export TARGET_DATABASE_URL="postgresql://postgres:PASS@localhost:5432/cnh_wms"

# Optional overrides:
# export LEGACY_DUMP_PATH="/Users/nghiatran/Desktop/CNH/workspace/postgres_19_11_2026.dump"
# export FILES_ROOT="/Users/nghiatran/Desktop/CNH/workspace/files"

# S3: sync_files_to_s3.sh auto-loads cnh-be-service-java/.env
# (AWS_S3_BUCKET, AWS_REGION, AWS_ACCESS_KEY, AWS_SECRET_KEY)
```


## Layout

```
mapping/     SCHEMA_MAP, ID_STRATEGY, DOMAIN_ORDER, FILES_MAP
sql/         staged SQL (extract → transform/load stubs → validate)
scripts/     restore → dry_run → link FDW → run_stage → file inventory/sync
out/         generated CSVs (gitignored locally; create via scripts)
```

## Safe run order (DB)

1. Read `mapping/DOMAIN_ORDER.md` and `SCHEMA_MAP.md`.
2. `./scripts/restore_legacy_dump.sh` — load dump into `cnh_legacy`.
3. `./scripts/dry_run.sh` — row counts on legacy (+ optional target); **no writes**.
4. `./scripts/link_legacy_fdw.sh` — on **target**, import `legacy_core` / `legacy_ies` via postgres_fdw.
5. Load stage 10: `./scripts/run_stage.sh 10` (type `YES`) — categories, vendors, customers, addresses, products.
6. Commercial + warehouse (Python; stages 20–40 SQL are historical stubs):
   - `./.venv/bin/python scripts/run_remaining_etl.py`
   - `./.venv/bin/python scripts/run_payment_inbound_etl.py`
   - `./.venv/bin/python scripts/run_stock_transaction_etl.py`
   - `./.venv/bin/python scripts/run_outbound_detail_etl.py`
   - `./.venv/bin/python scripts/backfill_audit_usernames.py`
7. Or run the idempotent completeness orchestrator:
   `./scripts/run_completeness_pass.sh`
8. Validate: `./.venv/bin/python scripts/validate_etl_completeness.py`  
   (also `90` / `91` / `92` SQL on target).

```text
postgres_19_11_2026.dump
        │
        ▼
restore_legacy_dump.sh  →  cnh_legacy (core/ies/audit)
        │
        ▼
dry_run.sh  (LEGACY_DATABASE_URL)
        │
        ▼
link_legacy_fdw.sh  (TARGET ← FDW → cnh_legacy)
        │
        ▼
run_stage.sh 10  →  master (products/addresses/…)
        │
        ▼
Python ETL / run_completeness_pass.sh  →  commercial + warehouse
        │
        ▼
validate_etl_completeness.py
```

## Files → DB → S3

1. `./scripts/inventory_files.sh`
2. Export media: `psql "$LEGACY_DATABASE_URL" -f sql/02_extract_media.sql -o …` (or via `build_files_manifest.sh`)
3. `./scripts/build_files_manifest.sh` — review matched / disk-only / db-only counts
4. `./scripts/sync_files_to_s3.sh` — dry-run; then `CONFIRM=YES ./scripts/sync_files_to_s3.sh`
5. `./scripts/load_file_infos.sh` (or `run_stage.sh 50`) — stage CSV → `file_infos`

```text
files/{PAPERWORK,BANK_NOTE,QUOTE}
        │
inventory_files.sh ──┐
                     ├──► build_files_manifest.sh ──► out/files_sync_manifest.csv
02_extract_media.sql ┘              │
                                    ├──► sync_files_to_s3.sh ──► S3 legacy/...
                                    └──► 50_file_infos.sql ──► file_infos
```

## Stages

| Code | File | Domain |
|------|------|--------|
| 01 | `sql/01_extract_counts.sql` | Baseline counts (on `cnh_legacy`) |
| 02 | `sql/02_extract_media.sql` | `SYS_MEDIA` extract for file join |
| 10 | `sql/10_master_data.sql` | Categories, vendors(+banks), customers, addresses, products |
| 20 | `sql/20_orders_po.sql` | Historical stub — use `run_remaining_etl.py` |
| 30 | `sql/30_payments.sql` | Historical stub — use Python payment ETLs |
| 40 | `sql/40_warehouse.sql` | Historical stub — use Python warehouse ETLs |
| 50 | `sql/50_file_infos.sql` | `file_infos` from staging / manifest |
| 90/91/92 | validate | Counts + orphans + legacy delta notes |

Stage `10` expects foreign schemas **`legacy_core`** / **`legacy_ies`** on the target connection (created by `link_legacy_fdw.sh`). Dual-DB Python scripts talk to `LEGACY_DATABASE_URL` + `TARGET_DATABASE_URL` directly.

## Known gaps / allowlists

- Outbound details without a resolvable `order_line` (sale_order+product) are skipped (~70–150); see `out/etl_skip_report_outbound_details.json`.
- Stock transactions with zero quantity are skipped.
- `file_infos` only loads matched disk ∩ `SYS_MEDIA` rows.
- Old-only tables (mappers, choices, business rules, changelog) are intentionally skipped.
- Flyway must already define target tables; this tool does not replace V* migrations.
- The dump / `files/` tree stay at the workspace root; do not commit secrets or 1.6GB binaries unless team policy allows.
