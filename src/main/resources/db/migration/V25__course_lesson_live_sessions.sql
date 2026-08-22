-- ============================================================
-- V25: Live online lesson sessions (per outline category)
-- ============================================================

ALTER TABLE course_lesson_contents
    ADD COLUMN IF NOT EXISTS live_sessions_draft_json TEXT;

CREATE TABLE course_lesson_live_sessions (
    id                  BIGSERIAL PRIMARY KEY,
    uuid                UUID            NOT NULL,
    tenant_id           BIGINT          NOT NULL,
    category_id         BIGINT          NOT NULL,
    title               VARCHAR(255)    NOT NULL,
    url                 TEXT            NOT NULL,
    provider            VARCHAR(50)     NOT NULL DEFAULT 'other',
    scheduled_at        TIMESTAMPTZ,
    duration_minutes    INTEGER,
    notes               TEXT,
    order_index         INTEGER         NOT NULL DEFAULT 0,
    status              VARCHAR(20)     NOT NULL DEFAULT 'ACTIVE',
    version             BIGINT          NOT NULL DEFAULT 0,
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ     NOT NULL DEFAULT now(),
    created_by          BIGINT,
    updated_by          BIGINT,
    CONSTRAINT uq_course_lesson_live_sessions_uuid UNIQUE (uuid),
    CONSTRAINT chk_course_lesson_live_sessions_status CHECK (status IN ('ACTIVE','SUSPENDED','TERMINATED')),
    CONSTRAINT fk_course_lesson_live_sessions_tenant
        FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE RESTRICT,
    CONSTRAINT fk_course_lesson_live_sessions_category
        FOREIGN KEY (category_id) REFERENCES course_categories(id) ON DELETE CASCADE
);

CREATE INDEX idx_course_lesson_live_sessions_category ON course_lesson_live_sessions(category_id);
CREATE INDEX idx_course_lesson_live_sessions_tenant ON course_lesson_live_sessions(tenant_id);

COMMENT ON TABLE course_lesson_live_sessions IS 'Scheduled live online lesson links (Zoom, Meet, Teams, etc.)';
