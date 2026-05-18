-- order_number must be assigned only by the application (prefix + sequence).
-- SERIAL/sequence caused collisions when the app also set order_number explicitly.
ALTER TABLE orders ALTER COLUMN order_number DROP DEFAULT;

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_class WHERE relname = 'orders_order_number_seq') THEN
        ALTER SEQUENCE orders_order_number_seq OWNED BY NONE;
        DROP SEQUENCE orders_order_number_seq;
    END IF;
END $$;
