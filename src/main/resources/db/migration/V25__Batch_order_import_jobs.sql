-- Persisted async batch order import jobs and per-row/detail messages.

CREATE TABLE batch_order_import_jobs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    owner_user_id UUID NOT NULL REFERENCES users(id),
    file_info_id UUID NOT NULL REFERENCES file_infos(id),
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    original_file_name VARCHAR(512),
    total_rows INTEGER NOT NULL DEFAULT 0,
    success_rows INTEGER NOT NULL DEFAULT 0,
    error_rows INTEGER NOT NULL DEFAULT 0,
    warning_rows INTEGER NOT NULL DEFAULT 0,
    error_message TEXT,
    started_at TIMESTAMP,
    finished_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_batch_order_import_jobs_owner_created
    ON batch_order_import_jobs(owner_user_id, created_at DESC)
    WHERE is_deleted = FALSE;

COMMENT ON TABLE batch_order_import_jobs IS 'Async batch order Excel import jobs';
COMMENT ON COLUMN batch_order_import_jobs.status IS 'PENDING, RUNNING, SUCCESS, PARTIAL, FAILED';
COMMENT ON COLUMN batch_order_import_jobs.file_info_id IS 'Uploaded Excel stored in S3 via file_infos';

CREATE TABLE batch_order_import_job_details (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    job_id UUID NOT NULL REFERENCES batch_order_import_jobs(id) ON DELETE CASCADE,
    row_num INTEGER,
    level VARCHAR(20) NOT NULL,
    code VARCHAR(100),
    message TEXT NOT NULL,
    payload_json TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_batch_order_import_job_details_job_created
    ON batch_order_import_job_details(job_id, created_at ASC)
    WHERE is_deleted = FALSE;

COMMENT ON TABLE batch_order_import_job_details IS 'Warnings and errors for a batch order import job';
