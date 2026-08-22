-- Google identity fields on users
ALTER TABLE users
    ADD COLUMN IF NOT EXISTS google_sub VARCHAR(255);

ALTER TABLE users
    ADD COLUMN IF NOT EXISTS auth_provider VARCHAR(20) NOT NULL DEFAULT 'local';

ALTER TABLE users
    ALTER COLUMN password_hash DROP NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS ux_users_google_sub
    ON users (google_sub)
    WHERE google_sub IS NOT NULL;

COMMENT ON COLUMN users.google_sub IS 'Stable Google account subject (sub claim)';
COMMENT ON COLUMN users.auth_provider IS 'local | google | both';

-- Student role for Google sign-ups (course viewing)
INSERT INTO roles (tenant_id, name, code, description, is_system, status)
SELECT t.id, 'Student', 'STUDENT', 'Course learner with content access', TRUE, 'ACTIVE'
FROM tenants t
WHERE t.code = 'DEFAULT'
  AND NOT EXISTS (SELECT 1 FROM roles WHERE code = 'STUDENT');

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permissions p
WHERE r.code = 'STUDENT'
  AND p.code = 'course:read'
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );
