# Schema map: old → new

Sources: `/Users/nghiatran/Desktop/CNH/be/cnh-be-service/schema/schema.prisma` → `cnh-be-service-java` Flyway + JPA entities.

**Modes:** `1:1` | `SPLIT` | `MANUAL/TRANSFORM` | `SKIP`

## Global transforms

| Old | New |
|-----|-----|
| `deleted_at IS NOT NULL` | `is_deleted = true` |
| `deleted_at IS NULL` | `is_deleted = false` |
| `created_at` / `updated_at` | copy timestamps |
| `created_by` / `updated_by` UUID FK | cast/store as string if target is `varchar`/`text` |
| `_metadata` / `_version` | drop unless target has `version` (e.g. orders) |
| Multi-schema `core`/`ies` | single `public` |

## Auth / ACL

| Old | New | Mode | Notes |
|-----|-----|------|-------|
| `core.SYS_USER` | `users` | 1:1 | Map login fields; `disabled_at` → `is_active` |
| `core.SYS_USER_PROFILE` | (columns on `users` or SKIP) | MANUAL | Fold needed profile fields into `users` |
| `core.SYS_ROLE` | `roles` | 1:1 | |
| `core.SYS_ACL_ITEM` | `permissions` | MANUAL | Prefer aligning to V32/V33 permission catalog codes |
| `core.SYS_ASSIGNMENT` | `user_roles` / `user_assignments` / `role_permissions` | MANUAL | Rebuild joins from assignment semantics |
| `core.SYS_MEDIA` | `file_infos` + S3 | MANUAL | Join disk `files/{PAPERWORK,BANK_NOTE,QUOTE}` by `name`; see `FILES_MAP.md` |
| `core.SYS_NOTIFICATION` | `notifications` | MANUAL | Shape differs |
| `core.SYS_CONFIG`, `SYS_MAPPER*`, `SYS_CHOICE`, `SYS_BUSINESS_RULE`, `SYS_PERIODIC_TASK`, `SYS_JOB`, `SYS_CUSTOM_NOTIFICATION`, `SYS_USER_PUSH_NOTI` | — | SKIP | Old platform features |
| `audit.AUDIT_CHANGE_LOG` | — | SKIP | |

## Master data

| Old | New | Mode | Notes |
|-----|-----|------|-------|
| `ies.VEN_VENDOR` | `vendors` | 1:1 | Preserve `id`, `code`, `name`, email, country, currency, address, misa_code |
| bank_* on `VEN_VENDOR` | `vendor_banks` | SPLIT | One bank row per vendor when any bank field present; new UUID for bank row OK |
| `ies.CUS_CUSTOMER` | `customers` | 1:1 | |
| `ies.CUS_CUSTOMER_CONTACT` | `customer_addresses` | 1:1 | Remap contact columns → address fields |
| `ies.PRO_PRODUCT_CAT` | `categories` | MANUAL | Legacy: `name` + `uom` only → derive `code`, map `uom`→`unit` |
| `ies.PRO_PRODUCT` | `products` | 1:1 | |
| `ies.PRODUCT_SUPPLIER` | — | SKIP | No direct successor |
| — | `product_tax_histories` | SKIP seed | New-only; leave empty or backfill later |

## Orders / PO

| Old | New | Mode | Notes |
|-----|-----|------|-------|
| `ies.ORD_ORDER` | `orders` | MANUAL | Split `sale_order_code` → `order_prefix` + `order_number`; set `order_code` if present (V30) |
| `ies.ORD_ORDER_LINE` | `order_lines` | 1:1 | Preserve line UUID; FK to order/product/vendor |
| `ies.PO_PURCHASE_ORDER` | `purchase_orders` | 1:1 | Field audit required before load |
| `ies.PO_PURCHASE_ORDER_LINE` | `purchase_order_lines` | 1:1 | |
| — | `batch_order_import_jobs*` | SKIP | New-only |

## Payments

| Old | New | Mode | Notes |
|-----|-----|------|-------|
| `ies.PAY_PAYMENT` | `payment_requests` | MANUAL/TRANSFORM | Header remap only as starting point |
| `ies.PAY_PAYMENT_LINE` | `payment_request_purchase_order_lines` / fees / docs | MANUAL/TRANSFORM | Requires product rules |
| `ies.PAY_APPROVAL_HISTORY` | `payment_request_approvals` | MANUAL/TRANSFORM | |
| `ies.FEE_MANAGEMENT` | `payment_request_extra_fees` or inbound fees | MANUAL | Context-dependent |
| `payment_request_details` (legacy V3) | — | SKIP | Superseded in new DB |

## Warehouse

| Old | New | Mode | Notes |
|-----|-----|------|-------|
| `ies.WAREHOUSE` | — | SKIP | No warehouse master in new schema |
| `ies.IMPORT_REQUEST` | `warehouse_inbound_receipts` | MANUAL/TRANSFORM | New receipts link to **payment request** |
| `ies.IMPORT_REQUEST_DETAIL` | `warehouse_inbound_receipt_lines` | MANUAL/TRANSFORM | |
| `ies.EXPORT_REQUEST` | `warehouse_outbounds` | MANUAL/TRANSFORM | |
| `ies.EXPORT_REQUEST_DETAIL` | `warehouse_outbound_details` | MANUAL/TRANSFORM | |
| `ies.INVENTORY` | `warehouse_inventory` | TRANSFORM | Aggregate by `product_id` via `run_remaining_etl.py` |
| `ies.INVENTORY_TRANSACTION` | `warehouse_stock_transactions` | TRANSFORM | `run_stock_transaction_etl.py`: NHAP→INBOUND, XUAT→OUTBOUND, TON→IMPORT |
| `ies.REPORT_DETAIL_IMPORT_EXPORT` | — | SKIP | Reports regenerated |

## New-only (no legacy source)

`user_passkey_credentials`, `export_jobs`, `batch_order_import_jobs*`, `product_tax_histories` (unless derived), inbound/outbound approval/fee tables until TRANSFORM defined.
