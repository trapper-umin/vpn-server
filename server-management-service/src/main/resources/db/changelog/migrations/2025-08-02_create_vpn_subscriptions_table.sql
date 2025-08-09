--liquibase formatted sql

--changeset vpn-management:create-vpn-subscriptions-table
CREATE TABLE vpn_subscriptions (
    subscription_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_email VARCHAR(255) NOT NULL,
    user_id UUID NOT NULL,
    plan_id UUID NOT NULL,
    billing_cycle VARCHAR(20) NOT NULL CHECK (billing_cycle IN ('MONTHLY', 'YEARLY')),
    start_date TIMESTAMPTZ NOT NULL,
    end_date TIMESTAMPTZ NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    total_paid DECIMAL(10,2) NOT NULL,
    last_login TIMESTAMPTZ,
    country VARCHAR(100),
    country_code VARCHAR(2),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW(),
    CONSTRAINT fk_vpn_subscriptions_plan FOREIGN KEY (plan_id) REFERENCES subscription_plans(plan_id) ON DELETE CASCADE
);

--rollback DROP TABLE vpn_subscriptions;

--changeset vpn-management:create-vpn-subscriptions-indexes
CREATE INDEX idx_vpn_subscriptions_plan_id ON vpn_subscriptions(plan_id);
CREATE INDEX idx_vpn_subscriptions_user_id ON vpn_subscriptions(user_id);
CREATE INDEX idx_vpn_subscriptions_is_active ON vpn_subscriptions(is_active);
CREATE INDEX idx_vpn_subscriptions_created_at ON vpn_subscriptions(created_at);

--rollback DROP INDEX idx_vpn_subscriptions_plan_id; DROP INDEX idx_vpn_subscriptions_user_id; DROP INDEX idx_vpn_subscriptions_is_active; DROP INDEX idx_vpn_subscriptions_created_at;