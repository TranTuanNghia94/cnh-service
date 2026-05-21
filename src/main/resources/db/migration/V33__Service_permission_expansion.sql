-- Fine-grained payment and warehouse permissions for frontend/service authorization

INSERT INTO permissions (id, name, code, description, resource, action) VALUES
-- Payment workflow
('b1b2c3d4-5555-4000-8000-000000000001', 'Payment Approve Level 1', 'PAYMENT_APPROVE_LEVEL_1', 'Approve payment requests at accountant level', 'PAYMENT', 'APPROVE_LEVEL_1'),
('b1b2c3d4-5555-4000-8000-000000000002', 'Payment Approve Level 2', 'PAYMENT_APPROVE_LEVEL_2', 'Approve payment requests at head accountant level', 'PAYMENT', 'APPROVE_LEVEL_2'),
('b1b2c3d4-5555-4000-8000-000000000003', 'Payment Approve Final', 'PAYMENT_APPROVE_FINAL', 'Final approval for payment requests', 'PAYMENT', 'APPROVE_FINAL'),
('b1b2c3d4-5555-4000-8000-000000000004', 'Payment Upload Bank Note', 'PAYMENT_UPLOAD_BANK_NOTE', 'Upload bank note when marking payment paid', 'PAYMENT', 'UPLOAD_BANK_NOTE'),
-- Warehouse inbound
('b1b2c3d4-5555-4000-8000-000000000010', 'Warehouse Inbound Read', 'WAREHOUSE_INBOUND_READ', 'View warehouse inbound receipts', 'WAREHOUSE_INBOUND', 'READ'),
('b1b2c3d4-5555-4000-8000-000000000011', 'Warehouse Inbound Create', 'WAREHOUSE_INBOUND_CREATE', 'Create warehouse inbound receipts', 'WAREHOUSE_INBOUND', 'CREATE'),
('b1b2c3d4-5555-4000-8000-000000000012', 'Warehouse Inbound Update', 'WAREHOUSE_INBOUND_UPDATE', 'Update warehouse inbound receipts', 'WAREHOUSE_INBOUND', 'UPDATE'),
('b1b2c3d4-5555-4000-8000-000000000013', 'Warehouse Inbound Approve Level 1', 'WAREHOUSE_INBOUND_APPROVE_LEVEL_1', 'Approve inbound receipt at accountant level', 'WAREHOUSE_INBOUND', 'APPROVE_LEVEL_1'),
('b1b2c3d4-5555-4000-8000-000000000014', 'Warehouse Inbound Approve Level 2', 'WAREHOUSE_INBOUND_APPROVE_LEVEL_2', 'Approve inbound receipt at head accountant level', 'WAREHOUSE_INBOUND', 'APPROVE_LEVEL_2'),
('b1b2c3d4-5555-4000-8000-000000000015', 'Warehouse Inbound Approve Final', 'WAREHOUSE_INBOUND_APPROVE_FINAL', 'Final approval for inbound receipts', 'WAREHOUSE_INBOUND', 'APPROVE_FINAL'),
-- Warehouse outbound
('b1b2c3d4-5555-4000-8000-000000000020', 'Warehouse Outbound Read', 'WAREHOUSE_OUTBOUND_READ', 'View warehouse outbound documents', 'WAREHOUSE_OUTBOUND', 'READ'),
('b1b2c3d4-5555-4000-8000-000000000021', 'Warehouse Outbound Create', 'WAREHOUSE_OUTBOUND_CREATE', 'Create warehouse outbound documents', 'WAREHOUSE_OUTBOUND', 'CREATE'),
('b1b2c3d4-5555-4000-8000-000000000022', 'Warehouse Outbound Update', 'WAREHOUSE_OUTBOUND_UPDATE', 'Update warehouse outbound documents', 'WAREHOUSE_OUTBOUND', 'UPDATE'),
('b1b2c3d4-5555-4000-8000-000000000023', 'Warehouse Outbound Approve Level 1', 'WAREHOUSE_OUTBOUND_APPROVE_LEVEL_1', 'Approve outbound at accountant level', 'WAREHOUSE_OUTBOUND', 'APPROVE_LEVEL_1'),
('b1b2c3d4-5555-4000-8000-000000000024', 'Warehouse Outbound Approve Level 2', 'WAREHOUSE_OUTBOUND_APPROVE_LEVEL_2', 'Approve outbound at head accountant level', 'WAREHOUSE_OUTBOUND', 'APPROVE_LEVEL_2'),
('b1b2c3d4-5555-4000-8000-000000000025', 'Warehouse Outbound Approve Final', 'WAREHOUSE_OUTBOUND_APPROVE_FINAL', 'Final approval for outbound documents', 'WAREHOUSE_OUTBOUND', 'APPROVE_FINAL'),
-- Warehouse inventory
('b1b2c3d4-5555-4000-8000-000000000030', 'Warehouse Inventory Read', 'WAREHOUSE_INVENTORY_READ', 'View warehouse inventory balances and transactions', 'WAREHOUSE_INVENTORY', 'READ')
ON CONFLICT (code) DO NOTHING;

