ALTER TABLE course_subscriptions
    ADD COLUMN IF NOT EXISTS expires_at TIMESTAMPTZ;
