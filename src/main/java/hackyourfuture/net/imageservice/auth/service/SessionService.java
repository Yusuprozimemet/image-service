package hackyourfuture.net.imageservice.auth.service;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;

import org.springframework.stereotype.Service;

import hackyourfuture.net.imageservice.auth.repository.SessionRepository;

// Manages opaque session ids. On login we generate a random id, store it with the
// user and an expiry, and hand it to the client in a cookie. The id is stored as-is:
// it is a long random string, not a password, and it is looked up directly.
@Service
public class SessionService {

    // How long a session stays valid after login. Also used for the cookie max-age.
    public static final Duration SESSION_TTL = Duration.ofDays(7);

    private static final SecureRandom RANDOM = new SecureRandom();

    private final SessionRepository sessions;

    public SessionService(SessionRepository sessions) {
        this.sessions = sessions;
    }

    // Creates a session for the user and returns the session id to send back.
    public String createSession(int userId) {
        String sessionId = generateId();
        sessions.insert(sessionId, userId, Instant.now().plus(SESSION_TTL));
        return sessionId;
    }

    // Returns the user id behind a still-valid session id, or empty if none.
    public Optional<Integer> authenticate(String sessionId) {
        return sessions.findUserId(sessionId, Instant.now());
    }

    // Ends a session on logout. Unknown ids are simply a no-op.
    public void deleteSession(String sessionId) {
        sessions.deleteById(sessionId);
    }

    private static String generateId() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
