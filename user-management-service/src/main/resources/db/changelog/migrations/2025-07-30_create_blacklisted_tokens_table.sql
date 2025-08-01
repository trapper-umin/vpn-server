---------------------------------------------
-- changeset trapper:003
-- comment create blacklisted_tokens
---------------------------------------------

CREATE TABLE IF NOT EXISTS blacklisted_tokens
(
    blacklisted_token_id BIGSERIAL PRIMARY KEY,
    jti                  VARCHAR(255) NOT NULL UNIQUE,
    user_email           VARCHAR(255) NOT NULL,
    expires_at           TIMESTAMPTZ  NOT NULL,
    created_at           TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Создание индексов для оптимизации запросов
CREATE INDEX idx_token_jti ON blacklisted_tokens (jti);
CREATE INDEX idx_blacklisted_tokens_expires_at ON blacklisted_tokens (expires_at);

-- Комментарии для документации
COMMENT ON TABLE blacklisted_tokens IS 'Таблица для хранения отозванных access токенов (blacklist)';
COMMENT ON COLUMN blacklisted_tokens.jti IS 'JWT ID - уникальный идентификатор токена';
COMMENT ON COLUMN blacklisted_tokens.user_email IS 'Email пользователя, которому принадлежит токен';
COMMENT ON COLUMN blacklisted_tokens.expires_at IS 'Время истечения токена';
COMMENT ON COLUMN blacklisted_tokens.created_at IS 'Время добавления в blacklist';

---------------------------------------------
-- rollback drop table if exists blacklisted_tokens;
---------------------------------------------