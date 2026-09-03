-- Enable real Flutterwave checkout (keys still come from settings / env)

UPDATE settings
SET value = 'true',
    description = 'When true, Subscribe and pay uses real Flutterwave hosted checkout (not simulated)'
WHERE tenant_id IS NULL AND category = 'payment' AND key = 'flutterwave_enabled';

UPDATE settings
SET value = 'https://educ.venerandahospital.org',
    description = 'Frontend base URL used for Flutterwave redirect_url after payment'
WHERE tenant_id IS NULL AND category = 'payment' AND key = 'frontend_base_url'
  AND (value IS NULL OR value = '' OR value LIKE 'http://localhost%');

INSERT INTO settings (tenant_id, category, key, value, value_type, description, is_public, is_encrypted)
SELECT NULL, 'payment', 'flutterwave_enabled', 'true', 'BOOLEAN',
       'When true, Subscribe and pay uses real Flutterwave hosted checkout (not simulated)', FALSE, FALSE
WHERE NOT EXISTS (
    SELECT 1 FROM settings WHERE tenant_id IS NULL AND category = 'payment' AND key = 'flutterwave_enabled'
);

INSERT INTO settings (tenant_id, category, key, value, value_type, description, is_public, is_encrypted)
SELECT NULL, 'payment', 'frontend_base_url', 'https://educ.venerandahospital.org', 'STRING',
       'Frontend base URL used for Flutterwave redirect_url after payment', FALSE, FALSE
WHERE NOT EXISTS (
    SELECT 1 FROM settings WHERE tenant_id IS NULL AND category = 'payment' AND key = 'frontend_base_url'
);
