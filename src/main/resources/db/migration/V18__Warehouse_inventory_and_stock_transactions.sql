-- Per-product warehouse quantity and immutable stock movements (inbound / outbound).

ALTER TABLE warehouse_inbound_receipts
    ADD COLUMN IF NOT EXISTS inventory_posted_at TIMESTAMP;

CREATE TABLE IF NOT EXISTS warehouse_inventory (
    id UUID PRIMARY KEY,
    product_id UUID NOT NULL REFERENCES products(id),
    quantity_on_hand DECIMAL(15, 3) NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    is_deleted BOOLEAN NOT NULL DEFAULT false
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_warehouse_inventory_product_id ON warehouse_inventory (product_id)
    WHERE is_deleted = false;

CREATE TABLE IF NOT EXISTS warehouse_stock_transactions (
    id UUID PRIMARY KEY,
    product_id UUID NOT NULL REFERENCES products(id),
    direction VARCHAR(20) NOT NULL,
    quantity DECIMAL(15, 3) NOT NULL,
    reference_type VARCHAR(80) NOT NULL,
    reference_id UUID,
    note TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    is_deleted BOOLEAN NOT NULL DEFAULT false
);

CREATE INDEX IF NOT EXISTS idx_wst_product_id_created ON warehouse_stock_transactions (product_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_wst_reference ON warehouse_stock_transactions (reference_type, reference_id);
