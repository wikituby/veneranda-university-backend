-- BaseEntity expects optimistic-lock version column on all tables.
ALTER TABLE tenants ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE branches ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;
