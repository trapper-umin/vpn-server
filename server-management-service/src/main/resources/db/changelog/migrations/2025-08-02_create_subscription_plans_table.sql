--liquibase formatted sql

--changeset vpn-management:create-subscription-plans-table
CREATE TABLE subscription_plans (
    plan_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    server_id UUID NOT NULL,
    type VARCHAR(20) NOT NULL CHECK (type IN ('BASIC', 'PREMIUM', 'ENTERPRISE')),
    monthly_price DECIMAL(10,2) NOT NULL,
    yearly_price DECIMAL(10,2) NOT NULL,
    max_connections INTEGER NOT NULL,
    bandwidth_limit VARCHAR(50),
    speed_limit VARCHAR(50),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    is_popular BOOLEAN NOT NULL DEFAULT FALSE,
    sort_order INTEGER NOT NULL DEFAULT 0,
    description TEXT,
    total_subscribers INTEGER NOT NULL DEFAULT 0,
    active_subscribers INTEGER NOT NULL DEFAULT 0,
    total_revenue DECIMAL(12,2) NOT NULL DEFAULT 0,
    monthly_revenue DECIMAL(10,2) NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW(),
    CONSTRAINT fk_subscription_plans_server FOREIGN KEY (server_id) REFERENCES vpn_servers(server_id) ON DELETE CASCADE
);

--rollback DROP TABLE subscription_plans;

--changeset vpn-management:create-subscription-plans-indexes
CREATE INDEX idx_subscription_plans_server_id ON subscription_plans(server_id);
CREATE INDEX idx_subscription_plans_is_active ON subscription_plans(is_active);
CREATE INDEX idx_subscription_plans_type ON subscription_plans(type);
CREATE INDEX idx_subscription_plans_created_at ON subscription_plans(created_at);

--rollback DROP INDEX idx_subscription_plans_server_id; DROP INDEX idx_subscription_plans_is_active; DROP INDEX idx_subscription_plans_type; DROP INDEX idx_subscription_plans_created_at;