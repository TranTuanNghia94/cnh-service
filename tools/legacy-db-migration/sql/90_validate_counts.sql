-- Run on TARGET after a stage load. Compare mentally to 01_extract_counts.sql output.

SELECT 'users' AS table_name, COUNT(*) AS row_count FROM users
UNION ALL SELECT 'roles', COUNT(*) FROM roles
UNION ALL SELECT 'vendors', COUNT(*) FROM vendors
UNION ALL SELECT 'vendor_banks', COUNT(*) FROM vendor_banks
UNION ALL SELECT 'customers', COUNT(*) FROM customers
UNION ALL SELECT 'customer_addresses', COUNT(*) FROM customer_addresses
UNION ALL SELECT 'categories', COUNT(*) FROM categories
UNION ALL SELECT 'products', COUNT(*) FROM products
UNION ALL SELECT 'orders', COUNT(*) FROM orders
UNION ALL SELECT 'order_lines', COUNT(*) FROM order_lines
UNION ALL SELECT 'purchase_orders', COUNT(*) FROM purchase_orders
UNION ALL SELECT 'purchase_order_lines', COUNT(*) FROM purchase_order_lines
UNION ALL SELECT 'payment_requests', COUNT(*) FROM payment_requests
UNION ALL SELECT 'payment_request_purchase_order_lines', COUNT(*) FROM payment_request_purchase_order_lines
UNION ALL SELECT 'payment_request_extra_fees', COUNT(*) FROM payment_request_extra_fees
UNION ALL SELECT 'payment_request_approvals', COUNT(*) FROM payment_request_approvals
UNION ALL SELECT 'warehouse_inbound_receipts', COUNT(*) FROM warehouse_inbound_receipts
UNION ALL SELECT 'warehouse_inbound_receipt_lines', COUNT(*) FROM warehouse_inbound_receipt_lines
UNION ALL SELECT 'warehouse_outbounds', COUNT(*) FROM warehouse_outbounds
UNION ALL SELECT 'warehouse_outbound_details', COUNT(*) FROM warehouse_outbound_details
UNION ALL SELECT 'warehouse_inventory', COUNT(*) FROM warehouse_inventory
UNION ALL SELECT 'warehouse_stock_transactions', COUNT(*) FROM warehouse_stock_transactions
UNION ALL SELECT 'file_infos', COUNT(*) FROM file_infos
UNION ALL SELECT 'file_infos_active', COUNT(*) FROM file_infos WHERE is_deleted = false
ORDER BY 1;
