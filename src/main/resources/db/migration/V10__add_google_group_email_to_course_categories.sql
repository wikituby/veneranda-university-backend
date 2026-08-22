ALTER TABLE course_categories
    ADD COLUMN IF NOT EXISTS google_group_email VARCHAR(255);

COMMENT ON COLUMN course_categories.google_group_email IS
'Google Group email used for private YouTube course access sync';
