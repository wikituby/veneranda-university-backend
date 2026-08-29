-- Give seeded programmes without a creator a real owner snapshot (system admin).
UPDATE course_categories c
SET
    created_by = 1,
    created_by_name = COALESCE(
        NULLIF(TRIM(c.created_by_name), ''),
        (SELECT NULLIF(TRIM(CONCAT(COALESCE(u.first_name, ''), ' ', COALESCE(u.last_name, ''))), '') FROM users u WHERE u.id = 1),
        'System Administrator'
    ),
    created_by_avatar_url = COALESCE(
        c.created_by_avatar_url,
        (SELECT u.avatar_url FROM users u WHERE u.id = 1)
    )
WHERE c.parent_id IS NULL
  AND UPPER(COALESCE(c.node_kind, 'PROGRAMME')) = 'PROGRAMME'
  AND c.created_by IS NULL
  AND c.status = 'ACTIVE';
