-- Renumber duplicate (order_prefix, order_number) pairs, then enforce uniqueness.

WITH duplicates AS (
    SELECT id,
           order_prefix,
           order_number,
           ROW_NUMBER() OVER (
               PARTITION BY order_prefix, order_number
               ORDER BY created_at ASC NULLS LAST, id ASC
           ) AS rn
    FROM orders
    WHERE is_deleted = FALSE
),
to_renumber AS (
    SELECT d.id,
           d.order_prefix,
           ROW_NUMBER() OVER (PARTITION BY d.order_prefix ORDER BY d.id) AS fix_seq
    FROM duplicates d
    WHERE d.rn > 1
),
prefix_max AS (
    SELECT order_prefix, COALESCE(MAX(order_number), 0) AS max_num
    FROM orders
    WHERE is_deleted = FALSE
    GROUP BY order_prefix
)
UPDATE orders o
SET order_number = pm.max_num + tr.fix_seq,
    updated_at = CURRENT_TIMESTAMP
FROM to_renumber tr
JOIN prefix_max pm ON pm.order_prefix = tr.order_prefix
WHERE o.id = tr.id;

CREATE UNIQUE INDEX IF NOT EXISTS uq_orders_prefix_number_active
    ON orders (order_prefix, order_number)
    WHERE is_deleted = FALSE;
