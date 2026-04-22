-- Improve payment request detail model:
-- 1) Add VND converted totals
-- 2) Normalize selected documents into child table
-- 3) Prevent duplicate PO line in one active payment request

ALTER TABLE payment_requests
    ADD COLUMN IF NOT EXISTS requested_amount_vnd DECIMAL(15,2) NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS fee_amount_vnd DECIMAL(15,2) NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS total_amount_vnd DECIMAL(15,2) NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS paid_amount_vnd DECIMAL(15,2) NOT NULL DEFAULT 0;

CREATE TABLE IF NOT EXISTS payment_request_item_documents (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    payment_request_item_id UUID NOT NULL REFERENCES payment_request_purchase_order_lines(id) ON DELETE CASCADE,
    document_type VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    is_deleted BOOLEAN NOT NULL DEFAULT false
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_pr_item_document_active
ON payment_request_item_documents(payment_request_item_id, document_type)
WHERE is_deleted = false;

CREATE UNIQUE INDEX IF NOT EXISTS uq_pr_po_line_active
ON payment_request_purchase_order_lines(payment_request_id, purchase_order_line_id)
WHERE is_deleted = false;

CREATE INDEX IF NOT EXISTS idx_pr_item_doc_item_id
ON payment_request_item_documents(payment_request_item_id);
