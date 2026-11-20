-- Extract SYS_MEDIA for join with disk files/ (basename == name).
-- Run on LEGACY_DATABASE_URL (cnh_legacy).
-- Note: banknote column is quoted camelCase in the dump.
-- Prefer CSV via scripts/build_files_manifest.sh (\copy).

SELECT
  m.id,
  m.name,
  m.ext,
  m.mime,
  m.size,
  m.uri,
  m.req_payment_id,
  m."banknote_paymentRequest_id" AS banknote_payment_request_id,
  m.import_request_document_id,
  m.created_at,
  m.updated_at,
  m.deleted_at,
  (m.deleted_at IS NOT NULL) AS is_deleted
FROM core."SYS_MEDIA" m
ORDER BY m.name;
