-- Programme cascade (programme > year > semester > unit > outline) + paid access

ALTER TABLE course_categories
    ADD COLUMN IF NOT EXISTS node_kind VARCHAR(20) NOT NULL DEFAULT 'OUTLINE';

ALTER TABLE course_categories
    ADD COLUMN IF NOT EXISTS price_amount NUMERIC(12, 2);

ALTER TABLE course_categories
    ADD COLUMN IF NOT EXISTS currency VARCHAR(3) NOT NULL DEFAULT 'KES';

ALTER TABLE course_categories
    DROP CONSTRAINT IF EXISTS chk_course_categories_node_kind;

ALTER TABLE course_categories
    ADD CONSTRAINT chk_course_categories_node_kind
        CHECK (node_kind IN ('PROGRAMME', 'YEAR', 'SEMESTER', 'UNIT', 'OUTLINE'));

COMMENT ON COLUMN course_categories.node_kind IS
    'PROGRAMME > YEAR > SEMESTER > UNIT > OUTLINE cascade';

WITH RECURSIVE tree AS (
    SELECT id, 0 AS depth
    FROM course_categories
    WHERE parent_id IS NULL
    UNION ALL
    SELECT c.id, t.depth + 1
    FROM course_categories c
    JOIN tree t ON c.parent_id = t.id
)
UPDATE course_categories c
SET node_kind = CASE
    WHEN tree.depth = 0 THEN 'PROGRAMME'
    WHEN tree.depth = 1 THEN 'YEAR'
    WHEN tree.depth = 2 THEN 'SEMESTER'
    WHEN tree.depth = 3 THEN 'UNIT'
    ELSE 'OUTLINE'
END
FROM tree
WHERE tree.id = c.id;

CREATE TABLE IF NOT EXISTS course_subscriptions (
    id              BIGSERIAL PRIMARY KEY,
    uuid            UUID            NOT NULL DEFAULT gen_random_uuid(),
    tenant_id       BIGINT          NOT NULL REFERENCES tenants(id),
    user_id         BIGINT          NOT NULL REFERENCES users(id),
    category_id     BIGINT          NOT NULL REFERENCES course_categories(id) ON DELETE CASCADE,
    payment_status  VARCHAR(20)     NOT NULL DEFAULT 'PENDING',
    amount          NUMERIC(12, 2),
    currency        VARCHAR(3)      NOT NULL DEFAULT 'KES',
    payment_method  VARCHAR(40)     NOT NULL DEFAULT 'SIMULATED',
    paid_at         TIMESTAMPTZ,
    status          VARCHAR(20)     NOT NULL DEFAULT 'ACTIVE',
    version         BIGINT          NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT now(),
    created_by      BIGINT,
    updated_by      BIGINT,
    CONSTRAINT uq_course_subscriptions_uuid UNIQUE (uuid),
    CONSTRAINT uq_course_subscriptions_user_category UNIQUE (user_id, category_id),
    CONSTRAINT chk_course_subscriptions_payment_status
        CHECK (payment_status IN ('PENDING', 'PAID', 'FAILED')),
    CONSTRAINT chk_course_subscriptions_status
        CHECK (status IN ('ACTIVE', 'SUSPENDED', 'TERMINATED'))
);

CREATE INDEX IF NOT EXISTS idx_course_subscriptions_user
    ON course_subscriptions (user_id);

CREATE INDEX IF NOT EXISTS idx_course_subscriptions_category
    ON course_subscriptions (category_id);

-- Diploma in Clinical Medicine and Community Health
INSERT INTO course_categories (uuid, tenant_id, parent_id, title, order_index, icon, description, is_published, status, node_kind, currency)
SELECT gen_random_uuid(), t.id, NULL,
       'Diploma in Clinical Medicine and Community Health', 10, 'health_and_safety',
       'Train as a clinician for hospitals, health centres, and community practice.',
       TRUE, 'ACTIVE', 'PROGRAMME', 'KES'
