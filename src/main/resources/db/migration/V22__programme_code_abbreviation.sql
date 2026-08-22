ALTER TABLE course_categories
    ADD COLUMN IF NOT EXISTS programme_code VARCHAR(50);

ALTER TABLE course_categories
    ADD COLUMN IF NOT EXISTS abbreviation VARCHAR(50);

COMMENT ON COLUMN course_categories.programme_code IS
    'Official catalogue code for a programme, e.g. DCMCH-001';
COMMENT ON COLUMN course_categories.abbreviation IS
    'Short programme abbreviation, e.g. DCMCH';
