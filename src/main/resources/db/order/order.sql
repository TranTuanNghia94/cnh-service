-- Orders table
CREATE TABLE orders (
    version BIGINT NOT NULL DEFAULT 1,
    id uuid PRIMARY KEY,
    order_number VARCHAR(100) UNIQUE NOT NULL,
    customer_id uuid REFERENCES customers(id),
    customer_address_id uuid REFERENCES customer_addresses(id),
    contract_number VARCHAR(200) NOT NULL,
    order_date DATE NOT NULL,
    delivery_date DATE,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    total_amount DECIMAL(15,0) NOT NULL DEFAULT 0,
    discount_amount DECIMAL(15,0) NOT NULL DEFAULT 0,
    is_included_tax BOOLEAN NOT NULL DEFAULT false,
    tax_rate DECIMAL(5,2) DEFAULT 0,
    tax_amount DECIMAL(15,0) NOT NULL DEFAULT 0,
    final_amount DECIMAL(15,0) NOT NULL DEFAULT 0,
    notes TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    is_deleted BOOLEAN NOT NULL DEFAULT false
);

-- Primary indexes for orders table
CREATE INDEX idx_orders_order_number ON orders(order_number);
CREATE INDEX idx_orders_customer_id ON orders(customer_id);
CREATE INDEX idx_orders_customer_address_id ON orders(customer_address_id);
CREATE INDEX idx_orders_status ON orders(status);
CREATE INDEX idx_orders_order_date ON orders(order_date);
CREATE INDEX idx_orders_delivery_date ON orders(delivery_date);
CREATE INDEX idx_orders_created_at ON orders(created_at);
CREATE INDEX idx_orders_is_deleted ON orders(is_deleted);

-- Composite indexes for common query patterns
CREATE INDEX idx_orders_customer_status ON orders(customer_id, status);
CREATE INDEX idx_orders_status_date ON orders(status, order_date);
CREATE INDEX idx_orders_customer_date ON orders(customer_id, order_date);

-- Partial indexes for better performance on active records
CREATE INDEX idx_orders_active ON orders(customer_id, status, order_date) WHERE is_deleted = false;

CREATE TABLE order_lines (
    version BIGINT NOT NULL DEFAULT 1,
    id uuid PRIMARY KEY,
    order_id uuid NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    product_id uuid NOT NULL REFERENCES products(id),
    vendor_id uuid NOT NULL REFERENCES vendors(id),
    product_code_suggest VARCHAR(200),
    product_name_suggest VARCHAR(200),
    vendor_code_suggest VARCHAR(200),
    vendor_name_suggest VARCHAR(200),
    quantity DECIMAL(15,3) NOT NULL,
    unit_price DECIMAL(15,0) NOT NULL,
    uom VARCHAR(50) NOT NULL,
    discount_percent DECIMAL(5,2) DEFAULT 0,
    discount_amount DECIMAL(15,0) DEFAULT 0,
    is_included_tax BOOLEAN NOT NULL DEFAULT false,
    tax_rate DECIMAL(5,2) DEFAULT 0,
    tax_amount DECIMAL(15,0) DEFAULT 0,
    total_amount DECIMAL(15,0) NOT NULL,
    notes TEXT,
    receiver_note TEXT,
    delivery_note TEXT,
    reference_note TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    is_deleted BOOLEAN NOT NULL DEFAULT false
);

-- Primary indexes for order_lines table
CREATE INDEX idx_order_lines_order_id ON order_lines(order_id);
CREATE INDEX idx_order_lines_product_id ON order_lines(product_id);
CREATE INDEX idx_order_lines_vendor_id ON order_lines(vendor_id);
CREATE INDEX idx_order_lines_is_deleted ON order_lines(is_deleted);
CREATE INDEX idx_order_lines_created_at ON order_lines(created_at);

-- Composite indexes for common query patterns
CREATE INDEX idx_order_lines_order_product ON order_lines(order_id, product_id);
CREATE INDEX idx_order_lines_product_vendor ON order_lines(product_id, vendor_id);
CREATE INDEX idx_order_lines_order_vendor ON order_lines(order_id, vendor_id);

-- Partial indexes for better performance on active records
CREATE INDEX idx_order_lines_active ON order_lines(order_id, product_id, vendor_id) WHERE is_deleted = false;
CREATE INDEX idx_order_lines_order_active ON order_lines(order_id) WHERE is_deleted = false;
