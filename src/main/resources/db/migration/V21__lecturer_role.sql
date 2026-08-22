-- Lecturer role: same course-authoring access as Instructor / Teacher / Coordinator

INSERT INTO roles (tenant_id, name, code, description, is_system, status)
SELECT t.id,
       'Lecturer',
       'LECTURER',
       'Teach programmes and manage assigned course content',
       TRUE,
       'ACTIVE'
FROM tenants t
WHERE t.code = 'DEFAULT'
  AND NOT EXISTS (SELECT 1 FROM roles WHERE code = 'LECTURER');

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permissions p
WHERE r.code = 'LECTURER'
  AND p.code IN (
      'course:read',
      'course:manage',
      'enrollment:read'
  )
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

-- Keep system / school admins able to manage programmes
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permissions p
WHERE r.code IN ('SUPER_ADMIN', 'SYSTEM_ADMIN', 'ADMIN')
  AND p.code IN ('course:read', 'course:manage')
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );
