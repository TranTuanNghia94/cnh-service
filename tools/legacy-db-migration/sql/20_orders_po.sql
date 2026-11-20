-- Stage 20: orders + purchase orders.
-- Requires stage 10 (customers, products, vendors) loaded.
-- MANUAL pieces: order_prefix / order_number / order_code derivation from legacy sale_order_code.

BEGIN;

-- ---------------------------------------------------------------------------
-- orders  ← ies.ORD_ORDER (MANUAL transform for numbering)
-- Uncomment and align columns after verifying OrderEntity + V30 order_code.
-- ---------------------------------------------------------------------------
/*
INSERT INTO orders (
  id, version, order_number, order_prefix, customer_id, customer_address_id,
  contract_number, order_date, delivery_date, status,
  total_amount, discount_amount, is_included_tax, tax_rate, tax_amount, final_amount,
  notes, created_at, updated_at, created_by, updated_by, is_deleted
)
SELECT
  o.id,
  COALESCE(o._version, 1),
  -- TODO: parse integer from sale_order_code
  0 AS order_number,
  -- TODO: prefix from sale_order_code
  'LEGACY' AS order_prefix,
  o.customer_id,
  o.customer_contact_id,
  COALESCE(o.contract_number, o.sale_order_code, 'UNKNOWN'),
  o.order_date::date,
  o.delivery_date::date,
  o.status::text,
  COALESCE(o.total_amount, 0),
  COALESCE(o.discount_amount, 0),
  COALESCE(o.is_included_tax, false),
  COALESCE(o.tax_rate, 0),
  COALESCE(o.tax_amount, 0),
  COALESCE(o.final_amount, 0),
  o.notes,
  COALESCE(o.created_at, NOW()),
  o.updated_at,
  NULL,
  NULL,
  (o.deleted_at IS NOT NULL)
FROM legacy_ies."ORD_ORDER" o
ON CONFLICT (id) DO NOTHING;
*/

-- order_lines  ← ies.ORD_ORDER_LINE (1:1 candidate once header exists)
/*
INSERT INTO order_lines (...)
SELECT ... FROM legacy_ies."ORD_ORDER_LINE" ol
ON CONFLICT (id) DO NOTHING;
*/

-- purchase_orders / purchase_order_lines similarly
/*
INSERT INTO purchase_orders (...)
SELECT ... FROM legacy_ies."PO_PURCHASE_ORDER" ...
*/

COMMIT;
