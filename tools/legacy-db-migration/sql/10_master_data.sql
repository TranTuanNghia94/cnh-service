-- Stage 10: master data (+ auth skeleton).
-- Assumes foreign schemas legacy_core / legacy_ies (see 00_setup_*.example)
-- OR rewrite FROM clauses to dblink/offline staging tables.
-- TARGET columns aligned to JPA entities — re-verify before enabling writes.

BEGIN;

-- ---------------------------------------------------------------------------
-- categories  ← ies.PRO_PRODUCT_CAT
-- Legacy has name + uom only (no code/parent). Derive code from name.
-- ---------------------------------------------------------------------------
INSERT INTO categories (
  id, code, name, unit, description, parent_id, is_active,
  created_at, updated_at, created_by, updated_by, is_deleted
)
SELECT
  c.id,
  LEFT(REGEXP_REPLACE(UPPER(c.name), '[^A-Z0-9]+', '-', 'g'), 50) AS code,
  c.name,
  c.uom,
  NULL,
  NULL,
  true,
  COALESCE(c.created_at, NOW()),
  c.updated_at,
  NULL,
  NULL,
  (c.deleted_at IS NOT NULL)
FROM legacy_ies."PRO_PRODUCT_CAT" c
ON CONFLICT (id) DO NOTHING;

-- ---------------------------------------------------------------------------
-- vendors  ← ies.VEN_VENDOR
-- ---------------------------------------------------------------------------
INSERT INTO vendors (
  id, code, name, email, country, currency, phone, misa_code, address,
  tax_code, contact_person, is_active,
  created_at, updated_at, created_by, updated_by, is_deleted
)
SELECT
  v.id,
  v.code,
  v.name,
  v.email,
  v.country,
  v.currency,
  NULL,
  v.misa_code,
  v.address,
  NULL,
  NULL,
  true,
  COALESCE(v.created_at, NOW()),
  v.updated_at,
  NULL,
  NULL,
  (v.deleted_at IS NOT NULL)
FROM legacy_ies."VEN_VENDOR" v
ON CONFLICT (id) DO NOTHING;

-- ---------------------------------------------------------------------------
-- vendor_banks  ← bank_* columns on VEN_VENDOR (SPLIT)
-- ---------------------------------------------------------------------------
INSERT INTO vendor_banks (
  id, vendor_id, bank_name, bank_account_name, bank_account_number,
  bank_account_branch, bank_account_swift, bank_account_iban, is_active,
  created_at, updated_at, created_by, updated_by, is_deleted
)
SELECT
  gen_random_uuid(),
  v.id,
  v.bank_name,
  v.bank_accountname,
  v.bank_accountnum,
  v.bank_address,
  v.bank_swiftcode,
  v.bank_iban,
  true,
  COALESCE(v.created_at, NOW()),
  v.updated_at,
  NULL,
  NULL,
  false
FROM legacy_ies."VEN_VENDOR" v
WHERE COALESCE(v.bank_name, v.bank_accountnum, v.bank_iban, v.bank_swiftcode) IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM vendor_banks vb
    WHERE vb.vendor_id = v.id AND vb.is_deleted = false
  );

-- ---------------------------------------------------------------------------
-- customers  ← ies.CUS_CUSTOMER
-- ---------------------------------------------------------------------------
INSERT INTO customers (
  id, code, name, email, phone, tax_code, misa_code, is_active,
  created_at, updated_at, created_by, updated_by, is_deleted
)
SELECT
  c.id,
  c.code,
  c.name,
  c.email,
  NULL,
  c.tax_no,
  c.misa_code,
  true,
  COALESCE(c.created_at, NOW()),
  c.updated_at,
  NULL,
  NULL,
  (c.deleted_at IS NOT NULL)
FROM legacy_ies."CUS_CUSTOMER" c
ON CONFLICT (id) DO NOTHING;

-- ---------------------------------------------------------------------------
-- customer_addresses  ← ies.CUS_CUSTOMER_CONTACT
-- ---------------------------------------------------------------------------
INSERT INTO customer_addresses (
  id, customer_id, address, contact_person, phone, email,
  created_at, updated_at, created_by, updated_by, is_deleted
)
SELECT
  a.id,
  a.customer_id,
  NULLIF(
    TRIM(BOTH ', ' FROM CONCAT_WS(', ',
      NULLIF(TRIM(a.add_line_1), ''),
      NULLIF(TRIM(a.add_line_2), ''),
      NULLIF(TRIM(a.ward), ''),
      NULLIF(TRIM(a.district), ''),
      NULLIF(TRIM(a.province), '')
    )),
    ''
  ),
  LEFT(a.contact_name, 100),
  LEFT(a.phone, 20),
  LEFT(a.email, 100),
  COALESCE(a.created_at, NOW()),
  COALESCE(a.updated_at, a.created_at, NOW()),
  'legacy-migrate',
  'legacy-migrate',
  (a.deleted_at IS NOT NULL)
FROM legacy_ies."CUS_CUSTOMER_CONTACT" a
WHERE a.customer_id IS NOT NULL
  AND EXISTS (SELECT 1 FROM customers c WHERE c.id = a.customer_id)
ON CONFLICT (id) DO NOTHING;

-- ---------------------------------------------------------------------------
-- products  ← ies.PRO_PRODUCT
-- ---------------------------------------------------------------------------
INSERT INTO products (
  id, code, name, description, category_id, unit_1, unit_2,
  price, cost_price, tax, misa_code, is_active,
  created_at, updated_at, created_by, updated_by, is_deleted
)
SELECT
  p.id,
  LEFT(p.code, 50),
  LEFT(COALESCE(NULLIF(TRIM(p.name), ''), p.code), 200),
  p.description,
  CASE
    WHEN p.cat_id IS NOT NULL AND EXISTS (SELECT 1 FROM categories c WHERE c.id = p.cat_id)
    THEN p.cat_id
    ELSE NULL
  END,
  LEFT(p.uom_1, 20),
  LEFT(p.uom_2, 20),
  0,
  0,
  COALESCE(p.tax, 0),
  LEFT(p.misa_code, 100),
  true,
  COALESCE(p.created_at, NOW()),
  COALESCE(p.updated_at, p.created_at, NOW()),
  'legacy-migrate',
  'legacy-migrate',
  (p.deleted_at IS NOT NULL)
FROM legacy_ies."PRO_PRODUCT" p
WHERE p.code IS NOT NULL AND TRIM(p.code) <> ''
ON CONFLICT (id) DO NOTHING;

-- ---------------------------------------------------------------------------
-- users / roles: map core.SYS_USER carefully (password hashes, ACL).
-- Prefer regenerating permissions from V32/V33 catalog over blind ACL copy.
-- Use scripts/run_master_backfill_etl.py for dual-DB user gap fill.
-- ---------------------------------------------------------------------------

COMMIT;
