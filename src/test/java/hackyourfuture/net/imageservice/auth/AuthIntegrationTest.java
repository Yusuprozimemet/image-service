package hackyourfuture.net.imageservice.auth;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.servlet.http.Cookie;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

// End-to-end auth flow over HTTP against a real PostgreSQL: register a user, log
// in to get a session cookie, then log out with it. Exercises the controller,
// Spring Security filter chain, bcrypt hashing, and the users + sessions tables
// together — the "auth works" smoke test.
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class AuthIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    @Autowired
    private MockMvc mvc;

    @Test
    void registerThenLoginThenLogout() throws Exception {
        String credentials = """
                {"email":"alice@example.com","password":"password123"}
                """;

        // Register -> 201 with the created user (password never echoed back).
        mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credentials))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").isNumber())
                .andExpect(jsonPath("$.email").value("alice@example.com"));

        // Login -> 204 and an HttpOnly session cookie.
        Cookie session = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credentials))
                .andExpect(status().isNoContent())
                .andExpect(cookie().exists("session"))
                .andExpect(cookie().httpOnly("session", true))
                .andReturn()
                .getResponse()
                .getCookie("session");

        // Logout carrying that cookie -> 204.
        mvc.perform(post("/api/auth/logout").cookie(session))
                .andExpect(status().isNoContent());
    }
}
