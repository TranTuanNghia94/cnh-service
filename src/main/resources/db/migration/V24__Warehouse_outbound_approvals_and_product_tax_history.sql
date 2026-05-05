ALTER TABLE warehouse_outbounds
    ADD COLUMN IF NOT EXISTS status VARCHAR(50) NOT NULL DEFAULT 'DRAFT',
    ADD COLUMN IF NOT EXISTS approval_levels INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS current_approval_level INTEGER NOT NULL DEFAULT 0;

ALTER TABLE warehouse_outbound_details
    ADD COLUMN IF NOT EXISTS price_without_tax DECIMAL(15, 2) NOT NULL DEFAULT 0;

CREATE TABLE IF NOT EXISTS warehouse_outbound_approvals (
    id UUID PRIMARY KEY,
    outbound_id UUID NOT NULL REFERENCES warehouse_outbounds(id),
    approval_level INTEGER NOT NULL,
    approval_role VARCHAR(50) NOT NULL,
    approver_id UUID REFERENCES users(id),
    status VARCHAR(20) NOT NULL,
    approved_at TIMESTAMP,
    rejection_reason TEXT,
    note TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    is_deleted BOOLEAN NOT NULL DEFAULT false
);

CREATE INDEX IF NOT EXISTS idx_warehouse_outbound_approvals_outbound_id
    ON warehouse_outbound_approvals(outbound_id);
CREATE INDEX IF NOT EXISTS idx_warehouse_outbound_approvals_level
    ON warehouse_outbound_approvals(outbound_id, approval_level);

ALTER TABLE file_infos
    ADD COLUMN IF NOT EXISTS warehouse_outbound_id UUID;

CREATE INDEX IF NOT EXISTS idx_file_infos_warehouse_outbound_id
    ON file_infos (warehouse_outbound_id);

CREATE TABLE IF NOT EXISTS product_tax_histories (
    id UUID PRIMARY KEY,
    product_id UUID NOT NULL REFERENCES products(id),
    old_tax DECIMAL(15, 2) NOT NULL,
    new_tax DECIMAL(15, 2) NOT NULL,
    source_type VARCHAR(80) NOT NULL,
    source_id UUID,
    note TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    is_deleted BOOLEAN NOT NULL DEFAULT false
);

CREATE INDEX IF NOT EXISTS idx_product_tax_histories_product_id
    ON product_tax_histories(product_id);
