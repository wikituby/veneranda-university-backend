-- ============================================================
-- V2: RBAC
-- Users, Roles, Permissions, Join tables and Refresh Tokens
-- ============================================================

CREATE EXTENSION IF NOT EXISTS pgcrypto;



-- ============================================================
-- USERS
-- ============================================================

CREATE TABLE users (

    id BIGSERIAL PRIMARY KEY,

    uuid UUID NOT NULL DEFAULT gen_random_uuid(),


    tenant_id BIGINT NOT NULL,

    branch_id BIGINT,


    username VARCHAR(50) NOT NULL,

    password_hash VARCHAR(100) NOT NULL,


    first_name VARCHAR(100) NOT NULL,

    last_name VARCHAR(100) NOT NULL,


    email VARCHAR(150) NOT NULL,

    phone VARCHAR(50),


    avatar_url VARCHAR(500),

    job_title VARCHAR(100),

    employee_number VARCHAR(50),


    is_active BOOLEAN NOT NULL DEFAULT TRUE,

    is_locked BOOLEAN NOT NULL DEFAULT FALSE,

    is_system BOOLEAN NOT NULL DEFAULT FALSE,


    failed_login_count INT NOT NULL DEFAULT 0,


    last_login_at TIMESTAMPTZ,

    last_login_ip VARCHAR(45),


    password_changed_at TIMESTAMPTZ,

    password_expires_at TIMESTAMPTZ,


    two_factor_enabled BOOLEAN NOT NULL DEFAULT FALSE,

    two_factor_secret VARCHAR(100),



    -- BaseEntity

    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',

    version BIGINT NOT NULL DEFAULT 0,

    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),

    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),

    created_by BIGINT,

    updated_by BIGINT,



    CONSTRAINT uq_users_uuid UNIQUE(uuid),

    CONSTRAINT uq_users_tenant_username 
    UNIQUE(tenant_id,username),

    CONSTRAINT uq_users_email UNIQUE(email),


    CONSTRAINT chk_users_status
    CHECK(status IN ('ACTIVE','SUSPENDED','TERMINATED','PENDING')),



    CONSTRAINT fk_users_tenant

    FOREIGN KEY(tenant_id)

    REFERENCES tenants(id)

    ON DELETE RESTRICT,



    CONSTRAINT fk_users_branch

    FOREIGN KEY(branch_id)

    REFERENCES branches(id)

    ON DELETE SET NULL

);



CREATE INDEX idx_users_tenant
ON users(tenant_id);


CREATE INDEX idx_users_branch
ON users(branch_id);


CREATE INDEX idx_users_status
ON users(status);



-- Branch manager relationship

ALTER TABLE branches

ADD CONSTRAINT fk_branches_manager

FOREIGN KEY(manager_id)

REFERENCES users(id)

ON DELETE SET NULL;




-- ============================================================
-- ROLES
-- ============================================================


CREATE TABLE roles (

    id BIGSERIAL PRIMARY KEY,

    uuid UUID NOT NULL DEFAULT gen_random_uuid(),


    tenant_id BIGINT NOT NULL,


    name VARCHAR(50) NOT NULL,

    code VARCHAR(50) NOT NULL,


    description TEXT,


    is_system BOOLEAN NOT NULL DEFAULT FALSE,

    is_active BOOLEAN NOT NULL DEFAULT TRUE,



    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',

    version BIGINT NOT NULL DEFAULT 0,

    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),

    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),

    created_by BIGINT,

    updated_by BIGINT,



    CONSTRAINT uq_roles_uuid UNIQUE(uuid),

    CONSTRAINT uq_roles_tenant_code
    UNIQUE(tenant_id,code),


    CONSTRAINT chk_roles_status
    CHECK(status IN ('ACTIVE','SUSPENDED','TERMINATED')),



    CONSTRAINT fk_roles_tenant

    FOREIGN KEY(tenant_id)

    REFERENCES tenants(id)

    ON DELETE RESTRICT

);



CREATE INDEX idx_roles_tenant
ON roles(tenant_id);



