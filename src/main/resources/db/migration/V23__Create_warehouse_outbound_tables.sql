CREATE TABLE IF NOT EXISTS warehouse_outbounds (
    id UUID PRIMARY KEY,
    outbound_number VARCHAR(50) NOT NULL UNIQUE,
    order_id UUID NOT NULL REFERENCES orders(id),
    contract_number VARCHAR(200) NOT NULL,
    outbound_reason VARCHAR(200) NOT NULL,
    currency VARCHAR(10) NOT NULL,
    outbound_date DATE NOT NULL,
    total_amount DECIMAL(15, 2) NOT NULL DEFAULT 0,
    tax_amount DECIMAL(15, 2) NOT NULL DEFAULT 0,
    note TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    is_deleted BOOLEAN NOT NULL DEFAULT false
);

CREATE INDEX IF NOT EXISTS idx_warehouse_outbounds_order_id ON warehouse_outbounds(order_id);
CREATE INDEX IF NOT EXISTS idx_warehouse_outbounds_contract_number ON warehouse_outbounds(contract_number);

CREATE TABLE IF NOT EXISTS warehouse_outbound_details (
    id UUID PRIMARY KEY,
    outbound_id UUID NOT NULL REFERENCES warehouse_outbounds(id),
    order_line_id UUID NOT NULL REFERENCES order_lines(id),
    product_id UUID NOT NULL REFERENCES products(id),
    quantity DECIMAL(15, 3) NOT NULL,
    box VARCHAR(200),
    reference_code VARCHAR(400),
    unit_price DECIMAL(15, 2) NOT NULL DEFAULT 0,
    vat DECIMAL(6, 2) NOT NULL DEFAULT 0,
    currency VARCHAR(10) NOT NULL,
    total_amount DECIMAL(15, 2) NOT NULL DEFAULT 0,
    tax_amount DECIMAL(15, 2) NOT NULL DEFAULT 0,
    note TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    is_deleted BOOLEAN NOT NULL DEFAULT false
);

CREATE INDEX IF NOT EXISTS idx_warehouse_outbound_details_outbound_id ON warehouse_outbound_details(outbound_id);
CREATE INDEX IF NOT EXISTS idx_warehouse_outbound_details_product_id ON warehouse_outbound_details(product_id);
