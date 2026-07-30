CREATE TABLE user_account (
    user_id UUID PRIMARY KEY,
    email VARCHAR(320) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    account_status VARCHAR(20) NOT NULL CHECK (account_status IN ('ACTIVE')),
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_user_account_email UNIQUE (email),
    CONSTRAINT chk_user_account_email_normalized CHECK (
        email = lower(trim(email))
        AND length(email) > 0
    ),
    CONSTRAINT chk_user_account_password_hash CHECK (
        length(trim(password_hash)) > 0
    )
);
