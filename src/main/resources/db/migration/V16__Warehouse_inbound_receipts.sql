-- Warehouse inbound receipts: keeper confirms received quantities and billing details against a payment request.

CREATE TABLE warehouse_inbound_receipts (
    id UUID PRIMARY KEY,
    payment_request_id UUID NOT NULL REFERENCES payment_requests(id),
    currency VARCHAR(10),
    exchange_rate DECIMAL(15, 6) NOT NULL DEFAULT 1,
    fee_amount DECIMAL(15, 2) NOT NULL DEFAULT 0,
    real_bill_amount DECIMAL(15, 2),
    bill_on_paper_amount DECIMAL(15, 2),
    note TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    is_deleted BOOLEAN NOT NULL DEFAULT false
);

CREATE INDEX idx_warehouse_inbound_receipts_payment_request_id
    ON warehouse_inbound_receipts (payment_request_id)
    WHERE is_deleted = false;

CREATE TABLE warehouse_inbound_receipt_lines (
    id UUID PRIMARY KEY,
    receipt_id UUID NOT NULL REFERENCES warehouse_inbound_receipts(id) ON DELETE CASCADE,
    payment_request_purchase_order_line_id UUID NOT NULL REFERENCES payment_request_purchase_order_lines(id),
    quantity_expected DECIMAL(15, 3),
    quantity_received DECIMAL(15, 3) NOT NULL,
    tax_percent DECIMAL(5, 2),
    line_note TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    is_deleted BOOLEAN NOT NULL DEFAULT false
);

CREATE INDEX idx_warehouse_inbound_receipt_lines_receipt_id
    ON warehouse_inbound_receipt_lines (receipt_id)
    WHERE is_deleted = false;

ALTER TABLE file_infos
    ADD COLUMN IF NOT EXISTS warehouse_inbound_receipt_id UUID REFERENCES warehouse_inbound_receipts(id) ON DELETE SET NULL;

CREATE INDEX IF NOT EXISTS idx_file_infos_warehouse_inbound_receipt_id
    ON file_infos (warehouse_inbound_receipt_id);
