-- Store JSON parameters for service report export jobs (date range and filters).

ALTER TABLE export_jobs
    ADD COLUMN IF NOT EXISTS report_params TEXT;

COMMENT ON COLUMN export_jobs.report_params IS 'JSON payload for SERVICE_* report export types (fromDate, toDate, filters)';