FROM tenants t
WHERE t.code = 'DEFAULT'
  AND NOT EXISTS (
      SELECT 1 FROM course_categories c
      WHERE c.tenant_id = t.id
        AND c.title = 'Diploma in Clinical Medicine and Community Health'
        AND c.parent_id IS NULL
  );

INSERT INTO course_categories (uuid, tenant_id, parent_id, title, order_index, icon, description, is_published, status, node_kind, currency)
SELECT gen_random_uuid(), p.tenant_id, p.id, 'Year 1', 1, 'calendar_month', 'First year of the diploma.', TRUE, 'ACTIVE', 'YEAR', 'KES'
FROM course_categories p
WHERE p.title = 'Diploma in Clinical Medicine and Community Health' AND p.parent_id IS NULL
  AND NOT EXISTS (SELECT 1 FROM course_categories c WHERE c.parent_id = p.id AND c.title = 'Year 1');

INSERT INTO course_categories (uuid, tenant_id, parent_id, title, order_index, icon, description, is_published, status, node_kind, currency)
SELECT gen_random_uuid(), p.tenant_id, p.id, 'Year 2', 2, 'calendar_month', 'Second year of the diploma.', TRUE, 'ACTIVE', 'YEAR', 'KES'
FROM course_categories p
WHERE p.title = 'Diploma in Clinical Medicine and Community Health' AND p.parent_id IS NULL
  AND NOT EXISTS (SELECT 1 FROM course_categories c WHERE c.parent_id = p.id AND c.title = 'Year 2');

INSERT INTO course_categories (uuid, tenant_id, parent_id, title, order_index, icon, description, is_published, status, node_kind, price_amount, currency)
SELECT gen_random_uuid(), y.tenant_id, y.id, 'Semester 1', 1, 'view_week', 'Year 1, first semester.', TRUE, 'ACTIVE', 'SEMESTER', 40000, 'KES'
FROM course_categories y
JOIN course_categories p ON y.parent_id = p.id
WHERE p.title = 'Diploma in Clinical Medicine and Community Health' AND y.title = 'Year 1'
  AND NOT EXISTS (SELECT 1 FROM course_categories c WHERE c.parent_id = y.id AND c.title = 'Semester 1');

INSERT INTO course_categories (uuid, tenant_id, parent_id, title, order_index, icon, description, is_published, status, node_kind, price_amount, currency)
SELECT gen_random_uuid(), y.tenant_id, y.id, 'Semester 2', 2, 'view_week', 'Year 1, second semester.', TRUE, 'ACTIVE', 'SEMESTER', 40000, 'KES'
FROM course_categories y
JOIN course_categories p ON y.parent_id = p.id
WHERE p.title = 'Diploma in Clinical Medicine and Community Health' AND y.title = 'Year 1'
  AND NOT EXISTS (SELECT 1 FROM course_categories c WHERE c.parent_id = y.id AND c.title = 'Semester 2');

INSERT INTO course_categories (uuid, tenant_id, parent_id, title, order_index, icon, description, is_published, status, node_kind, price_amount, currency)
SELECT gen_random_uuid(), y.tenant_id, y.id, 'Semester 1', 1, 'view_week', 'Year 2, first semester.', TRUE, 'ACTIVE', 'SEMESTER', 40000, 'KES'
FROM course_categories y
JOIN course_categories p ON y.parent_id = p.id
WHERE p.title = 'Diploma in Clinical Medicine and Community Health' AND y.title = 'Year 2'
  AND NOT EXISTS (SELECT 1 FROM course_categories c WHERE c.parent_id = y.id AND c.title = 'Semester 1');

