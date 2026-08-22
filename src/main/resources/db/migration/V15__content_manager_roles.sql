-- ============================================================
-- V15: Content-manager roles (Teacher, Coordinator, System Admin)
-- Add/edit course content only for these staff roles (+ existing Admin/Instructor)
-- ============================================================

INSERT INTO roles (tenant_id, name, code, description, is_system, status)
SELECT t.id,
       'System Administrator',
       'SYSTEM_ADMIN',
       'Platform system administrator with full school access',
       TRUE,
       'ACTIVE'
FROM tenants t
WHERE t.code = 'DEFAULT'
  AND NOT EXISTS (SELECT 1 FROM roles WHERE code = 'SYSTEM_ADMIN');

INSERT INTO roles (tenant_id, name, code, description, is_system, status)
SELECT t.id,
       'Teacher',
       'TEACHER',
       'Teach and manage assigned course content',
       TRUE,
       'ACTIVE'
FROM tenants t
WHERE t.code = 'DEFAULT'
  AND NOT EXISTS (SELECT 1 FROM roles WHERE code = 'TEACHER');

INSERT INTO roles (tenant_id, name, code, description, is_system, status)
SELECT t.id,
       'Coordinator',
       'COORDINATOR',
       'Coordinate courses and manage outline / lesson content',
       TRUE,
       'ACTIVE'
FROM tenants t
WHERE t.code = 'DEFAULT'
  AND NOT EXISTS (SELECT 1 FROM roles WHERE code = 'COORDINATOR');

-- SYSTEM_ADMIN: all permissions (same breadth as SUPER_ADMIN)
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permissions p
WHERE r.code = 'SYSTEM_ADMIN'
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

-- TEACHER / COORDINATOR: course authoring (+ view enrollments)
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permissions p
WHERE r.code IN ('TEACHER', 'COORDINATOR')
  AND p.code IN (
      'course:read',
      'course:manage',
      'enrollment:read'
  )
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

-- Ensure INSTRUCTOR still has manage (idempotent)
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permissions p
WHERE r.code = 'INSTRUCTOR'
  AND p.code IN (
      'course:read',
      'course:manage',
      'enrollment:read'
  )
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

-- Ensure ADMIN / SUPER_ADMIN retain course:manage
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permissions p
WHERE r.code IN ('SUPER_ADMIN', 'ADMIN')
  AND p.code IN ('course:read', 'course:manage')
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );
