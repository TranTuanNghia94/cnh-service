-- Persisted metadata for uploaded files (e.g. payment request attachments in S3).

CREATE TABLE IF NOT EXISTS file_infos (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    file_name VARCHAR(512) NOT NULL,
    file_url TEXT NOT NULL,
    content_type VARCHAR(255),
    file_size_bytes BIGINT,
    category VARCHAR(100),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    is_deleted BOOLEAN NOT NULL DEFAULT false
);

CREATE INDEX IF NOT EXISTS idx_file_infos_category ON file_infos(category);
CREATE INDEX IF NOT EXISTS idx_file_infos_created_at ON file_infos(created_at);
