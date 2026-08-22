-- Unpublished lesson notes: staff edit notes_draft_body; students see notes_body until Publish.

ALTER TABLE course_lesson_contents
    ADD COLUMN IF NOT EXISTS notes_draft_body TEXT;

UPDATE course_lesson_contents
SET notes_draft_body = notes_body
WHERE notes_draft_body IS NULL;

COMMENT ON COLUMN course_lesson_contents.notes_body IS
    'Published lesson notes shown to students';
COMMENT ON COLUMN course_lesson_contents.notes_draft_body IS
    'Staff draft notes; copied to notes_body on publish';
