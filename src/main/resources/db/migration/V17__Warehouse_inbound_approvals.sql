-- N-level approval workflow for warehouse inbound receipts (mirrors payment_request_approvals pattern).

ALTER TABLE warehouse_inbound_receipts
    ADD COLUMN IF NOT EXISTS status VARCHAR(50) NOT NULL DEFAULT 'APPROVED',
    ADD COLUMN IF NOT EXISTS approval_levels INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS current_approval_level INTEGER NOT NULL DEFAULT 0;

CREATE TABLE IF NOT EXISTS warehouse_inbound_receipt_approvals (
    id UUID PRIMARY KEY,
    receipt_id UUID NOT NULL REFERENCES warehouse_inbound_receipts(id) ON DELETE CASCADE,
    approval_level INTEGER NOT NULL,
    approval_role VARCHAR(50) NOT NULL,
    approver_id UUID REFERENCES users(id),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    approved_at TIMESTAMP,
    rejection_reason TEXT,
    note TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    is_deleted BOOLEAN NOT NULL DEFAULT false
);

CREATE INDEX IF NOT EXISTS idx_whir_approval_receipt_id ON warehouse_inbound_receipt_approvals(receipt_id);
