-- Async Excel export jobs (products, vendors, customers, warehouse inventory).

CREATE TABLE export_jobs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    owner_user_id UUID NOT NULL REFERENCES users(id),
    type VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    file_info_id UUID REFERENCES file_infos(id),
    file_name VARCHAR(512),
    error_message TEXT,
    started_at TIMESTAMP,
    finished_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_export_jobs_owner_created
    ON export_jobs(owner_user_id, created_at DESC)
    WHERE is_deleted = FALSE;

COMMENT ON TABLE export_jobs IS 'Async Excel export jobs for master data and inventory';
COMMENT ON COLUMN export_jobs.type IS 'PRODUCTS, VENDORS, CUSTOMERS, WAREHOUSE_INVENTORY';
COMMENT ON COLUMN export_jobs.status IS 'PENDING, RUNNING, SUCCESS, FAILED';
