CREATE TABLE vendors (
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

CREATE TABLE vendor_banks (
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

CREATE INDEX idx_vendors_code ON vendors(code);
CREATE INDEX idx_vendors_is_active ON vendors(is_active);
CREATE INDEX idx_vendor_banks_vendor_id ON vendor_banks(vendor_id);
CREATE INDEX idx_vendor_banks_is_active ON vendor_banks(is_active);
CREATE INDEX idx_vendor_banks_is_deleted ON vendor_banks(is_deleted);