-- Flutterwave payment refs on subscriptions + payment settings (UGX default)

ALTER TABLE course_subscriptions
    ADD COLUMN IF NOT EXISTS payment_tx_ref VARCHAR(100);

ALTER TABLE course_subscriptions
    ADD COLUMN IF NOT EXISTS payment_provider_ref VARCHAR(100);

CREATE UNIQUE INDEX IF NOT EXISTS uq_course_subscriptions_payment_tx_ref
    ON course_subscriptions (payment_tx_ref)
    WHERE payment_tx_ref IS NOT NULL;

COMMENT ON COLUMN course_subscriptions.payment_tx_ref IS 'Merchant transaction reference sent to Flutterwave';
COMMENT ON COLUMN course_subscriptions.payment_provider_ref IS 'Flutterwave transaction id after successful charge';

-- Currency charged via Flutterwave (always UGX unless changed in admin settings)
INSERT INTO settings (tenant_id, category, key, value, value_type, description, is_public, is_encrypted)
SELECT NULL, 'payment', 'payment_currency', 'UGX', 'STRING',
       'Currency sent to Flutterwave for LMS subscriptions (default UGX)', TRUE, FALSE
WHERE NOT EXISTS (
    SELECT 1 FROM settings WHERE tenant_id IS NULL AND category = 'payment' AND key = 'payment_currency'
);

INSERT INTO settings (tenant_id, category, key, value, value_type, description, is_public, is_encrypted)
SELECT NULL, 'payment', 'frontend_base_url', 'http://localhost:4900', 'STRING',
       'Frontend base URL used for Flutterwave redirect_url after payment', FALSE, FALSE
WHERE NOT EXISTS (
    SELECT 1 FROM settings WHERE tenant_id IS NULL AND category = 'payment' AND key = 'frontend_base_url'
);

INSERT INTO settings (tenant_id, category, key, value, value_type, description, is_public, is_encrypted)
SELECT NULL, 'payment', 'flutterwave_public_key', '', 'STRING',
       'Flutterwave public key (pk_...)', FALSE, FALSE
WHERE NOT EXISTS (
    SELECT 1 FROM settings WHERE tenant_id IS NULL AND category = 'payment' AND key = 'flutterwave_public_key'
);

INSERT INTO settings (tenant_id, category, key, value, value_type, description, is_public, is_encrypted)
SELECT NULL, 'payment', 'flutterwave_secret_key', '', 'STRING',
       'Flutterwave secret key (sk_...) — keep private', FALSE, TRUE
WHERE NOT EXISTS (
    SELECT 1 FROM settings WHERE tenant_id IS NULL AND category = 'payment' AND key = 'flutterwave_secret_key'
);

INSERT INTO settings (tenant_id, category, key, value, value_type, description, is_public, is_encrypted)
SELECT NULL, 'payment', 'flutterwave_webhook_hash', '', 'STRING',
       'Flutterwave webhook secret hash (verif-hash header)', FALSE, TRUE
WHERE NOT EXISTS (
    SELECT 1 FROM settings WHERE tenant_id IS NULL AND category = 'payment' AND key = 'flutterwave_webhook_hash'
);

UPDATE settings
SET description = 'When true, Subscribe and pay uses Flutterwave hosted checkout (one payment for card + MoMo)'
WHERE tenant_id IS NULL AND category = 'payment' AND key = 'flutterwave_enabled';
