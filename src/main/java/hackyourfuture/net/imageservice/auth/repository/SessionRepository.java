package hackyourfuture.net.imageservice.auth.repository;

import java.time.Instant;
import java.util.Optional;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

// Reads and writes the sessions table, keyed by the opaque session id.
@Repository
public class SessionRepository {

    private final JdbcClient jdbc;

    public SessionRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public void insert(String sessionId, int userId, Instant expiresAt) {
        jdbc.sql("INSERT INTO sessions (session_id, user_id, expires_at) VALUES (?, ?, ?)")
                .params(sessionId, userId, java.sql.Timestamp.from(expiresAt))
                .update();
    }

    // Returns the owning user id if a matching, non-expired session exists.
    public Optional<Integer> findUserId(String sessionId, Instant now) {
        return jdbc.sql("SELECT user_id FROM sessions WHERE session_id = ? AND expires_at > ?")
                .params(sessionId, java.sql.Timestamp.from(now))
                .query(Integer.class)
                .optional();
    }

    public void deleteById(String sessionId) {
        jdbc.sql("DELETE FROM sessions WHERE session_id = ?")
                .param(sessionId)
                .update();
    }
}
