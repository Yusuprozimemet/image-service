-- Application users. Passwords are stored only as a bcrypt hash, never in plain text.
CREATE TABLE users (
    user_id       SERIAL      PRIMARY KEY,
    email         TEXT        NOT NULL UNIQUE,
    password_hash TEXT        NOT NULL,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);
