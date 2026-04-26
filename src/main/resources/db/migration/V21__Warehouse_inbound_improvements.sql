-- Fix #2: Allow standalone PO-based inbound (payment_request_id becomes nullable)
ALTER TABLE warehouse_inbound_receipts
    ALTER COLUMN payment_request_id DROP NOT NULL;

-- Fix #6: Add received_date so actual goods-receipt date can differ from created_at
ALTER TABLE warehouse_inbound_receipts
    ADD COLUMN IF NOT EXISTS received_date DATE;

-- Fix #2: Add direct PO line reference so receipt lines work without a payment request
ALTER TABLE warehouse_inbound_receipt_lines
    ADD COLUMN IF NOT EXISTS purchase_order_line_id UUID REFERENCES purchase_order_lines(id);

ALTER TABLE warehouse_inbound_receipt_lines
    ALTER COLUMN payment_request_purchase_order_line_id DROP NOT NULL;

CREATE INDEX IF NOT EXISTS idx_whirl_purchase_order_line_id
    ON warehouse_inbound_receipt_lines (purchase_order_line_id)
    WHERE is_deleted = false;

-- Fix #7: Unique constraint to prevent duplicate PO lines on the same receipt
CREATE UNIQUE INDEX IF NOT EXISTS uq_whirl_receipt_prpol
    ON warehouse_inbound_receipt_lines (receipt_id, payment_request_purchase_order_line_id)
    WHERE is_deleted = false AND payment_request_purchase_order_line_id IS NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uq_whirl_receipt_pol
    ON warehouse_inbound_receipt_lines (receipt_id, purchase_order_line_id)
    WHERE is_deleted = false AND purchase_order_line_id IS NOT NULL;
