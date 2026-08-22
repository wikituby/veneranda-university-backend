-- Draft lesson media: staff working copy stays unpublished until Publish.

ALTER TABLE course_lesson_contents
    ADD COLUMN IF NOT EXISTS slides_draft_json TEXT;

ALTER TABLE course_lesson_contents
    ADD COLUMN IF NOT EXISTS videos_draft_json TEXT;

ALTER TABLE course_lesson_contents
    ADD COLUMN IF NOT EXISTS documents_draft_json TEXT;

COMMENT ON COLUMN course_lesson_contents.slides_draft_json IS
    'Staff draft slides JSON; copied to course_lesson_slides on publish';
COMMENT ON COLUMN course_lesson_contents.videos_draft_json IS
    'Staff draft videos JSON; copied to course_lesson_videos on publish';
COMMENT ON COLUMN course_lesson_contents.documents_draft_json IS
    'Staff draft documents JSON; copied to course_lesson_documents on publish';
