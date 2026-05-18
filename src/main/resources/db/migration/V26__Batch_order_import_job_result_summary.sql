ALTER TABLE batch_order_import_jobs
    ADD COLUMN result_summary_json TEXT;

COMMENT ON COLUMN batch_order_import_jobs.result_summary_json IS
    'JSON summary for UI: orders created, new products/vendors, errors (see BatchOrderImportResultSummary)';
