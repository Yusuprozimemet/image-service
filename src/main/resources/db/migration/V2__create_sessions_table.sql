-- Opaque session ids. On login the server generates a random session id, stores
-- it here with the owning user and an expiry, and sends it to the client in an
-- HttpOnly cookie. Every request looks the session up by id and checks expires_at.
CREATE TABLE sessions (
    session_id TEXT        PRIMARY KEY,
    user_id    INTEGER     NOT NULL REFERENCES users (user_id) ON DELETE CASCADE,
    expires_at TIMESTAMPTZ NOT NULL
);
