-- ============================================================
-- V16: Remove demo "Module 2: Advanced Topics" outline section
-- ============================================================

DELETE FROM course_categories
WHERE title = 'Module 2: Advanced Topics'
  AND parent_id IS NULL;
