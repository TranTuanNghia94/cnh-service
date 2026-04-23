-- V15: Create notifications table

CREATE TABLE notifications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id),
    title VARCHAR(255) NOT NULL,
    message TEXT NOT NULL,
    type VARCHAR(50) NOT NULL DEFAULT 'INFO',
    category VARCHAR(50),
    reference_id VARCHAR(255),
    reference_type VARCHAR(50),
    action_url VARCHAR(500),
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    read_at TIMESTAMP,
    priority VARCHAR(20) DEFAULT 'NORMAL',
    metadata TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_notification_user_read ON notifications(user_id, is_read) WHERE is_deleted = FALSE;
CREATE INDEX idx_notification_user_created ON notifications(user_id, created_at DESC) WHERE is_deleted = FALSE;
CREATE INDEX idx_notification_reference ON notifications(reference_id, reference_type) WHERE is_deleted = FALSE;

COMMENT ON TABLE notifications IS 'In-app notifications for users';
COMMENT ON COLUMN notifications.type IS 'INFO, SUCCESS, WARNING, ERROR, PAYMENT, APPROVAL, ORDER, SYSTEM';
COMMENT ON COLUMN notifications.category IS 'PAYMENT_REQUEST, PURCHASE_ORDER, SALES_ORDER, APPROVAL, SYSTEM';
COMMENT ON COLUMN notifications.reference_id IS 'ID of the related entity (e.g., payment request ID)';
COMMENT ON COLUMN notifications.reference_type IS 'Type of the related entity (e.g., PAYMENT_REQUEST)';
COMMENT ON COLUMN notifications.priority IS 'LOW, NORMAL, HIGH, URGENT';
