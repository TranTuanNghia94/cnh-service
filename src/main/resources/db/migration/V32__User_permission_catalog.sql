-- Additional user-management and self-service permissions

INSERT INTO permissions (id, name, code, description, resource, action) VALUES
('a1b2c3d4-4444-4000-8000-000000000001', 'User Restore', 'USER_RESTORE', 'Restore deleted users', 'USER', 'RESTORE'),
('a1b2c3d4-4444-4000-8000-000000000002', 'User Activate', 'USER_ACTIVATE', 'Activate or deactivate users', 'USER', 'ACTIVATE'),
('a1b2c3d4-4444-4000-8000-000000000003', 'User Reset Password', 'USER_RESET_PASSWORD', 'Reset user password', 'USER', 'RESET_PASSWORD'),
('a1b2c3d4-4444-4000-8000-000000000004', 'User Self Read', 'USER_SELF_READ', 'View own profile', 'USER', 'SELF_READ'),
('a1b2c3d4-4444-4000-8000-000000000005', 'User Self Update Profile', 'USER_SELF_UPDATE_PROFILE', 'Update own profile', 'USER', 'SELF_UPDATE'),
('a1b2c3d4-4444-4000-8000-000000000006', 'User Self Change Password', 'USER_SELF_CHANGE_PASSWORD', 'Change own password', 'USER', 'SELF_CHANGE_PASSWORD')
ON CONFLICT (code) DO NOTHING;

-- ADMIN: all user admin permissions (existing USER_* plus new ones)
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permissions p
WHERE r.code = 'ADMIN'
  AND p.code IN (
    'USER_CREATE', 'USER_READ', 'USER_UPDATE', 'USER_DELETE',
    'USER_RESTORE', 'USER_ACTIVATE', 'USER_RESET_PASSWORD'
  )
ON CONFLICT DO NOTHING;

-- All roles: self-service permissions for every authenticated user
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permissions p
WHERE p.code IN (
    'USER_SELF_READ', 'USER_SELF_UPDATE_PROFILE', 'USER_SELF_CHANGE_PASSWORD'
  )
ON CONFLICT DO NOTHING;