INSERT INTO course_categories (uuid, tenant_id, parent_id, title, order_index, icon, description, is_published, status, node_kind, price_amount, currency)
SELECT gen_random_uuid(), s.tenant_id, s.id, v.title, v.ord, 'menu_book', v.descr, TRUE, 'ACTIVE', 'UNIT', 15000, 'KES'
FROM course_categories s
JOIN course_categories y ON s.parent_id = y.id
JOIN course_categories p ON y.parent_id = p.id
JOIN (VALUES
    ('Human Anatomy', 1, 'Structure of the human body for clinical practice.'),
    ('Medical Physiology', 2, 'Function of body systems in health and disease.'),
    ('Community Health I', 3, 'Primary care and population health in the community.')
) AS v(title, ord, descr) ON TRUE
WHERE p.title = 'Diploma in Clinical Medicine and Community Health'
  AND y.title = 'Year 1' AND s.title = 'Semester 1'
  AND NOT EXISTS (SELECT 1 FROM course_categories c WHERE c.parent_id = s.id AND c.title = v.title);

INSERT INTO course_categories (uuid, tenant_id, parent_id, title, order_index, icon, description, is_published, status, node_kind, price_amount, currency)
SELECT gen_random_uuid(), s.tenant_id, s.id, v.title, v.ord, 'menu_book', v.descr, TRUE, 'ACTIVE', 'UNIT', 15000, 'KES'
FROM course_categories s
JOIN course_categories y ON s.parent_id = y.id
JOIN course_categories p ON y.parent_id = p.id
JOIN (VALUES
    ('Microbiology', 1, 'Microbes, infection, and laboratory diagnosis.'),
    ('Pathology', 2, 'Mechanisms of disease and clinical correlation.')
) AS v(title, ord, descr) ON TRUE
WHERE p.title = 'Diploma in Clinical Medicine and Community Health'
  AND y.title = 'Year 1' AND s.title = 'Semester 2'
  AND NOT EXISTS (SELECT 1 FROM course_categories c WHERE c.parent_id = s.id AND c.title = v.title);

INSERT INTO course_categories (uuid, tenant_id, parent_id, title, order_index, icon, description, is_published, status, node_kind, price_amount, currency)
SELECT gen_random_uuid(), s.tenant_id, s.id, v.title, v.ord, 'menu_book', v.descr, TRUE, 'ACTIVE', 'UNIT', 15000, 'KES'
FROM course_categories s
JOIN course_categories y ON s.parent_id = y.id
JOIN course_categories p ON y.parent_id = p.id
JOIN (VALUES
    ('Clinical Medicine I', 1, 'Approach to the medical patient and common presentations.'),
    ('Community Health II', 2, 'Field practice and community diagnosis.')
) AS v(title, ord, descr) ON TRUE
WHERE p.title = 'Diploma in Clinical Medicine and Community Health'
  AND y.title = 'Year 2' AND s.title = 'Semester 1'
  AND NOT EXISTS (SELECT 1 FROM course_categories c WHERE c.parent_id = s.id AND c.title = v.title);

-- Bachelor of Medicine and Surgery (MBChB)
INSERT INTO course_categories (uuid, tenant_id, parent_id, title, order_index, icon, description, is_published, status, node_kind, currency)
SELECT gen_random_uuid(), t.id, NULL,
       'Bachelor of Medicine and Surgery (MBChB)', 11, 'medical_services',
       'Undergraduate medical degree leading to practice as a medical doctor.',
       TRUE, 'ACTIVE', 'PROGRAMME', 'KES'
FROM tenants t
WHERE t.code = 'DEFAULT'
  AND NOT EXISTS (
      SELECT 1 FROM course_categories c
      WHERE c.tenant_id = t.id
        AND c.title = 'Bachelor of Medicine and Surgery (MBChB)'
        AND c.parent_id IS NULL
  );