-- ============================================================
-- PERMISSIONS
-- ============================================================


CREATE TABLE permissions (

    id BIGSERIAL PRIMARY KEY,


    uuid UUID NOT NULL DEFAULT gen_random_uuid(),


    name VARCHAR(100) NOT NULL,

    code VARCHAR(100) NOT NULL UNIQUE,


    module VARCHAR(50) NOT NULL,


    description TEXT,


    is_system BOOLEAN NOT NULL DEFAULT TRUE,



    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',

    version BIGINT NOT NULL DEFAULT 0,

    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),

    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),

    created_by BIGINT,

    updated_by BIGINT,



    CONSTRAINT uq_permissions_uuid UNIQUE(uuid),


    CONSTRAINT chk_permissions_status

    CHECK(status IN ('ACTIVE','SUSPENDED','TERMINATED'))

);



CREATE INDEX idx_permissions_module
ON permissions(module);



-- ============================================================
-- ROLE PERMISSIONS
-- ============================================================


CREATE TABLE role_permissions (

    id BIGSERIAL PRIMARY KEY,


    role_id BIGINT NOT NULL,


    permission_id BIGINT NOT NULL,


    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),

    created_by BIGINT,



    CONSTRAINT uq_role_permissions

    UNIQUE(role_id,permission_id),



    CONSTRAINT fk_role_permission_role

    FOREIGN KEY(role_id)

    REFERENCES roles(id)

    ON DELETE CASCADE,



    CONSTRAINT fk_role_permission_permission

    FOREIGN KEY(permission_id)

    REFERENCES permissions(id)

    ON DELETE CASCADE

);



CREATE INDEX idx_role_permissions_role

ON role_permissions(role_id);



CREATE INDEX idx_role_permissions_permission

ON role_permissions(permission_id);




-- ============================================================
-- USER ROLES
-- ============================================================


CREATE TABLE user_roles (

    id BIGSERIAL PRIMARY KEY,


    user_id BIGINT NOT NULL,


    role_id BIGINT NOT NULL,


    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),

    created_by BIGINT,



    CONSTRAINT uq_user_roles

    UNIQUE(user_id,role_id),



    CONSTRAINT fk_user_roles_user

    FOREIGN KEY(user_id)

    REFERENCES users(id)

    ON DELETE CASCADE,



    CONSTRAINT fk_user_roles_role

    FOREIGN KEY(role_id)

    REFERENCES roles(id)

    ON DELETE CASCADE

);



CREATE INDEX idx_user_roles_user

ON user_roles(user_id);



CREATE INDEX idx_user_roles_role

ON user_roles(role_id);




-- ============================================================
-- REFRESH TOKENS
-- ============================================================


CREATE TABLE refresh_tokens (

    id BIGSERIAL PRIMARY KEY,


    uuid UUID NOT NULL DEFAULT gen_random_uuid(),


    user_id BIGINT NOT NULL,


    token_hash VARCHAR(100) NOT NULL,


    device_info VARCHAR(255),


    ip_address VARCHAR(45),


    expires_at TIMESTAMPTZ NOT NULL,


    revoked_at TIMESTAMPTZ,


    is_revoked BOOLEAN NOT NULL DEFAULT FALSE,



    -- BaseEntity

    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',

    version BIGINT NOT NULL DEFAULT 0,

    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),

    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),

    created_by BIGINT,

    updated_by BIGINT,



    CONSTRAINT uq_refresh_token_uuid UNIQUE(uuid),

    CONSTRAINT uq_refresh_token_hash UNIQUE(token_hash),



    CONSTRAINT fk_refresh_token_user

    FOREIGN KEY(user_id)

    REFERENCES users(id)

    ON DELETE CASCADE

);



CREATE INDEX idx_refresh_token_user

ON refresh_tokens(user_id);


CREATE INDEX idx_refresh_token_expiry

ON refresh_tokens(expires_at);


CREATE INDEX idx_refresh_token_revoked

ON refresh_tokens(is_revoked);