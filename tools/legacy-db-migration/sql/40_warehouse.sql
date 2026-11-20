-- Stage 40: warehouse — MANUAL/TRANSFORM stubs only.
-- New inbound receipts link to payment_requests (not old WAREHOUSE / SO-centric IMPORT_REQUEST).
-- Old ies.WAREHOUSE master has no new equivalent → SKIP.

BEGIN;

-- Read-only gap inventory on LEGACY:
-- SELECT 'IMPORT_REQUEST' AS t, COUNT(*) FROM ies."IMPORT_REQUEST" WHERE deleted_at IS NULL
-- UNION ALL SELECT 'EXPORT_REQUEST', COUNT(*) FROM ies."EXPORT_REQUEST" WHERE deleted_at IS NULL
-- UNION ALL SELECT 'INVENTORY', COUNT(*) FROM ies."INVENTORY" WHERE deleted_at IS NULL;

/*
-- Inventory 1:1 candidate sketch (verify WarehouseInventoryEntity first):
INSERT INTO warehouse_inventory (...)
SELECT ... FROM legacy_ies."INVENTORY" i
ON CONFLICT (id) DO NOTHING;
*/

-- BLOCKED: warehouse_inbound_receipts*  ← IMPORT_REQUEST* (needs payment_request_id policy)
-- Outbounds + inventory balances: see scripts/run_remaining_etl.py
-- Stock ledger: scripts/run_stock_transaction_etl.py (ies.INVENTORY_TRANSACTION)

DO $$
BEGIN
  RAISE NOTICE 'Stage 40 stub: use run_remaining_etl.py + run_stock_transaction_etl.py for warehouse loads.';
END $$;

COMMIT;
