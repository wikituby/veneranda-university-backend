-- ============================================================
-- V14: Replace ISP / hotspot RBAC with online-school roles
-- ============================================================

-- ------------------------------------------------------------
-- 1) School-oriented permissions (idempotent)
-- ------------------------------------------------------------

INSERT INTO permissions (name, code, module, description, is_system)
SELECT 'View courses', 'course:read', 'course', 'View course outline, lessons, and media', TRUE
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE code = 'course:read');

INSERT INTO permissions (name, code, module, description, is_system)
SELECT 'Manage courses', 'course:manage', 'course', 'Create and manage courses, lessons, and media', TRUE
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE code = 'course:manage');

INSERT INTO permissions (name, code, module, description, is_system)
SELECT 'View enrollments', 'enrollment:read', 'course', 'View course enrollments', TRUE
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE code = 'enrollment:read');

INSERT INTO permissions (name, code, module, description, is_system)
SELECT 'Manage enrollments', 'enrollment:manage', 'course', 'Enroll or remove students from courses', TRUE
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE code = 'enrollment:manage');

-- Ensure core admin permissions exist (from V5; re-assert for safety)
INSERT INTO permissions (name, code, module, description, is_system)
SELECT v.name, v.code, v.module, v.description, TRUE
FROM (
    VALUES
        ('View users', 'user:read', 'admin', 'View school users'),
        ('Create user', 'user:create', 'admin', 'Create school users'),
        ('Update user', 'user:update', 'admin', 'Update school users'),
        ('Delete user', 'user:delete', 'admin', 'Delete school users'),
        ('View roles', 'role:read', 'admin', 'View roles'),
        ('Manage roles', 'role:manage', 'admin', 'Manage roles and permissions'),
        ('View audit logs', 'audit:read', 'admin', 'View audit logs'),
        ('Manage settings', 'setting:manage', 'admin', 'Manage school settings')
) AS v(name, code, module, description)
WHERE NOT EXISTS (SELECT 1 FROM permissions p WHERE p.code = v.code);

-- ------------------------------------------------------------
-- 2) School roles
-- ------------------------------------------------------------

UPDATE roles
SET name = 'Super Administrator',
    description = 'Full access to the online school platform'
WHERE code = 'SUPER_ADMIN';

UPDATE roles
SET name = 'School Administrator',
    description = 'Manage courses, users, and day-to-day school operations'
WHERE code = 'ADMIN';

UPDATE roles
SET name = 'Student',
    description = 'Learner — view and enroll in courses'
WHERE code = 'STUDENT';

INSERT INTO roles (tenant_id, name, code, description, is_system, status)
SELECT t.id,
       'Instructor',
       'INSTRUCTOR',
       'Create and manage course content and lessons',
       TRUE,
       'ACTIVE'
FROM tenants t
WHERE t.code = 'DEFAULT'
  AND NOT EXISTS (SELECT 1 FROM roles WHERE code = 'INSTRUCTOR');

-- ------------------------------------------------------------
-- 3) Detach users from obsolete ISP roles, then remove those roles
-- ------------------------------------------------------------

DELETE FROM user_roles
WHERE role_id IN (
    SELECT id FROM roles WHERE code IN ('FINANCE', 'AGENT', 'TECHNICIAN', 'SUPPORT')
);

DELETE FROM role_permissions
WHERE role_id IN (
    SELECT id FROM roles WHERE code IN ('FINANCE', 'AGENT', 'TECHNICIAN', 'SUPPORT')
);

DELETE FROM roles
WHERE code IN ('FINANCE', 'AGENT', 'TECHNICIAN', 'SUPPORT');

-- ------------------------------------------------------------
-- 4) Remove ISP / hotspot permissions (and their grants)
-- ------------------------------------------------------------

DELETE FROM role_permissions
WHERE permission_id IN (
    SELECT id
    FROM permissions
    WHERE module IN (
        'customer', 'package', 'billing', 'payment', 'voucher',
        'hotspot', 'pppoe', 'router', 'monitoring', 'inventory',
        'finance', 'report', 'crm', 'technician', 'sales', 'gis',
        'notification'
    )
    OR code IN ('branch:read', 'branch:manage')
);

DELETE FROM permissions
WHERE module IN (
    'customer', 'package', 'billing', 'payment', 'voucher',
    'hotspot', 'pppoe', 'router', 'monitoring', 'inventory',
    'finance', 'report', 'crm', 'technician', 'sales', 'gis',
    'notification'
)
OR code IN ('branch:read', 'branch:manage');

-- ------------------------------------------------------------
-- 5) Rebuild role → permission grants for school roles
-- ------------------------------------------------------------

-- Clear existing grants for school roles (we re-seed cleanly)
DELETE FROM role_permissions
WHERE role_id IN (
    SELECT id FROM roles WHERE code IN ('SUPER_ADMIN', 'ADMIN', 'INSTRUCTOR', 'STUDENT')
);

-- SUPER_ADMIN: every remaining permission
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permissions p
WHERE r.code = 'SUPER_ADMIN';

-- ADMIN: school operations (not full role/permission redesign)
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permissions p
WHERE r.code = 'ADMIN'
  AND p.code IN (
      'course:read',
      'course:manage',
      'enrollment:read',
      'enrollment:manage',
      'user:read',
      'user:create',
      'user:update',
      'role:read',
      'audit:read',
      'setting:manage'
  );

-- INSTRUCTOR: course authoring
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permissions p
WHERE r.code = 'INSTRUCTOR'
  AND p.code IN (
      'course:read',
      'course:manage',
      'enrollment:read'
  );

-- STUDENT: learn
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permissions p
WHERE r.code = 'STUDENT'
  AND p.code IN (
      'course:read'
  );
