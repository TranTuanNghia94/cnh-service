-- Align orders and order_lines tables with Java entities

-- =============================================
-- Orders table: align with OrderEntity
-- =============================================

-- Add missing columns
ALTER TABLE orders ADD COLUMN version BIGINT NOT NULL DEFAULT 1;
ALTER TABLE orders ADD COLUMN order_prefix VARCHAR(10);
ALTER TABLE orders ADD COLUMN customer_address_id UUID REFERENCES customer_addresses(id);
ALTER TABLE orders ADD COLUMN contract_number VARCHAR(200);
ALTER TABLE orders ADD COLUMN is_included_tax BOOLEAN NOT NULL DEFAULT false;
ALTER TABLE orders ADD COLUMN tax_rate DECIMAL(5,2) DEFAULT 0;

-- Change order_number from VARCHAR to SERIAL (drop old, add new)
ALTER TABLE orders DROP CONSTRAINT IF EXISTS orders_order_number_key;
DROP INDEX IF EXISTS idx_orders_order_number;
ALTER TABLE orders DROP COLUMN order_number;
ALTER TABLE orders ADD COLUMN order_number SERIAL;

-- Change order_date and delivery_date from TIMESTAMP to DATE
ALTER TABLE orders ALTER COLUMN order_date TYPE DATE;
ALTER TABLE orders ALTER COLUMN delivery_date TYPE DATE;

-- Change status default from PENDING to DRAFT
ALTER TABLE orders ALTER COLUMN status SET DEFAULT 'DRAFT';

-- Change amount precision from DECIMAL(15,2) to DECIMAL(15,0)
ALTER TABLE orders ALTER COLUMN total_amount TYPE DECIMAL(15,0);
ALTER TABLE orders ALTER COLUMN total_amount SET NOT NULL;
ALTER TABLE orders ALTER COLUMN discount_amount TYPE DECIMAL(15,0);
ALTER TABLE orders ALTER COLUMN discount_amount SET NOT NULL;
ALTER TABLE orders ALTER COLUMN tax_amount TYPE DECIMAL(15,0);
ALTER TABLE orders ALTER COLUMN tax_amount SET NOT NULL;
ALTER TABLE orders ALTER COLUMN final_amount TYPE DECIMAL(15,0);
ALTER TABLE orders ALTER COLUMN final_amount SET NOT NULL;

-- Add indexes
CREATE INDEX IF NOT EXISTS idx_orders_customer_address_id ON orders(customer_address_id);
CREATE INDEX IF NOT EXISTS idx_orders_order_date ON orders(order_date);
CREATE INDEX IF NOT EXISTS idx_orders_delivery_date ON orders(delivery_date);
CREATE INDEX IF NOT EXISTS idx_orders_created_at ON orders(created_at);
CREATE INDEX IF NOT EXISTS idx_orders_is_deleted ON orders(is_deleted);

-- =============================================
-- Order lines table: align with OrderLineEntity
-- =============================================

-- Add missing columns
ALTER TABLE order_lines ADD COLUMN version BIGINT NOT NULL DEFAULT 1;
ALTER TABLE order_lines ADD COLUMN vendor_id UUID REFERENCES vendors(id);
ALTER TABLE order_lines ADD COLUMN product_code_suggest VARCHAR(200);
ALTER TABLE order_lines ADD COLUMN product_name_suggest VARCHAR(200);
ALTER TABLE order_lines ADD COLUMN vendor_code_suggest VARCHAR(200);
ALTER TABLE order_lines ADD COLUMN vendor_name_suggest VARCHAR(200);
ALTER TABLE order_lines ADD COLUMN uom VARCHAR(50);
ALTER TABLE order_lines ADD COLUMN is_included_tax BOOLEAN NOT NULL DEFAULT false;
ALTER TABLE order_lines ADD COLUMN receiver_note TEXT;
ALTER TABLE order_lines ADD COLUMN delivery_note TEXT;
ALTER TABLE order_lines ADD COLUMN reference_note TEXT;

-- Add ON DELETE CASCADE to order_id FK
ALTER TABLE order_lines DROP CONSTRAINT IF EXISTS order_lines_order_id_fkey;
ALTER TABLE order_lines ADD CONSTRAINT order_lines_order_id_fkey FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE;

-- Change quantity from INTEGER to DECIMAL(15,3)
ALTER TABLE order_lines ALTER COLUMN quantity TYPE DECIMAL(15,3);

-- Rename tax_percent to tax_rate
ALTER TABLE order_lines RENAME COLUMN tax_percent TO tax_rate;

-- Change amount precision from DECIMAL(15,2) to DECIMAL(15,0)
ALTER TABLE order_lines ALTER COLUMN unit_price TYPE DECIMAL(15,0);
ALTER TABLE order_lines ALTER COLUMN discount_amount TYPE DECIMAL(15,0);
ALTER TABLE order_lines ALTER COLUMN tax_amount TYPE DECIMAL(15,0);
ALTER TABLE order_lines ALTER COLUMN total_amount TYPE DECIMAL(15,0);

-- Add indexes
CREATE INDEX IF NOT EXISTS idx_order_lines_vendor_id ON order_lines(vendor_id);
CREATE INDEX IF NOT EXISTS idx_order_lines_is_deleted ON order_lines(is_deleted);
CREATE INDEX IF NOT EXISTS idx_order_lines_created_at ON order_lines(created_at);
