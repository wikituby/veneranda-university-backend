-- ============================================================
-- V6: Seed default super-admin user
-- ============================================================
-- Password is intentionally stored as a placeholder.
-- DataInitializer.java replaces it with BCrypt hash
-- using app.default-admin.password during first application boot.
-- ============================================================


-- ============================================================
-- Create default admin user
-- ============================================================

INSERT INTO users (
    tenant_id,
    branch_id,
    username,
    password_hash,
    first_name,
    last_name,
    email,
    phone,
    is_active,
    is_locked,
    is_system,
    status,
    password_changed_at
)
SELECT
    t.id,

    (
        SELECT b.id
        FROM branches b
        WHERE b.tenant_id = t.id
        AND b.code = 'HQ'
        LIMIT 1
    ),

    'admin',

    'PLACEHOLDER_TO_BE_REPLACED_BY_BCRYPT',

    'System',
    'Administrator',

    'admin@ispautomation.com',

    '+256700000000',

    TRUE,
    FALSE,
    TRUE,

    'ACTIVE',

    now()

FROM tenants t

WHERE t.code = 'DEFAULT'

ON CONFLICT (tenant_id, username)
DO NOTHING;



-- ============================================================
-- Assign SUPER_ADMIN role to admin user
-- ============================================================

INSERT INTO user_roles (
    user_id,
    role_id
)

SELECT
    u.id,
    r.id

FROM users u

JOIN roles r
    ON r.tenant_id = u.tenant_id
    AND r.code = 'SUPER_ADMIN'

WHERE u.username = 'admin'

AND NOT EXISTS (

    SELECT 1

    FROM user_roles ur

    WHERE ur.user_id = u.id

    AND ur.role_id = r.id

);