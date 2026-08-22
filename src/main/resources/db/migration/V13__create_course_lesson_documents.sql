-- ============================================================
-- V13: Lesson documents (PDF, Word, etc.) via R2 or external URL
-- ============================================================

CREATE TABLE IF NOT EXISTS course_lesson_documents (
    id              BIGSERIAL PRIMARY KEY,
    uuid            UUID            NOT NULL DEFAULT gen_random_uuid(),
    tenant_id       BIGINT          NOT NULL,
    category_id     BIGINT          NOT NULL,
    title           VARCHAR(255)    NOT NULL,
    url             TEXT            NOT NULL,
    provider        VARCHAR(50)     NOT NULL DEFAULT 'r2',
    file_format     VARCHAR(20)     NOT NULL DEFAULT 'other',
    order_index     INTEGER         NOT NULL DEFAULT 0,
    status          VARCHAR(20)     NOT NULL DEFAULT 'ACTIVE',
    version         BIGINT          NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT now(),
    created_by      BIGINT,
    updated_by      BIGINT,
    CONSTRAINT uq_course_lesson_documents_uuid UNIQUE (uuid),
    CONSTRAINT chk_course_lesson_documents_status CHECK (status IN ('ACTIVE','SUSPENDED','TERMINATED')),
    CONSTRAINT fk_course_lesson_documents_tenant
        FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE RESTRICT,
    CONSTRAINT fk_course_lesson_documents_category
        FOREIGN KEY (category_id) REFERENCES course_categories(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_course_lesson_documents_category ON course_lesson_documents(category_id);
CREATE INDEX IF NOT EXISTS idx_course_lesson_documents_tenant ON course_lesson_documents(tenant_id);

COMMENT ON TABLE course_lesson_documents IS 'Lesson handouts: PDF, Word, and other documents (often R2 private)';
