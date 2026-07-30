package hackyourfuture.net.imageservice.auth.service;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import hackyourfuture.net.imageservice.auth.model.User;
import hackyourfuture.net.imageservice.auth.repository.UserRepository;
import hackyourfuture.net.imageservice.shared.ApiException;

// Registration, login and logout. Passwords are hashed with bcrypt.
@Service
public class AuthService {

    private final UserRepository users;
    private final SessionService sessions;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UserRepository users, SessionService sessions, PasswordEncoder passwordEncoder) {
        this.users = users;
        this.sessions = sessions;
        this.passwordEncoder = passwordEncoder;
    }

    // Creates an account. Fails if the email is already taken.
    public User register(String email, String rawPassword) {
        if (users.findByEmail(email).isPresent()) {
            throw new ApiException(HttpStatus.CONFLICT, "email already registered");
        }
        return users.insert(email, passwordEncoder.encode(rawPassword));
    }

    // Verifies the password and returns a fresh session id.
    public String login(String email, String rawPassword) {
        // Same error for wrong email or password, so we don't reveal which emails exist.
        User user = users.findByEmail(email)
                .filter(u -> passwordEncoder.matches(rawPassword, u.passwordHash()))
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "invalid email or password"));
        return sessions.createSession(user.userId());
    }

    // Ends the given session.
    public void logout(String sessionId) {
        sessions.deleteSession(sessionId);
    }
}
