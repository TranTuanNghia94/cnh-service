-- Orphan / integrity checks on TARGET. Expect 0 rows from each query after a good load.

-- vendor_banks without vendor
SELECT 'orphan_vendor_banks' AS check_name, vb.id
FROM vendor_banks vb
LEFT JOIN vendors v ON v.id = vb.vendor_id
WHERE v.id IS NULL;

-- order_lines without order
SELECT 'orphan_order_lines' AS check_name, ol.id
FROM order_lines ol
LEFT JOIN orders o ON o.id = ol.order_id
WHERE o.id IS NULL;

-- order_lines without product
SELECT 'order_lines_missing_product' AS check_name, ol.id
FROM order_lines ol
LEFT JOIN products p ON p.id = ol.product_id
WHERE ol.product_id IS NOT NULL AND p.id IS NULL;

-- orders without customer (when customer_id NOT NULL)
SELECT 'orders_missing_customer' AS check_name, o.id
FROM orders o
LEFT JOIN customers c ON c.id = o.customer_id
WHERE o.customer_id IS NOT NULL AND c.id IS NULL;

-- purchase_order_lines without PO
SELECT 'orphan_po_lines' AS check_name, pol.id
FROM purchase_order_lines pol
LEFT JOIN purchase_orders po ON po.id = pol.purchase_order_id
WHERE po.id IS NULL;

-- Soft-delete sanity: banks for deleted vendors still marked active (informational)
SELECT 'active_bank_on_deleted_vendor' AS check_name, vb.id
FROM vendor_banks vb
JOIN vendors v ON v.id = vb.vendor_id
WHERE v.is_deleted = true AND vb.is_deleted = false AND vb.is_active = true;

-- file_infos pointing at missing payment_requests
SELECT 'file_infos_missing_payment' AS check_name, f.id
FROM file_infos f
LEFT JOIN payment_requests pr ON pr.id = f.payment_request_id
WHERE f.payment_request_id IS NOT NULL AND pr.id IS NULL;

-- inventory without product
SELECT 'inventory_missing_product' AS check_name, i.id
FROM warehouse_inventory i
LEFT JOIN products p ON p.id = i.product_id
WHERE p.id IS NULL;

-- stock transactions without product
SELECT 'stock_tx_missing_product' AS check_name, t.id
FROM warehouse_stock_transactions t
LEFT JOIN products p ON p.id = t.product_id
WHERE p.id IS NULL;

-- outbound details missing parents
SELECT 'outbound_detail_missing_outbound' AS check_name, d.id
FROM warehouse_outbound_details d
LEFT JOIN warehouse_outbounds o ON o.id = d.outbound_id
WHERE o.id IS NULL;

SELECT 'outbound_detail_missing_product' AS check_name, d.id
FROM warehouse_outbound_details d
LEFT JOIN products p ON p.id = d.product_id
WHERE p.id IS NULL;

SELECT 'outbound_detail_missing_order_line' AS check_name, d.id
FROM warehouse_outbound_details d
LEFT JOIN order_lines ol ON ol.id = d.order_line_id
WHERE ol.id IS NULL;

-- inbound lines missing receipt
SELECT 'inbound_line_missing_receipt' AS check_name, l.id
FROM warehouse_inbound_receipt_lines l
LEFT JOIN warehouse_inbound_receipts r ON r.id = l.receipt_id
WHERE r.id IS NULL;
