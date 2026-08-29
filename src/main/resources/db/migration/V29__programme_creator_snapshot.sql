-- Persist programme creator display name/avatar on the category row.
ALTER TABLE course_categories
    ADD COLUMN IF NOT EXISTS created_by_name VARCHAR(200);

ALTER TABLE course_categories
    ADD COLUMN IF NOT EXISTS created_by_avatar_url VARCHAR(500);

COMMENT ON COLUMN course_categories.created_by_name IS 'Snapshot of creator full name at create/update time';
COMMENT ON COLUMN course_categories.created_by_avatar_url IS 'Snapshot of creator avatar URL at create/update time';

-- Backfill from users for rows that already have created_by.
UPDATE course_categories c
SET
    created_by_name = NULLIF(TRIM(CONCAT(COALESCE(u.first_name, ''), ' ', COALESCE(u.last_name, ''))), ''),
    created_by_avatar_url = u.avatar_url
FROM users u
WHERE c.created_by = u.id
  AND (c.created_by_name IS NULL OR c.created_by_name = '');
