ALTER TABLE course_categories
    ADD COLUMN IF NOT EXISTS affiliated_institution VARCHAR(255);

COMMENT ON COLUMN course_categories.affiliated_institution IS
    'Partner or awarding institution the programme is affiliated to';