INSERT INTO course_categories (uuid, tenant_id, parent_id, title, order_index, icon, description, is_published, status, node_kind, currency)
SELECT gen_random_uuid(), p.tenant_id, p.id, 'Year 1', 1, 'calendar_month', 'Pre-clinical year one.', TRUE, 'ACTIVE', 'YEAR', 'KES'
FROM course_categories p
WHERE p.title = 'Bachelor of Medicine and Surgery (MBChB)' AND p.parent_id IS NULL
  AND NOT EXISTS (SELECT 1 FROM course_categories c WHERE c.parent_id = p.id AND c.title = 'Year 1');

INSERT INTO course_categories (uuid, tenant_id, parent_id, title, order_index, icon, description, is_published, status, node_kind, price_amount, currency)
SELECT gen_random_uuid(), y.tenant_id, y.id, 'Semester 1', 1, 'view_week', 'MBChB Year 1 Semester 1.', TRUE, 'ACTIVE', 'SEMESTER', 55000, 'KES'
FROM course_categories y
JOIN course_categories p ON y.parent_id = p.id
WHERE p.title = 'Bachelor of Medicine and Surgery (MBChB)' AND y.title = 'Year 1'
  AND NOT EXISTS (SELECT 1 FROM course_categories c WHERE c.parent_id = y.id AND c.title = 'Semester 1');

INSERT INTO course_categories (uuid, tenant_id, parent_id, title, order_index, icon, description, is_published, status, node_kind, price_amount, currency)
SELECT gen_random_uuid(), y.tenant_id, y.id, 'Semester 2', 2, 'view_week', 'MBChB Year 1 Semester 2.', TRUE, 'ACTIVE', 'SEMESTER', 55000, 'KES'
FROM course_categories y
JOIN course_categories p ON y.parent_id = p.id
WHERE p.title = 'Bachelor of Medicine and Surgery (MBChB)' AND y.title = 'Year 1'
  AND NOT EXISTS (SELECT 1 FROM course_categories c WHERE c.parent_id = y.id AND c.title = 'Semester 2');

INSERT INTO course_categories (uuid, tenant_id, parent_id, title, order_index, icon, description, is_published, status, node_kind, price_amount, currency)
SELECT gen_random_uuid(), s.tenant_id, s.id, v.title, v.ord, 'menu_book', v.descr, TRUE, 'ACTIVE', 'UNIT', 20000, 'KES'
FROM course_categories s
JOIN course_categories y ON s.parent_id = y.id
JOIN course_categories p ON y.parent_id = p.id
JOIN (VALUES
    ('Human Anatomy', 1, 'Gross anatomy and introduction to clinical anatomy.'),
    ('Medical Physiology', 2, 'Cellular and systems physiology.'),
    ('Medical Biochemistry', 3, 'Molecular basis of metabolism and disease.')
) AS v(title, ord, descr) ON TRUE
WHERE p.title = 'Bachelor of Medicine and Surgery (MBChB)'
  AND y.title = 'Year 1' AND s.title = 'Semester 1'
  AND NOT EXISTS (SELECT 1 FROM course_categories c WHERE c.parent_id = s.id AND c.title = v.title);

INSERT INTO course_categories (uuid, tenant_id, parent_id, title, order_index, icon, description, is_published, status, node_kind, price_amount, currency)
SELECT gen_random_uuid(), s.tenant_id, s.id, v.title, v.ord, 'menu_book', v.descr, TRUE, 'ACTIVE', 'UNIT', 20000, 'KES'
FROM course_categories s
JOIN course_categories y ON s.parent_id = y.id
JOIN course_categories p ON y.parent_id = p.id
JOIN (VALUES
    ('Histology', 1, 'Microscopic structure of tissues and organs.'),
    ('Embryology', 2, 'Human development and congenital anomalies.')
) AS v(title, ord, descr) ON TRUE
WHERE p.title = 'Bachelor of Medicine and Surgery (MBChB)'
  AND y.title = 'Year 1' AND s.title = 'Semester 2'
  AND NOT EXISTS (SELECT 1 FROM course_categories c WHERE c.parent_id = s.id AND c.title = v.title);
