-- Align database tables with Java entities

-- =============================================
-- Categories: add code and unit columns
-- =============================================
ALTER TABLE categories ADD COLUMN code VARCHAR(50);
ALTER TABLE categories ADD COLUMN unit VARCHAR(100);

-- Populate code for existing categories
UPDATE categories SET code = 'ELECTRONICS' WHERE name = 'Electronics';
UPDATE categories SET code = 'CLOTHING' WHERE name = 'Clothing';
UPDATE categories SET code = 'FOOD_BEVERAGES' WHERE name = 'Food & Beverages';
UPDATE categories SET code = 'BOOKS' WHERE name = 'Books';
UPDATE categories SET code = 'HOME_GARDEN' WHERE name = 'Home & Garden';

-- Now apply constraints
ALTER TABLE categories ALTER COLUMN code SET NOT NULL;
ALTER TABLE categories ADD CONSTRAINT categories_code_unique UNIQUE (code);

-- =============================================
-- Products: add barcode, tax, misa_code; rename unit to unit_1, add unit_2; drop unused columns
-- =============================================
ALTER TABLE products ADD COLUMN barcode VARCHAR(100);
ALTER TABLE products RENAME COLUMN unit TO unit_1;
ALTER TABLE products ADD COLUMN unit_2 VARCHAR(20);
ALTER TABLE products ADD COLUMN tax DECIMAL(15,2) NOT NULL DEFAULT 0;
ALTER TABLE products ADD COLUMN misa_code VARCHAR(100);
ALTER TABLE products DROP COLUMN IF EXISTS min_stock;
ALTER TABLE products DROP COLUMN IF EXISTS max_stock;
ALTER TABLE products DROP COLUMN IF EXISTS current_stock;

-- =============================================
-- Customers: add misa_code; adjust code/name lengths; drop address/contact_person
-- =============================================
ALTER TABLE customers ALTER COLUMN code TYPE VARCHAR(100);
ALTER TABLE customers ALTER COLUMN name TYPE VARCHAR(300);
ALTER TABLE customers ADD COLUMN misa_code VARCHAR(50);
ALTER TABLE customers DROP COLUMN IF EXISTS address;
ALTER TABLE customers DROP COLUMN IF EXISTS contact_person;

-- =============================================
-- Customer addresses table
-- =============================================
CREATE TABLE IF NOT EXISTS customer_addresses (
    id uuid PRIMARY KEY,
    customer_id uuid NOT NULL REFERENCES customers(id),
    address TEXT,
    contact_person VARCHAR(100),
    phone VARCHAR(20),
    email VARCHAR(100),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    is_deleted BOOLEAN NOT NULL DEFAULT false
);

CREATE INDEX IF NOT EXISTS idx_customer_addresses_customer_id ON customer_addresses(customer_id);

-- =============================================
-- Vendors table
-- =============================================
CREATE TABLE IF NOT EXISTS vendors (
    id uuid PRIMARY KEY,
    code VARCHAR(50) UNIQUE NOT NULL,
    name VARCHAR(200) NOT NULL,
    email VARCHAR(100),
    country VARCHAR(100),
    currency VARCHAR(100),
    phone VARCHAR(20),
    misa_code VARCHAR(100),
    address TEXT,
    tax_code VARCHAR(50),
    contact_person VARCHAR(100),
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    is_deleted BOOLEAN NOT NULL DEFAULT false
);

CREATE INDEX IF NOT EXISTS idx_vendors_code ON vendors(code);
CREATE INDEX IF NOT EXISTS idx_vendors_is_active ON vendors(is_active);

-- =============================================
-- Vendor banks table
-- =============================================
CREATE TABLE IF NOT EXISTS vendor_banks (
    id uuid PRIMARY KEY,
    vendor_id uuid NOT NULL REFERENCES vendors(id),
    bank_name VARCHAR(100),
    bank_account_name VARCHAR(200),
    bank_account_number VARCHAR(100),
    bank_account_branch VARCHAR(200),
    bank_account_swift VARCHAR(100),
    bank_account_iban VARCHAR(100),
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    is_deleted BOOLEAN NOT NULL DEFAULT false
);

CREATE INDEX IF NOT EXISTS idx_vendor_banks_vendor_id ON vendor_banks(vendor_id);
CREATE INDEX IF NOT EXISTS idx_vendor_banks_is_active ON vendor_banks(is_active);
