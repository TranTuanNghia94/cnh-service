-- Insert initial data
CREATE USER new_cnh_root WITH PASSWORD 'Str0ngP@ssw0rId';
CREATE DATABASE new_db_cnh;
GRANT ALL PRIVILEGES ON DATABASE new_db_cnh TO new_cnh_root;

GRANT CONNECT ON DATABASE new_db_cnh TO new_cnh_root;
GRANT USAGE ON SCHEMA public TO new_cnh_root;

GRANT ALL PRIVILEGES ON ALL TABLES    IN SCHEMA public TO new_cnh_root;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO new_cnh_root;
GRANT ALL PRIVILEGES ON ALL FUNCTIONS IN SCHEMA public TO new_cnh_root;


ALTER DEFAULT PRIVILEGES IN SCHEMA public
GRANT ALL PRIVILEGES ON TABLES TO new_cnh_root;

ALTER DEFAULT PRIVILEGES IN SCHEMA public
GRANT ALL PRIVILEGES ON SEQUENCES TO new_cnh_root;

ALTER DEFAULT PRIVILEGES IN SCHEMA public
GRANT ALL PRIVILEGES ON FUNCTIONS TO new_cnh_root;




-- Insert default roles
INSERT INTO roles (id, name, code,description) VALUES
('b7ec72d7-ed04-4068-8ad4-60477b00ce6e','ADMIN','ADMIN', 'System Administrator'),
('90fc7d7c-54f0-4c4d-875c-4f3c57ca8b9c','CS MANAGER','CS_MANAGER', 'Customer Service Manager'),
('a549de97-3a7a-4c1d-84ed-3040149bc743','CS','CS', 'Customer Service'),
('e0affac4-d9ca-42fd-8324-f23b6a5de692',    'ACCOUNT','ACCOUNTANT', 'Accountant'),
('02227161-198a-457d-9561-6bdf06a86239','ACCOUNTANT MANAGER','ACCOUNTANT_MANAGER', 'Accountant'),
('7f30103c-b454-4a7f-80a9-11dec8e86974','WAREHOUSE KEEPER','WAREHOUSE_KEEPER', 'Warehouse Keeper')

-- Insert default permissions
INSERT INTO permissions (id, name, code,description, resource, action) VALUES
-- User management
('6f6480ac-93d2-49b1-90a6-8c1882380f31','User Create','USER_CREATE','Create users', 'USER', 'CREATE'),
('116c0c0d-a3c0-4336-b022-a0768b4b0ffa','User Read','USER_READ', 'Read users', 'USER', 'READ'),
('956dc385-f299-41fe-8d69-89db7038d0ea','User Update','USER_UPDATE', 'Update users', 'USER', 'UPDATE'),
('7f30e82c-d6b8-40d4-9553-a3216d05de9b','User Delete','USER_DELETE', 'Delete users', 'USER', 'DELETE'),

-- Product management
('4f0125c2-9668-4113-8ecb-07b2443d579e','Product Create','PRODUCT_CREATE', 'Create products', 'PRODUCT', 'CREATE'),
('f5e8b789-af0d-4443-92f6-34880b67e3e9','Product Read','PRODUCT_READ', 'Read products', 'PRODUCT', 'READ'),
('7b0e035b-4d5d-49d7-b70f-de0ffffcdd8b','Product Update','PRODUCT_UPDATE', 'Update products', 'PRODUCT', 'UPDATE'),
('19ba7f91-688d-4a35-b823-9910752421d1','Product Delete','PRODUCT_DELETE', 'Delete products', 'PRODUCT', 'DELETE'),

-- Order management
('ORDER_CREATE', 'Create orders', 'ORDER', 'CREATE'),
('ORDER_READ', 'Read orders', 'ORDER', 'READ'),
('ORDER_UPDATE', 'Update orders', 'ORDER', 'UPDATE'),
('ORDER_DELETE', 'Delete orders', 'ORDER', 'DELETE'),

-- Purchase order management
('PO_CREATE', 'Create purchase orders', 'PURCHASE_ORDER', 'CREATE'),
('PO_READ', 'Read purchase orders', 'PURCHASE_ORDER', 'READ'),
('PO_UPDATE', 'Update purchase orders', 'PURCHASE_ORDER', 'UPDATE'),
('PO_DELETE', 'Delete purchase orders', 'PURCHASE_ORDER', 'DELETE'),

-- Payment management
('PAYMENT_CREATE', 'Create payment requests', 'PAYMENT', 'CREATE'),
('PAYMENT_READ', 'Read payment requests', 'PAYMENT', 'READ'),
('PAYMENT_APPROVE', 'Approve payment requests', 'PAYMENT', 'APPROVE'),
('PAYMENT_REJECT', 'Reject payment requests', 'PAYMENT', 'REJECT');

-- Assign permissions to ADMIN role
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r, permissions p
WHERE r.name = 'ADMIN';

-- Assign permissions to MANAGER role
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r, permissions p
WHERE r.name = 'MANAGER' 
AND p.name IN (
    'PRODUCT_READ', 'PRODUCT_UPDATE',
    'ORDER_CREATE', 'ORDER_READ', 'ORDER_UPDATE',
    'PO_CREATE', 'PO_READ', 'PO_UPDATE',
    'PAYMENT_READ', 'PAYMENT_APPROVE', 'PAYMENT_REJECT'
);

-- Assign permissions to USER role
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r, permissions p
WHERE r.name = 'USER' 
AND p.name IN (
    'PRODUCT_READ',
    'ORDER_CREATE', 'ORDER_READ',
    'PO_READ',
    'PAYMENT_CREATE', 'PAYMENT_READ'
);

-- Create default admin user (password: admin123)
INSERT INTO users (username, email, password, first_name, last_name, is_active) VALUES
('admin', 'admin@cnh.com', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewdBPj4J/HS.iK8i', 'System', 'Administrator', true);

-- Assign admin role to admin user
INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u, roles r
WHERE u.username = 'admin' AND r.name = 'ADMIN';

-- Insert default categories
INSERT INTO categories (name, description) VALUES
('Electronics', 'Electronic devices and components'),
('Clothing', 'Clothing and apparel'),
('Food & Beverages', 'Food and beverage products'),
('Books', 'Books and publications'),
('Home & Garden', 'Home and garden products');