-- ADMIN: all new service permissions
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permissions p
WHERE r.code = 'ADMIN'
  AND p.code IN (
    'PAYMENT_APPROVE_LEVEL_1', 'PAYMENT_APPROVE_LEVEL_2', 'PAYMENT_APPROVE_FINAL', 'PAYMENT_UPLOAD_BANK_NOTE',
    'WAREHOUSE_INBOUND_READ', 'WAREHOUSE_INBOUND_CREATE', 'WAREHOUSE_INBOUND_UPDATE',
    'WAREHOUSE_INBOUND_APPROVE_LEVEL_1', 'WAREHOUSE_INBOUND_APPROVE_LEVEL_2', 'WAREHOUSE_INBOUND_APPROVE_FINAL',
    'WAREHOUSE_OUTBOUND_READ', 'WAREHOUSE_OUTBOUND_CREATE', 'WAREHOUSE_OUTBOUND_UPDATE',
    'WAREHOUSE_OUTBOUND_APPROVE_LEVEL_1', 'WAREHOUSE_OUTBOUND_APPROVE_LEVEL_2', 'WAREHOUSE_OUTBOUND_APPROVE_FINAL',
    'WAREHOUSE_INVENTORY_READ'
  )
ON CONFLICT DO NOTHING;

-- ACCOUNTANT
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permissions p
WHERE r.code = 'ACCOUNTANT'
  AND p.code IN (
    'PAYMENT_APPROVE_LEVEL_1', 'PAYMENT_UPLOAD_BANK_NOTE',
    'WAREHOUSE_INBOUND_APPROVE_LEVEL_1', 'WAREHOUSE_OUTBOUND_APPROVE_LEVEL_1'
  )
ON CONFLICT DO NOTHING;

-- ACCOUNTANT_MANAGER
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permissions p
WHERE r.code = 'ACCOUNTANT_MANAGER'
  AND p.code IN (
    'PAYMENT_APPROVE_LEVEL_2', 'PAYMENT_APPROVE_FINAL', 'PAYMENT_UPLOAD_BANK_NOTE',
    'WAREHOUSE_INBOUND_APPROVE_LEVEL_2', 'WAREHOUSE_INBOUND_APPROVE_FINAL',
    'WAREHOUSE_OUTBOUND_APPROVE_LEVEL_2', 'WAREHOUSE_OUTBOUND_APPROVE_FINAL'
  )
ON CONFLICT DO NOTHING;

-- WAREHOUSE_KEEPER
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permissions p
WHERE r.code = 'WAREHOUSE_KEEPER'
  AND p.code IN (
    'WAREHOUSE_INBOUND_READ', 'WAREHOUSE_INBOUND_CREATE', 'WAREHOUSE_INBOUND_UPDATE',
    'WAREHOUSE_OUTBOUND_READ', 'WAREHOUSE_OUTBOUND_CREATE', 'WAREHOUSE_OUTBOUND_UPDATE',
    'WAREHOUSE_INVENTORY_READ'
  )
ON CONFLICT DO NOTHING;
