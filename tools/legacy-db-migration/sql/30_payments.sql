-- Stage 30: payments — MANUAL/TRANSFORM stubs only.
-- New model: payment_requests + PO-line joins + fees + approvals + item documents.
-- Do NOT run a blind INSERT until product mapping is approved.

BEGIN;

-- Gap inventory (run on LEGACY; read-only):
-- SELECT p.id, p.status, COUNT(pl.id) AS line_count
-- FROM ies."PAY_PAYMENT" p
-- LEFT JOIN ies."PAY_PAYMENT_LINE" pl ON pl.payment_id = p.id
-- WHERE p.deleted_at IS NULL
-- GROUP BY p.id, p.status;

/*
-- Example header-only sketch (INCOMPLETE — not production-ready):
INSERT INTO payment_requests (
  id, created_at, updated_at, is_deleted
)
SELECT
  p.id,
  COALESCE(p.created_at, NOW()),
  p.updated_at,
  (p.deleted_at IS NOT NULL)
FROM legacy_ies."PAY_PAYMENT" p
ON CONFLICT (id) DO NOTHING;
*/

-- BLOCKED: payment_request_purchase_order_lines
-- BLOCKED: payment_request_extra_fees
-- BLOCKED: payment_request_approvals  ← PAY_APPROVAL_HISTORY
-- BLOCKED: payment_request_item_documents

DO $$
BEGIN
  RAISE NOTICE 'Stage 30 is stubbed (MANUAL/TRANSFORM). No rows loaded.';
END $$;

COMMIT;
