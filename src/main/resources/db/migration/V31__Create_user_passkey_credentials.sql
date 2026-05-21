CREATE TABLE user_passkey_credentials (
    id uuid PRIMARY KEY,
    user_id uuid NOT NULL REFERENCES users(id),
    credential_id BYTEA NOT NULL,
    public_key_cose BYTEA NOT NULL,
    signature_count BIGINT NOT NULL DEFAULT 0,
    user_handle BYTEA NOT NULL,
    device_name VARCHAR(255),
    transports VARCHAR(255),
    is_discoverable BOOLEAN NOT NULL DEFAULT false,
    is_backup_eligible BOOLEAN NOT NULL DEFAULT false,
    is_backed_up BOOLEAN NOT NULL DEFAULT false,
    is_active BOOLEAN NOT NULL DEFAULT true,
    last_used_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    is_deleted BOOLEAN NOT NULL DEFAULT false,
    CONSTRAINT uq_user_passkey_credential_id UNIQUE (credential_id)
);

CREATE INDEX idx_user_passkey_credentials_user_id ON user_passkey_credentials(user_id);
CREATE INDEX idx_user_passkey_credentials_user_handle ON user_passkey_credentials(user_handle);
CREATE INDEX idx_user_passkey_credentials_active ON user_passkey_credentials(is_active) WHERE is_deleted = false;
