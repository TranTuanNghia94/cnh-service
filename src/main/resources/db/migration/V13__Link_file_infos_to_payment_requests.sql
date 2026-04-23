-- Link uploaded files to a payment request and record whether they are general papers or bank-note proof.

ALTER TABLE file_infos
    ADD COLUMN IF NOT EXISTS payment_request_id UUID REFERENCES payment_requests(id) ON DELETE SET NULL,
    ADD COLUMN IF NOT EXISTS attachment_type VARCHAR(50) NOT NULL DEFAULT 'PAPER';

CREATE INDEX IF NOT EXISTS idx_file_infos_payment_request_id ON file_infos(payment_request_id);
CREATE INDEX IF NOT EXISTS idx_file_infos_pr_attachment ON file_infos(payment_request_id, attachment_type);
