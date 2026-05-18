-- order_prefix is shared per month (e.g. OD_2026_05). Uniqueness is (order_prefix, order_number).
-- version is JPA optimistic-lock, not the business order sequence.

-- Fix rows created while SERIAL/default conflicted with app-assigned numbers.
WITH bad AS (
    SELECT id,
           order_prefix,
           ROW_NUMBER() OVER (PARTITION BY order_prefix ORDER BY created_at ASC NULLS LAST, id ASC) AS rn
    FROM orders
    WHERE is_deleted = FALSE
      AND (order_number IS NULL OR order_number <= 0)
),
prefix_max AS (
    SELECT order_prefix, COALESCE(MAX(order_number), 0) AS max_num
    FROM orders
    WHERE is_deleted = FALSE
      AND order_number > 0
    GROUP BY order_prefix
)
UPDATE orders o
SET order_number = COALESCE(pm.max_num, 0) + bad.rn,
    updated_at = CURRENT_TIMESTAMP
FROM bad
LEFT JOIN prefix_max pm ON pm.order_prefix = bad.order_prefix
WHERE o.id = bad.id;

-- Readable order code in DB tools: OD_2026_05.42
ALTER TABLE orders
    ADD COLUMN IF NOT EXISTS order_code VARCHAR(32)
        GENERATED ALWAYS AS (order_prefix || '.' || order_number::text) STORED;

CREATE INDEX IF NOT EXISTS idx_orders_order_code ON orders (order_code) WHERE is_deleted = FALSE;

COMMENT ON COLUMN orders.order_prefix IS 'Monthly bucket, e.g. OD_2026_05; shared by many orders';
COMMENT ON COLUMN orders.order_number IS 'Sequence within order_prefix; unique per prefix';
COMMENT ON COLUMN orders.order_code IS 'Display code: order_prefix || ''.'' || order_number';
COMMENT ON COLUMN orders.version IS 'JPA optimistic-lock counter; not the order sequence';
