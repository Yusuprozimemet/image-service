package hackyourfuture.net.imageservice.auth.controller;

import java.time.Duration;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import hackyourfuture.net.imageservice.auth.dto.LoginRequest;
import hackyourfuture.net.imageservice.auth.dto.RegisterRequest;
import hackyourfuture.net.imageservice.auth.dto.UserResponse;
import hackyourfuture.net.imageservice.auth.model.User;
import hackyourfuture.net.imageservice.auth.security.SessionAuthFilter;
import hackyourfuture.net.imageservice.auth.service.AuthService;
import hackyourfuture.net.imageservice.auth.service.SessionService;

// Registration, login and logout endpoints. The session id travels in an HttpOnly cookie.
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService auth;

    // Secure cookies (HTTPS only). False for local dev over http, true in production.
    private final boolean cookieSecure;

    public AuthController(AuthService auth, @Value("${app.session.cookie-secure:false}") boolean cookieSecure) {
        this.auth = auth;
        this.cookieSecure = cookieSecure;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse register(@Valid @RequestBody RegisterRequest request) {
        User user = auth.register(request.email(), request.password());
        return new UserResponse(user.userId(), user.email());
    }

    @PostMapping("/login")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void login(@Valid @RequestBody LoginRequest request, HttpServletResponse response) {
        String sessionId = auth.login(request.email(), request.password());
        ResponseCookie cookie = buildSessionCookie(sessionId, SessionService.SESSION_TTL);
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@CookieValue(name = SessionAuthFilter.SESSION_COOKIE, required = false) String sessionId,
                       HttpServletResponse response) {
        if (sessionId != null) {
            auth.logout(sessionId);
        }
        // Expire the cookie on the client too, whether or not it was a known session.
        response.addHeader(HttpHeaders.SET_COOKIE, buildSessionCookie("", Duration.ZERO).toString());
    }

    private ResponseCookie buildSessionCookie(String value, Duration maxAge) {
        return ResponseCookie.from(SessionAuthFilter.SESSION_COOKIE, value)
                .httpOnly(true)          // not readable by JavaScript (mitigates XSS token theft)
                .secure(cookieSecure)    // only sent over HTTPS in production
                .sameSite("Strict")      // not sent on cross-site requests (mitigates CSRF)
                .path("/")
                .maxAge(maxAge)
                .build();
    }
}
