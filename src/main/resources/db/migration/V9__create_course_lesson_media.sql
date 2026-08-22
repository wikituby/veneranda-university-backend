-- ============================================================
-- V9: Lesson notes, slides, and videos (per outline category)
-- ============================================================

CREATE TABLE course_lesson_contents (
    id              BIGSERIAL PRIMARY KEY,
    uuid            UUID            NOT NULL,
    tenant_id       BIGINT          NOT NULL,
    category_id     BIGINT          NOT NULL,
    notes_body      TEXT,
    status          VARCHAR(20)     NOT NULL DEFAULT 'ACTIVE',
    version         BIGINT          NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT now(),
    created_by      BIGINT,
    updated_by      BIGINT,
    CONSTRAINT uq_course_lesson_contents_uuid UNIQUE (uuid),
    CONSTRAINT uq_course_lesson_contents_category UNIQUE (category_id),
    CONSTRAINT chk_course_lesson_contents_status CHECK (status IN ('ACTIVE','SUSPENDED','TERMINATED')),
    CONSTRAINT fk_course_lesson_contents_tenant
        FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE RESTRICT,
    CONSTRAINT fk_course_lesson_contents_category
        FOREIGN KEY (category_id) REFERENCES course_categories(id) ON DELETE CASCADE
);

CREATE TABLE course_lesson_slides (
    id              BIGSERIAL PRIMARY KEY,
    uuid            UUID            NOT NULL,
    tenant_id       BIGINT          NOT NULL,
    category_id     BIGINT          NOT NULL,
    title           VARCHAR(255)    NOT NULL,
    url             TEXT            NOT NULL,
    provider        VARCHAR(50)     NOT NULL DEFAULT 'google-slides',
    order_index     INTEGER         NOT NULL DEFAULT 0,
    status          VARCHAR(20)     NOT NULL DEFAULT 'ACTIVE',
    version         BIGINT          NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT now(),
    created_by      BIGINT,
    updated_by      BIGINT,
    CONSTRAINT uq_course_lesson_slides_uuid UNIQUE (uuid),
    CONSTRAINT chk_course_lesson_slides_status CHECK (status IN ('ACTIVE','SUSPENDED','TERMINATED')),
    CONSTRAINT fk_course_lesson_slides_tenant
        FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE RESTRICT,
    CONSTRAINT fk_course_lesson_slides_category
        FOREIGN KEY (category_id) REFERENCES course_categories(id) ON DELETE CASCADE
);

CREATE TABLE course_lesson_videos (
    id              BIGSERIAL PRIMARY KEY,
    uuid            UUID            NOT NULL,
    tenant_id       BIGINT          NOT NULL,
    category_id     BIGINT          NOT NULL,
    title           VARCHAR(255)    NOT NULL,
    url             TEXT            NOT NULL,
    provider        VARCHAR(50)     NOT NULL DEFAULT 'youtube',
    order_index     INTEGER         NOT NULL DEFAULT 0,
    status          VARCHAR(20)     NOT NULL DEFAULT 'ACTIVE',
    version         BIGINT          NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT now(),
    created_by      BIGINT,
    updated_by      BIGINT,
    CONSTRAINT uq_course_lesson_videos_uuid UNIQUE (uuid),
    CONSTRAINT chk_course_lesson_videos_status CHECK (status IN ('ACTIVE','SUSPENDED','TERMINATED')),
    CONSTRAINT fk_course_lesson_videos_tenant
        FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE RESTRICT,
    CONSTRAINT fk_course_lesson_videos_category
        FOREIGN KEY (category_id) REFERENCES course_categories(id) ON DELETE CASCADE
);

CREATE INDEX idx_course_lesson_contents_tenant ON course_lesson_contents(tenant_id);
CREATE INDEX idx_course_lesson_slides_category ON course_lesson_slides(category_id);
CREATE INDEX idx_course_lesson_slides_tenant ON course_lesson_slides(tenant_id);
CREATE INDEX idx_course_lesson_videos_category ON course_lesson_videos(category_id);
CREATE INDEX idx_course_lesson_videos_tenant ON course_lesson_videos(tenant_id);

COMMENT ON TABLE course_lesson_contents IS 'Notes/main body for a leaf lesson category';
COMMENT ON TABLE course_lesson_slides IS 'Google Slides/Drive presentation links for a lesson';
COMMENT ON TABLE course_lesson_videos IS 'Video links (YouTube, MP4) for a lesson';
