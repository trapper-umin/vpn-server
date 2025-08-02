---------------------------------------------
-- changeset trapper:001
-- comment create users
---------------------------------------------
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

CREATE TABLE IF NOT EXISTS users
(
    user_id       UUID PRIMARY KEY                  DEFAULT gen_random_uuid(),
    email         VARCHAR(255)             NOT NULL,
    role          VARCHAR(20)              NOT NULL DEFAULT 'USER',
    password      VARCHAR(255)             NOT NULL,
    created_at    TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at    TIMESTAMP WITH TIME ZONE NULL,
    is_active     BOOLEAN                  NOT NULL DEFAULT TRUE,
    token_version BIGINT                   NOT NULL DEFAULT 0
);


CREATE INDEX idx_users_role ON users (role);

ALTER TABLE users
    ADD CONSTRAINT chk_users_role
        CHECK (role IN ('USER', 'SELLER', 'ADMIN'));

---------------------------------------------
-- rollback drop table if exists users;
---------------------------------------------