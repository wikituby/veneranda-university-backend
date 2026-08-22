-- ============================================================
-- V8: Course categories (hierarchical outline) + permissions
-- ============================================================

CREATE TABLE course_categories (
    id              BIGSERIAL PRIMARY KEY,
    uuid            UUID            NOT NULL,
    tenant_id       BIGINT          NOT NULL,
    parent_id       BIGINT,
    title           VARCHAR(255)    NOT NULL,
    order_index     INTEGER         NOT NULL DEFAULT 0,
    icon            VARCHAR(100),
    description     TEXT,
    content_id      VARCHAR(100),
    content_path    VARCHAR(500),
    is_published    BOOLEAN         NOT NULL DEFAULT TRUE,
    -- audit columns
    status          VARCHAR(20)     NOT NULL DEFAULT 'ACTIVE',
    version         BIGINT          NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT now(),
    created_by      BIGINT,
    updated_by      BIGINT,
    CONSTRAINT uq_course_categories_uuid UNIQUE (uuid),
    CONSTRAINT chk_course_categories_status CHECK (status IN ('ACTIVE','SUSPENDED','TERMINATED')),
    CONSTRAINT fk_course_categories_tenant
        FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE RESTRICT,
    CONSTRAINT fk_course_categories_parent
        FOREIGN KEY (parent_id) REFERENCES course_categories(id) ON DELETE CASCADE
);

CREATE INDEX idx_course_categories_tenant_id ON course_categories(tenant_id);
CREATE INDEX idx_course_categories_parent_id ON course_categories(parent_id);
CREATE INDEX idx_course_categories_order ON course_categories(tenant_id, parent_id, order_index);
CREATE INDEX idx_course_categories_published ON course_categories(tenant_id, is_published);
CREATE INDEX idx_course_categories_status ON course_categories(status);

COMMENT ON TABLE course_categories IS 'Hierarchical course outline categories (adjacency list)';

-- ===== Permissions =====
INSERT INTO permissions (name, code, module, description, is_system)
SELECT 'View courses', 'course:read', 'course', 'View course categories and content', TRUE
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE code = 'course:read');

INSERT INTO permissions (name, code, module, description, is_system)
SELECT 'Manage courses', 'course:manage', 'course', 'Create and manage course outline', TRUE
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE code = 'course:manage');

-- SUPER_ADMIN gets every permission (including newly added)
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permissions p
WHERE r.code = 'SUPER_ADMIN'
AND NOT EXISTS (
    SELECT 1 FROM role_permissions rp
    WHERE rp.role_id = r.id AND rp.permission_id = p.id
);

-- ADMIN gets course read + manage
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permissions p
WHERE r.code = 'ADMIN'
AND p.code IN ('course:read', 'course:manage')
AND NOT EXISTS (
    SELECT 1 FROM role_permissions rp
    WHERE rp.role_id = r.id AND rp.permission_id = p.id
);

-- ===== Seed default outline for each tenant (demo starting point) =====
INSERT INTO course_categories (uuid, tenant_id, parent_id, title, order_index, icon, description, is_published, status)
SELECT gen_random_uuid(), t.id, NULL, 'Orientation', 1, 'school', 'Getting started with the course.', TRUE, 'ACTIVE'
FROM tenants t
WHERE NOT EXISTS (
    SELECT 1 FROM course_categories c WHERE c.tenant_id = t.id AND c.title = 'Orientation' AND c.parent_id IS NULL
);

INSERT INTO course_categories (uuid, tenant_id, parent_id, title, order_index, icon, description, is_published, status)
SELECT gen_random_uuid(), t.id, NULL, 'Module 1: Foundations', 2, 'foundation', 'Core concepts and terminology.', TRUE, 'ACTIVE'
FROM tenants t
WHERE NOT EXISTS (
    SELECT 1 FROM course_categories c WHERE c.tenant_id = t.id AND c.title = 'Module 1: Foundations' AND c.parent_id IS NULL
);

INSERT INTO course_categories (uuid, tenant_id, parent_id, title, order_index, icon, description, is_published, status)
SELECT gen_random_uuid(), t.id, NULL, 'Module 2: Advanced Topics', 3, 'auto_stories', 'Deeper dive into advanced material.', TRUE, 'ACTIVE'
FROM tenants t
WHERE NOT EXISTS (
    SELECT 1 FROM course_categories c WHERE c.tenant_id = t.id AND c.title = 'Module 2: Advanced Topics' AND c.parent_id IS NULL
);
