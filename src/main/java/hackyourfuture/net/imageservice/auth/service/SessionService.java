package hackyourfuture.net.imageservice.auth.service;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import hackyourfuture.net.imageservice.auth.repository.SessionRepository;

// Manages session ids. On login we make a random id, store it with the user and an
// expiry, and send it in a cookie.
@Service
public class SessionService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final SessionRepository sessions;

    // How long a session lasts, from app.session.ttl-days (APP_SESSION_TTL_DAYS).
    private final Duration sessionTtl;

    public SessionService(SessionRepository sessions, @Value("${app.session.ttl-days}") int ttlDays) {
        this.sessions = sessions;
        this.sessionTtl = Duration.ofDays(ttlDays);
    }

    // The stored expiry and the cookie's Max-Age have to agree, so the controller
    // reads the lifetime from here rather than keeping its own copy.
    public Duration sessionTtl() {
        return sessionTtl;
    }

    // Make a session and return its id.
    public String createSession(int userId) {
        String sessionId = generateId();
        sessions.insert(sessionId, userId, Instant.now().plus(sessionTtl));
        return sessionId;
    }

    // Return the user id for a valid session, or empty.
    public Optional<Integer> authenticate(String sessionId) {
        return sessions.findUserId(sessionId, Instant.now());
    }

    // Delete a session (logout).
    public void deleteSession(String sessionId) {
        sessions.deleteById(sessionId);
    }

    private static String generateId() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
