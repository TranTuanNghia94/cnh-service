# ID strategy

## Default: preserve UUID

For 1:1 successor tables, **copy the legacy primary key** into the new `id` column so FKs remain stable across stages.

Applies to (non-exhaustive): `users`, `roles`, `vendors`, `customers`, `customer_addresses`, `categories`, `products`, `orders`, `order_lines`, `purchase_orders`, `purchase_order_lines`.

## Generate new UUID

| Case | Rule |
|------|------|
| `vendor_banks` split from vendor columns | New UUID per bank row; `vendor_id` = preserved vendor UUID |
| Join tables with no stable legacy PK | New UUID; uniqueness via natural key |
| MANUAL/TRANSFORM children without clear 1:1 | Prefer preserve parent UUID; new child UUIDs only when necessary; document in stage SQL |

## Soft delete

```sql
(CASE WHEN src.deleted_at IS NOT NULL THEN true ELSE false END) AS is_deleted
```

## Active flags

- User `disabled_at IS NOT NULL` → `is_active = false` (confirm column name on `users` before load).
- Vendor/customer without disable → default `is_active = true` unless a legacy flag exists.

## Conflict policy

- `ON CONFLICT (id) DO UPDATE` only in non-prod iterative loads.
- Natural unique keys (`code`, order prefix+number): fail loud on conflict during cutover dry-runs; do not silently overwrite production.
