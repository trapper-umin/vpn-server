--liquibase formatted sql

--changeset vpn-management:create-vpn-server-protocols-table
CREATE TABLE vpn_server_protocols (
    server_id UUID NOT NULL,
    protocol VARCHAR(255) NOT NULL,
    CONSTRAINT fk_vpn_server_protocols_server FOREIGN KEY (server_id) REFERENCES vpn_servers(server_id) ON DELETE CASCADE
);

--rollback DROP TABLE vpn_server_protocols;

--changeset vpn-management:create-vpn-server-protocols-indexes
CREATE INDEX idx_vpn_server_protocols_server_id ON vpn_server_protocols(server_id);

--rollback DROP INDEX idx_vpn_server_protocols_server_id;