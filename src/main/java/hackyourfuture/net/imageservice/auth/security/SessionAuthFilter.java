package hackyourfuture.net.imageservice.auth.security;

import java.io.IOException;
import java.util.List;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import hackyourfuture.net.imageservice.auth.service.SessionService;

// Runs on every request: if there is a session cookie whose id maps to a valid
// session, marks the request as authenticated with the user id as the principal.
@Component
public class SessionAuthFilter extends OncePerRequestFilter {

    // Name of the HttpOnly cookie that carries the opaque session id.
    public static final String SESSION_COOKIE = "session";

    private final SessionService sessions;

    public SessionAuthFilter(SessionService sessions) {
        this.sessions = sessions;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String sessionId = readSessionCookie(request);
        if (sessionId != null) {
            sessions.authenticate(sessionId).ifPresent(userId -> {
                var authentication = new UsernamePasswordAuthenticationToken(userId, null, List.of());
                SecurityContextHolder.getContext().setAuthentication(authentication);
            });
        }
        chain.doFilter(request, response);
    }

    private static String readSessionCookie(HttpServletRequest request) {
        if (request.getCookies() == null) {
            return null;
        }
        for (Cookie cookie : request.getCookies()) {
            if (SESSION_COOKIE.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }

    // Also run on the ERROR dispatch (e.g. a 404 forwarded to /error). Otherwise an
    // authenticated request to a missing route loses its auth on the re-dispatch and
    // comes back as a misleading 401 instead of 404.
    @Override
    protected boolean shouldNotFilterErrorDispatch() {
        return false;
    }
}
