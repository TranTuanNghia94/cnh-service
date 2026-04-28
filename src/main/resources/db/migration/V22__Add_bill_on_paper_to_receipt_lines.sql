ALTER TABLE warehouse_inbound_receipt_lines
    ADD COLUMN IF NOT EXISTS bill_on_paper TEXT;

ALTER TABLE warehouse_inbound_receipt_lines
    ADD COLUMN IF NOT EXISTS tax_included BOOLEAN NOT NULL DEFAULT false;
