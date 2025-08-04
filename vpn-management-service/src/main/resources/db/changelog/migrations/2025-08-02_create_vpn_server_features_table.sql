--liquibase formatted sql

--changeset vpn-management:create-vpn-server-features-table
CREATE TABLE vpn_server_features (
    server_id UUID NOT NULL,
    feature VARCHAR(255) NOT NULL,
    CONSTRAINT fk_vpn_server_features_server FOREIGN KEY (server_id) REFERENCES vpn_servers(server_id) ON DELETE CASCADE
);

--rollback DROP TABLE vpn_server_features;

--changeset vpn-management:create-vpn-server-features-indexes
CREATE INDEX idx_vpn_server_features_server_id ON vpn_server_features(server_id);

--rollback DROP INDEX idx_vpn_server_features_server_id;