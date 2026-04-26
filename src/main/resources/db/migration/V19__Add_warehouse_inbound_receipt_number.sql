ALTER TABLE warehouse_inbound_receipts
    ADD COLUMN IF NOT EXISTS receipt_number VARCHAR(50);

CREATE UNIQUE INDEX IF NOT EXISTS ux_warehouse_inbound_receipts_receipt_number
    ON warehouse_inbound_receipts (receipt_number)
    WHERE is_deleted = false AND receipt_number IS NOT NULL;
