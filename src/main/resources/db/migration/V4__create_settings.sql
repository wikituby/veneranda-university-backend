-- ============================================================
-- V4: System Settings
-- Tenant-scoped and global key/value configuration
-- ============================================================


CREATE TABLE settings (

    id BIGSERIAL PRIMARY KEY,


    uuid UUID NOT NULL DEFAULT gen_random_uuid(),


    tenant_id BIGINT,


    category VARCHAR(50) NOT NULL,


    key VARCHAR(100) NOT NULL,


    value TEXT,


    value_type VARCHAR(20) NOT NULL DEFAULT 'STRING',


    description TEXT,


    is_public BOOLEAN NOT NULL DEFAULT FALSE,


    is_encrypted BOOLEAN NOT NULL DEFAULT FALSE,



    -- BaseEntity fields

    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',

    version BIGINT NOT NULL DEFAULT 0,

    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),

    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),

    created_by BIGINT,

    updated_by BIGINT,



    CONSTRAINT uq_settings_uuid

    UNIQUE(uuid),



    CONSTRAINT uq_settings_tenant_category_key

    UNIQUE(tenant_id, category, key),



    CONSTRAINT chk_settings_status

    CHECK(status IN ('ACTIVE','SUSPENDED','TERMINATED')),



    CONSTRAINT chk_settings_value_type

    CHECK(value_type IN ('STRING','NUMBER','BOOLEAN','JSON')),



    CONSTRAINT fk_settings_tenant

    FOREIGN KEY(tenant_id)

    REFERENCES tenants(id)

    ON DELETE CASCADE

);



CREATE INDEX idx_settings_tenant

ON settings(tenant_id);



CREATE INDEX idx_settings_category

ON settings(category);



CREATE INDEX idx_settings_key

ON settings(key);



COMMENT ON TABLE settings IS

'System configuration key/value store (tenant-scoped or global)';





-- ============================================================
-- Default Global Settings
-- ============================================================


INSERT INTO settings
(
    tenant_id,
    category,
    key,
    value,
    value_type,
    description,
    is_public,
    is_encrypted
)

VALUES


(NULL,
'general',
'platform_name',
'ISP Automation',
'STRING',
'Platform display name',
TRUE,
FALSE),


(NULL,
'general',
'support_email',
'support@ispautomation.com',
'STRING',
'Support email',
TRUE,
FALSE),


(NULL,
'general',
'support_phone',
'+256700000000',
'STRING',
'Support phone',
TRUE,
FALSE),


(NULL,
'general',
'default_currency',
'UGX',
'STRING',
'Default currency code',
TRUE,
FALSE),


(NULL,
'general',
'default_timezone',
'Africa/Nairobi',
'STRING',
'Default timezone',
TRUE,
FALSE),


(NULL,
'general',
'default_language',
'en',
'STRING',
'Default language code',
TRUE,
FALSE),


(NULL,
'billing',
'invoice_prefix',
'INV',
'STRING',
'Invoice number prefix',
FALSE,
FALSE),


(NULL,
'billing',
'invoice_due_days',
'7',
'NUMBER',
'Default invoice due days',
FALSE,
FALSE),


(NULL,
'billing',
'late_fee_percentage',
'5',
'NUMBER',
'Late fee percentage',
FALSE,
FALSE),


(NULL,
'billing',
'auto_suspend_enabled',
'true',
'BOOLEAN',
'Auto suspend on expiry',
FALSE,
FALSE),


(NULL,
'billing',
'suspend_grace_days',
'2',
'NUMBER',
'Grace days before suspension',
FALSE,
FALSE),


(NULL,
'auth',
'password_min_length',
'8',
'NUMBER',
'Minimum password length',
FALSE,
FALSE),


(NULL,
'auth',
'session_timeout',
'30',
'NUMBER',
'Session timeout minutes',
FALSE,
FALSE),


(NULL,
'sms',
'provider',
'none',
'STRING',
'SMS gateway provider',
FALSE,
FALSE),


(NULL,
'email',
'provider',
'smtp',
'STRING',
'Email provider',
FALSE,
FALSE),


(NULL,
'payment',
'mtn_momo_enabled',
'false',
'BOOLEAN',
'MTN MoMo enabled',
FALSE,
FALSE),


(NULL,
'payment',
'airtel_money_enabled',
'false',
'BOOLEAN',
'Airtel Money enabled',
FALSE,
FALSE),


(NULL,
'payment',
'flutterwave_enabled',
'false',
'BOOLEAN',
'Flutterwave enabled',
FALSE,
FALSE),


(NULL,
'payment',
'pesapal_enabled',
'false',
'BOOLEAN',
'Pesapal enabled',
FALSE,
FALSE);