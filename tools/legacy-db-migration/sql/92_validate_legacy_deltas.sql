-- Documentation of expected legacy↔ target deltas.
-- Prefer scripts/validate_etl_completeness.py (dual-DB) when FDW is incomplete.
--
-- Hard gaps (max_allowed = 0 for active rows):
--   products, customers, vendors, categories, orders, order_lines (active),
--   purchase_orders, purchase_order_lines, payment_requests,
--   warehouse_inbound_receipts, warehouse_inbound_receipt_lines
--
-- Soft / allowlisted residuals:
--   warehouse_outbound_details: up to ~150 unresolved (no order_line match)
--   warehouse_stock_transactions: zero-qty skips (~20)
--   file_infos: matched-only (disk ∩ SYS_MEDIA)
--   users: intentional SKIP for system accounts when username/email collide

SELECT 'see_validate_etl_completeness_py' AS note;
