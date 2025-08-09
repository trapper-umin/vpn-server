--liquibase formatted sql

--changeset vpn-management:create-vpn-servers-table
CREATE TABLE vpn_servers (
    server_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    country VARCHAR(100) NOT NULL,
    country_code VARCHAR(2) NOT NULL,
    city VARCHAR(100) NOT NULL,
    ip_address VARCHAR(45) NOT NULL,
    port INTEGER NOT NULL,
    max_connections INTEGER NOT NULL,
    current_connections INTEGER NOT NULL DEFAULT 0,
    bandwidth VARCHAR(50) NOT NULL,
    speed VARCHAR(50) NOT NULL,
    ping INTEGER NOT NULL DEFAULT 0,
    uptime DECIMAL(5,2) NOT NULL DEFAULT 100.0,
    is_online BOOLEAN NOT NULL DEFAULT FALSE,
    is_active BOOLEAN NOT NULL DEFAULT FALSE,
    total_subscribers INTEGER NOT NULL DEFAULT 0,
    active_subscribers INTEGER NOT NULL DEFAULT 0,
    total_revenue DECIMAL(12,2) NOT NULL DEFAULT 0,
    monthly_revenue DECIMAL(10,2) NOT NULL DEFAULT 0,
    status VARCHAR(20) NOT NULL DEFAULT 'SETUP' CHECK (status IN ('SETUP', 'ACTIVE', 'INACTIVE', 'MAINTENANCE')),
    description TEXT,
    seller_id UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

--rollback DROP TABLE vpn_servers;

--changeset vpn-management:create-vpn-servers-indexes
CREATE INDEX idx_vpn_servers_seller_id ON vpn_servers(seller_id);
CREATE INDEX idx_vpn_servers_is_active ON vpn_servers(is_active);
CREATE INDEX idx_vpn_servers_created_at ON vpn_servers(created_at);

--rollback DROP INDEX idx_vpn_servers_seller_id; DROP INDEX idx_vpn_servers_is_active; DROP INDEX idx_vpn_servers_created_at;