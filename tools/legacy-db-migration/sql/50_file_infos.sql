-- Stage 50: load file_infos from staging table legacy_media_sync.
-- Prerequisite: scripts/load_file_infos.sh created+filled legacy_media_sync from
--   out/files_sync_manifest.csv (matched rows only, after S3 sync recommended).
-- payment_request_id / warehouse FKs: NULL if target row missing (SET NULL on FK).

BEGIN;

-- Drop payment/warehouse FKs temporarily only if inserts fail — prefer soft NULL.
-- Null out payment_request_id when payment_requests row is absent.
UPDATE legacy_media_sync s
SET payment_request_id = NULL
WHERE s.payment_request_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM payment_requests pr
    WHERE pr.id = s.payment_request_id AND pr.is_deleted = false
  );

UPDATE legacy_media_sync s
SET warehouse_inbound_receipt_id = NULL
WHERE s.warehouse_inbound_receipt_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM warehouse_inbound_receipts w
    WHERE w.id = s.warehouse_inbound_receipt_id
  );

INSERT INTO file_infos (
  id,
  file_name,
  file_path,
  content_type,
  file_size_bytes,
  category,
  payment_request_id,
  warehouse_inbound_receipt_id,
  attachment_type,
  created_at,
  updated_at,
  created_by,
  updated_by,
  is_deleted
)
SELECT
  s.id,
  s.file_name,
  s.s3_key AS file_path,
  s.content_type,
  s.file_size_bytes,
  s.category,
  s.payment_request_id,
  s.warehouse_inbound_receipt_id,
  s.attachment_type,
  COALESCE(s.created_at, NOW()),
  s.updated_at,
  'legacy-migrate',
  'legacy-migrate',
  COALESCE(s.is_deleted, false)
FROM legacy_media_sync s
WHERE s.match_status = 'matched'
ON CONFLICT (id) DO UPDATE SET
  file_name = EXCLUDED.file_name,
  file_path = EXCLUDED.file_path,
  content_type = EXCLUDED.content_type,
  file_size_bytes = EXCLUDED.file_size_bytes,
  category = EXCLUDED.category,
  payment_request_id = EXCLUDED.payment_request_id,
  warehouse_inbound_receipt_id = EXCLUDED.warehouse_inbound_receipt_id,
  attachment_type = EXCLUDED.attachment_type,
  updated_at = EXCLUDED.updated_at,
  is_deleted = EXCLUDED.is_deleted;
-- created_by/updated_by remapped by scripts/backfill_audit_usernames.py (SYS_MEDIA UUID → username)

COMMIT;
