ALTER TABLE "user"
    ADD COLUMN failed_login_attempts INT NOT NULL DEFAULT 0,
    ADD COLUMN login_locked_until    BIGINT,
    ADD COLUMN otp_failed_attempts   INT NOT NULL DEFAULT 0,
    ADD COLUMN otp_locked_until      BIGINT;

CREATE TABLE refresh_token
(
    id         UUID PRIMARY KEY NOT NULL DEFAULT gen_random_uuid(),
    token_hash VARCHAR(64)      NOT NULL UNIQUE,
    user_id    UUID             NOT NULL REFERENCES "user" (id) ON DELETE CASCADE,
    expires_at TIMESTAMP        NOT NULL,
    revoked    BOOLEAN          NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP        NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_refresh_token_user_id ON refresh_token (user_id);
