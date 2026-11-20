# Domain load order (FK-safe)

Run stages top → bottom. Do not load a stage until parents exist in target.

```
1. Auth skeleton
   users → roles → permissions (catalog) → user_roles / role_permissions / user_assignments

2. Master data
   categories
   vendors → vendor_banks
   customers → customer_addresses
   products  (FK category)

3. Commercial docs
   orders → order_lines
   purchase_orders → purchase_order_lines

4. Payments (after PO lines + vendors)
   payment_requests → payment_request_* children
   (Python: run_remaining_etl.py headers + run_payment_inbound_etl.py children)

5. Warehouse (after products; inbound after payments in NEW model)
   warehouse_inventory / stock_transactions
   warehouse_outbounds → warehouse_outbound_details
   warehouse_inbound_receipts → warehouse_inbound_receipt_lines
   (Python: run_remaining_etl / run_stock_transaction_etl /
            run_outbound_detail_etl / run_payment_inbound_etl)

6. Files / media (after payments when linking payment_request_id)
   Disk files/ + SYS_MEDIA → S3 → file_infos
   See mapping/FILES_MAP.md
   Can insert file_infos with NULL payment_request_id before payments;
   re-link after payments if needed.

7. Notifications (optional)
   notifications
```

## Script stage codes

| Order | Stage | Script |
|------:|-------|--------|
| 0 | Connectivity example | `00_setup_fdw_or_dblink.sql.example` |
| 1 | Counts | `01_extract_counts.sql` |
| 1b | Media extract | `02_extract_media.sql` |
| 2 | Master (+ auth) | `10_master_data.sql` (categories/vendors/customers/addresses/products) |
| 3 | Orders / PO | `20_orders_po.sql` (**historical stub** — use Python) |
| 4 | Payments | `30_payments.sql` (**historical stub** — use Python) |
| 5 | Warehouse | `40_warehouse.sql` (**historical stub** — use Python) |
| 6 | Files | `50_file_infos.sql` (+ S3 sync scripts) |
| 9 | Validate | `90_validate_counts.sql`, `91_validate_orphans.sql`, `92_validate_legacy_deltas.sql` |

## Python ETL (live load path)

| Script | Loads |
|--------|-------|
| `scripts/run_master_backfill_etl.py` | Missing products, customer_addresses, users; relink order addresses |
| `scripts/run_remaining_etl.py` | Orders/lines, POs/lines, payment headers, inventory, outbounds |
| `scripts/run_payment_inbound_etl.py` | Payment children + inbound receipts/lines |
| `scripts/run_stock_transaction_etl.py` | Stock transactions |
| `scripts/run_outbound_detail_etl.py` | Outbound details (order_line via sale_order+product) |
| `scripts/backfill_audit_usernames.py` | created_by/updated_by UUID → username |
| `scripts/validate_etl_completeness.py` | Legacy vs target gap gate (exit non-zero on FAIL) |
| `scripts/run_completeness_pass.sh` | Orchestrates master → remaining → stock → outbound → audit → validate |
