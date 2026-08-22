-- ============================================================
-- V1: Tenants and Branches (Multi-tenant / White-label foundation)
-- ============================================================

-- ===== Tenants (SaaS tenant isolation) =====
CREATE TABLE tenants (
    id              BIGSERIAL PRIMARY KEY,
    uuid            UUID         NOT NULL,
    code            VARCHAR(50)  NOT NULL,
    name            VARCHAR(150) NOT NULL,
    description     TEXT,
    contact_email   VARCHAR(150),
    contact_phone   VARCHAR(50),
    address         TEXT,
    logo_url        VARCHAR(500),
    primary_color   VARCHAR(20)  DEFAULT '#1976d2',
    is_active       BOOLEAN      NOT NULL DEFAULT TRUE,
    plan            VARCHAR(30)  NOT NULL DEFAULT 'TRIAL',  -- TRIAL, STARTER, BUSINESS, ENTERPRISE
    trial_ends_at   TIMESTAMPTZ,
    -- audit columns
    status          VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',  -- ACTIVE, SUSPENDED, TERMINATED
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    created_by      BIGINT,
    updated_by      BIGINT,
    CONSTRAINT uq_tenants_code UNIQUE (code),
    CONSTRAINT uq_tenants_uuid UNIQUE (uuid),
    CONSTRAINT chk_tenants_status CHECK (status IN ('ACTIVE','SUSPENDED','TERMINATED')),
    CONSTRAINT chk_tenants_plan CHECK (plan IN ('TRIAL','STARTER','BUSINESS','ENTERPRISE'))
);

CREATE INDEX idx_tenants_status ON tenants(status);
CREATE INDEX idx_tenants_is_active ON tenants(is_active);

COMMENT ON TABLE tenants IS 'SaaS tenants for multi-tenant isolation and white-labeling';

-- ===== Branches (physical locations / offices per tenant) =====
CREATE TABLE branches (
    id              BIGSERIAL PRIMARY KEY,
    uuid            UUID         NOT NULL,
    tenant_id       BIGINT       NOT NULL,
    code            VARCHAR(50)  NOT NULL,
    name            VARCHAR(150) NOT NULL,
    description     TEXT,
    contact_email   VARCHAR(150),
    contact_phone   VARCHAR(50),
    address         TEXT,
    district        VARCHAR(100),
    latitude        NUMERIC(10,7),
    longitude       NUMERIC(10,7),
    manager_id      BIGINT,      -- FK to users (added in V2)
    is_head_office  BOOLEAN      NOT NULL DEFAULT FALSE,
    is_active       BOOLEAN      NOT NULL DEFAULT TRUE,
    -- audit columns
    status          VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    created_by      BIGINT,
    updated_by      BIGINT,
    CONSTRAINT uq_branches_tenant_code UNIQUE (tenant_id, code),
    CONSTRAINT uq_branches_uuid UNIQUE (uuid),
    CONSTRAINT chk_branches_status CHECK (status IN ('ACTIVE','SUSPENDED','TERMINATED')),
    CONSTRAINT fk_branches_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE RESTRICT
);

CREATE INDEX idx_branches_tenant_id ON branches(tenant_id);
CREATE INDEX idx_branches_status ON branches(status);

COMMENT ON TABLE branches IS 'Physical branches/offices belonging to a tenant';

-- ===== Default tenant + branch seed =====
INSERT INTO tenants (code, name, description, plan, is_active, status)
VALUES ('DEFAULT', 'Default ISP Tenant', 'System default tenant', 'ENTERPRISE', TRUE, 'ACTIVE');

INSERT INTO branches (tenant_id, code, name, is_head_office, is_active, status)
SELECT id, 'HQ', 'Head Office', TRUE, TRUE, 'ACTIVE'
FROM tenants WHERE code = 'DEFAULT';