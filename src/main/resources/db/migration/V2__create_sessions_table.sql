-- Login sessions: a random id, the user, and when it expires.
CREATE TABLE sessions (
    session_id TEXT        PRIMARY KEY,
    user_id    INTEGER     NOT NULL REFERENCES users (user_id) ON DELETE CASCADE,
    expires_at TIMESTAMPTZ NOT NULL
);
