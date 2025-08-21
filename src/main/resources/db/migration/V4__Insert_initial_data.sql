-- Insert initial data
CREATE USER new_cnh_root WITH PASSWORD 'Str0ngP@ssw0rId';
CREATE DATABASE new_db_cnh;
GRANT ALL PRIVILEGES ON DATABASE new_db_cnh TO new_cnh_root;


-- Insert default roles
INSERT INTO roles (name, description) VALUES
('ADMIN', 'System Administrator'),
('MANAGER', 'Manager'),
('USER', 'Regular User'),
('ACCOUNTANT', 'Accountant'),
('WAREHOUSE_KEEPER', 'Warehouse Keeper');

-- Insert default permissions
INSERT INTO permissions (name, description, resource, action) VALUES
-- User management
('USER_CREATE', 'Create users', 'USER', 'CREATE'),
('USER_READ', 'Read users', 'USER', 'READ'),
('USER_UPDATE', 'Update users', 'USER', 'UPDATE'),
('USER_DELETE', 'Delete users', 'USER', 'DELETE'),

-- Product management
('PRODUCT_CREATE', 'Create products', 'PRODUCT', 'CREATE'),
('PRODUCT_READ', 'Read products', 'PRODUCT', 'READ'),
('PRODUCT_UPDATE', 'Update products', 'PRODUCT', 'UPDATE'),
('PRODUCT_DELETE', 'Delete products', 'PRODUCT', 'DELETE'),

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
