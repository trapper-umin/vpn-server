--liquibase formatted sql

--changeset vpn-management:create-subscription-plan-features-table
CREATE TABLE subscription_plan_features (
    plan_id UUID NOT NULL,
    feature VARCHAR(255) NOT NULL,
    CONSTRAINT fk_subscription_plan_features_plan FOREIGN KEY (plan_id) REFERENCES subscription_plans(plan_id) ON DELETE CASCADE
);

--rollback DROP TABLE subscription_plan_features;

--changeset vpn-management:create-subscription-plan-features-indexes
CREATE INDEX idx_subscription_plan_features_plan_id ON subscription_plan_features(plan_id);

--rollback DROP INDEX idx_subscription_plan_features_plan_id;