ALTER TABLE course_subscriptions
    DROP CONSTRAINT IF EXISTS chk_course_subscriptions_payment_status;

ALTER TABLE course_subscriptions
    ADD CONSTRAINT chk_course_subscriptions_payment_status
        CHECK (payment_status IN ('PENDING', 'PAID', 'FAILED', 'CANCELLED'));
