-- ============================================================
-- V7: Router Management (Network devices)
-- ============================================================

-- ===== Routers =====
CREATE TABLE routers (
    id              BIGSERIAL PRIMARY KEY,
    uuid            UUID            NOT NULL,
    tenant_id       BIGINT          NOT NULL,
    branch_id       BIGINT,
    name            VARCHAR(150)    NOT NULL,
    vendor          VARCHAR(50)     NOT NULL,   -- MIKROTIK, UBIQUITI, TP_LINK, D_LINK, CISCO, HUAWEI, GENERIC
    model           VARCHAR(100),
    ip_address      VARCHAR(45)     NOT NULL,   -- supports IPv6
    api_port        INTEGER         NOT NULL DEFAULT 8728,
    username        VARCHAR(100)    NOT NULL,
    password_encrypted TEXT         NOT NULL,   -- encrypted credential
    location        VARCHAR(255),
    firmware        VARCHAR(100),
    router_version  VARCHAR(50),
    serial_number   VARCHAR(100),
    is_enabled      BOOLEAN         NOT NULL DEFAULT TRUE,
    is_online       BOOLEAN         NOT NULL DEFAULT FALSE,
    last_sync_at    TIMESTAMPTZ,
    last_seen_at    TIMESTAMPTZ,
    notes           TEXT,
    -- audit columns
    status          VARCHAR(20)     NOT NULL DEFAULT 'ACTIVE',  -- ACTIVE, OFFLINE, MAINTENANCE, RETIRED
    version         BIGINT          NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT now(),
    created_by      BIGINT,
    updated_by      BIGINT,
    CONSTRAINT uq_routers_uuid UNIQUE (uuid),
    CONSTRAINT uq_routers_tenant_ip UNIQUE (tenant_id, ip_address),
    CONSTRAINT chk_routers_status CHECK (status IN ('ACTIVE','OFFLINE','MAINTENANCE','RETIRED')),
    CONSTRAINT chk_routers_vendor CHECK (vendor IN ('MIKROTIK','UBIQUITI','TP_LINK','D_LINK','CISCO','HUAWEI','GENERIC')),
    CONSTRAINT fk_routers_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE RESTRICT,
    CONSTRAINT fk_routers_branch FOREIGN KEY (branch_id) REFERENCES branches(id) ON DELETE SET NULL
);

CREATE INDEX idx_routers_tenant_id ON routers(tenant_id);
CREATE INDEX idx_routers_branch_id ON routers(branch_id);
CREATE INDEX idx_routers_status ON routers(status);
CREATE INDEX idx_routers_vendor ON routers(vendor);
CREATE INDEX idx_routers_is_enabled ON routers(is_enabled);
CREATE INDEX idx_routers_is_online ON routers(is_online);

COMMENT ON TABLE routers IS 'Network routers and devices (MikroTik, Ubiquiti, Cisco, etc.)';