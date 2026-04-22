-- Enhance payment request to support PO line payments, dynamic fees, approvals, and payment evidence.

ALTER TABLE payment_requests
    ADD COLUMN IF NOT EXISTS vendor_id UUID REFERENCES vendors(id),
    ADD COLUMN IF NOT EXISTS approval_levels INTEGER NOT NULL DEFAULT 2,
    ADD COLUMN IF NOT EXISTS current_approval_level INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS requested_amount DECIMAL(15,2) NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS fee_amount DECIMAL(15,2) NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS total_amount DECIMAL(15,2) NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS paid_amount DECIMAL(15,2) NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS exchange_rate DECIMAL(15,6) NOT NULL DEFAULT 1,
    ADD COLUMN IF NOT EXISTS papers TEXT,
    ADD COLUMN IF NOT EXISTS bank_info TEXT,
    ADD COLUMN IF NOT EXISTS bank_note TEXT,
    ADD COLUMN IF NOT EXISTS paid_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS paid_by UUID REFERENCES users(id);

CREATE TABLE IF NOT EXISTS payment_request_purchase_order_lines (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    payment_request_id UUID NOT NULL REFERENCES payment_requests(id) ON DELETE CASCADE,
    purchase_order_line_id UUID NOT NULL REFERENCES purchase_order_lines(id),
    selected_documents TEXT NOT NULL,
    requested_amount DECIMAL(15,2) NOT NULL,
    paid_amount DECIMAL(15,2) NOT NULL DEFAULT 0,
    note TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    is_deleted BOOLEAN NOT NULL DEFAULT false
);

CREATE TABLE IF NOT EXISTS payment_request_extra_fees (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    payment_request_id UUID NOT NULL REFERENCES payment_requests(id) ON DELETE CASCADE,
    fee_name VARCHAR(100) NOT NULL,
    fee_type VARCHAR(50),
    amount DECIMAL(15,2) NOT NULL,
    note TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    is_deleted BOOLEAN NOT NULL DEFAULT false
);

CREATE TABLE IF NOT EXISTS payment_request_approvals (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    payment_request_id UUID NOT NULL REFERENCES payment_requests(id) ON DELETE CASCADE,
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

CREATE INDEX IF NOT EXISTS idx_payment_requests_vendor_id ON payment_requests(vendor_id);
CREATE INDEX IF NOT EXISTS idx_payment_requests_paid_by ON payment_requests(paid_by);
CREATE INDEX IF NOT EXISTS idx_pr_pol_request_id ON payment_request_purchase_order_lines(payment_request_id);
CREATE INDEX IF NOT EXISTS idx_pr_pol_po_line_id ON payment_request_purchase_order_lines(purchase_order_line_id);
CREATE INDEX IF NOT EXISTS idx_pr_fee_request_id ON payment_request_extra_fees(payment_request_id);
CREATE INDEX IF NOT EXISTS idx_pr_approval_request_id ON payment_request_approvals(payment_request_id);
