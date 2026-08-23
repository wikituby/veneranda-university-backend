ALTER TABLE course_categories
    ADD COLUMN IF NOT EXISTS cover_image_url TEXT;

COMMENT ON COLUMN course_categories.cover_image_url IS
    'Optional programme card / hero image URL (or data URL). When null, the UI uses a default theme.';
