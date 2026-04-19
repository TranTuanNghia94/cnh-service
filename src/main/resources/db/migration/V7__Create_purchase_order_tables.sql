-- Drop existing purchase order tables (from V2 migration) and recreate with proper structure

DROP TABLE IF EXISTS purchase_order_lines;
DROP TABLE IF EXISTS purchase_orders;

-- Purchase Orders table
CREATE TABLE purchase_orders (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    version BIGINT NOT NULL DEFAULT 1,
    po_number INTEGER NOT NULL,
    po_prefix VARCHAR(10) NOT NULL,
    order_id UUID REFERENCES orders(id),
    order_date DATE NOT NULL DEFAULT CURRENT_DATE,
    expected_delivery_date DATE,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    notes TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    is_deleted BOOLEAN NOT NULL DEFAULT false
);

-- Purchase Order Lines table
CREATE TABLE purchase_order_lines (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    version BIGINT NOT NULL DEFAULT 1,
    purchase_order_id UUID NOT NULL REFERENCES purchase_orders(id) ON DELETE CASCADE,
    sale_orderline_id UUID REFERENCES order_lines(id),
    product_id UUID REFERENCES products(id),
    vendor_id UUID REFERENCES vendors(id),
    link TEXT,
    quantity DECIMAL(15,3) NOT NULL,
    uom_1 VARCHAR(50),
    uom_2 VARCHAR(50),
    unit_price DECIMAL(15,0) NOT NULL,
    is_tax_included BOOLEAN DEFAULT false,
    tax DECIMAL(5,2) DEFAULT 0,
    total_before_tax DECIMAL(15,0),
    total_price DECIMAL(15,0),
    currency VARCHAR(10),
    exchange_rate DECIMAL(15,4),
    total_price_vnd DECIMAL(15,0),
    note TEXT,
    quote TEXT,
    invoice TEXT,
    bill_of_ladding TEXT,
    receipt_warehouse TEXT,
    track_id TEXT,
    purchase_contract_number TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    is_deleted BOOLEAN NOT NULL DEFAULT false
);

-- Indexes for purchase_orders
CREATE INDEX idx_po_po_prefix ON purchase_orders(po_prefix);
CREATE INDEX idx_po_order_id ON purchase_orders(order_id);
CREATE INDEX idx_po_status ON purchase_orders(status);
CREATE INDEX idx_po_is_deleted ON purchase_orders(is_deleted);
CREATE INDEX idx_po_order_date ON purchase_orders(order_date);
CREATE INDEX idx_po_created_at ON purchase_orders(created_at);

-- Indexes for purchase_order_lines
CREATE INDEX idx_pol_purchase_order_id ON purchase_order_lines(purchase_order_id);
CREATE INDEX idx_pol_sale_orderline_id ON purchase_order_lines(sale_orderline_id);
CREATE INDEX idx_pol_product_id ON purchase_order_lines(product_id);
CREATE INDEX idx_pol_vendor_id ON purchase_order_lines(vendor_id);
CREATE INDEX idx_pol_is_deleted ON purchase_order_lines(is_deleted);
CREATE INDEX idx_pol_created_at ON purchase_order_lines(created_at);
