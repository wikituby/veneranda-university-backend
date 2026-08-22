CREATE TABLE IF NOT EXISTS course_enrollments (
    id              BIGSERIAL PRIMARY KEY,
    uuid            UUID            NOT NULL DEFAULT gen_random_uuid(),
    tenant_id       BIGINT          NOT NULL REFERENCES tenants(id),
    user_id         BIGINT          NOT NULL REFERENCES users(id),
    category_id     BIGINT          NOT NULL REFERENCES course_categories(id) ON DELETE CASCADE,
    enrollment_status VARCHAR(20)   NOT NULL DEFAULT 'ACTIVE',
    group_sync_status VARCHAR(20)   NOT NULL DEFAULT 'PENDING',
    group_sync_error  TEXT,
    group_synced_at   TIMESTAMPTZ,
    enrolled_at     TIMESTAMPTZ     NOT NULL DEFAULT now(),
    unenrolled_at   TIMESTAMPTZ,
    status          VARCHAR(20)     NOT NULL DEFAULT 'ACTIVE',
    version         BIGINT          NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT now(),
    created_by      BIGINT,
    updated_by      BIGINT,
    CONSTRAINT uq_course_enrollments_uuid UNIQUE (uuid),
    CONSTRAINT uq_course_enrollments_user_category UNIQUE (user_id, category_id),
    CONSTRAINT chk_course_enrollments_enrollment_status
        CHECK (enrollment_status IN ('ACTIVE', 'DROPPED')),
    CONSTRAINT chk_course_enrollments_group_sync_status
        CHECK (group_sync_status IN ('PENDING', 'SYNCED', 'FAILED', 'SKIPPED', 'REMOVED')),
    CONSTRAINT chk_course_enrollments_status
        CHECK (status IN ('ACTIVE', 'SUSPENDED', 'TERMINATED'))
);

CREATE INDEX IF NOT EXISTS idx_course_enrollments_user
    ON course_enrollments (user_id);

CREATE INDEX IF NOT EXISTS idx_course_enrollments_category
    ON course_enrollments (category_id);

CREATE INDEX IF NOT EXISTS idx_course_enrollments_tenant
    ON course_enrollments (tenant_id);

COMMENT ON TABLE course_enrollments IS
'Student enrollments for a course category (typically a root section with google_group_email)';
