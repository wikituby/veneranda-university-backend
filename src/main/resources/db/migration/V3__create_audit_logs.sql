-- ============================================================
-- V3: Audit Logs
-- Immutable audit trail for system mutations and auth events
-- ============================================================


CREATE TABLE audit_logs (

    id BIGSERIAL PRIMARY KEY,


    uuid UUID NOT NULL DEFAULT gen_random_uuid(),



    -- BaseEntity fields

    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',

    version BIGINT NOT NULL DEFAULT 0,

    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),

    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),

    created_by BIGINT,

    updated_by BIGINT,



    -- Tenant / User context

    tenant_id BIGINT,

    user_id BIGINT,

    username VARCHAR(50),



    -- Audit event details

    module VARCHAR(50) NOT NULL,

    action VARCHAR(50) NOT NULL,


    entity_type VARCHAR(50),

    entity_id BIGINT,

    entity_uuid UUID,


    description TEXT,


    old_values JSONB,

    new_values JSONB,


    ip_address VARCHAR(45),

    user_agent VARCHAR(500),


    request_path VARCHAR(255),

    http_method VARCHAR(10),


    status_code INT,

    duration_ms BIGINT,



    CONSTRAINT uq_audit_logs_uuid

    UNIQUE(uuid),



    CONSTRAINT chk_audit_logs_status

    CHECK(status IN ('ACTIVE','SUSPENDED','TERMINATED')),



    CONSTRAINT fk_audit_logs_tenant

    FOREIGN KEY(tenant_id)

    REFERENCES tenants(id)

    ON DELETE SET NULL,



    CONSTRAINT fk_audit_logs_user

    FOREIGN KEY(user_id)

    REFERENCES users(id)

    ON DELETE SET NULL

);



CREATE INDEX idx_audit_logs_tenant_id

ON audit_logs(tenant_id);



CREATE INDEX idx_audit_logs_user_id

ON audit_logs(user_id);



CREATE INDEX idx_audit_logs_module

ON audit_logs(module);



CREATE INDEX idx_audit_logs_action

ON audit_logs(action);



CREATE INDEX idx_audit_logs_entity

ON audit_logs(entity_type,entity_id);



CREATE INDEX idx_audit_logs_created_at

ON audit_logs(created_at);



COMMENT ON TABLE audit_logs IS

'Immutable audit trail of all system mutations and authentication events';