package hackyourfuture.net.imageservice.auth.repository;

import java.util.Optional;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import hackyourfuture.net.imageservice.auth.model.User;

// Reads and writes the users table.
@Repository
public class UserRepository {

    private static final RowMapper<User> ROW_MAPPER = (rs, rowNum) ->
            new User(rs.getInt("user_id"), rs.getString("email"), rs.getString("password_hash"));

    private final JdbcClient jdbc;

    public UserRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<User> findByEmail(String email) {
        return jdbc.sql("SELECT user_id, email, password_hash FROM users WHERE email = ?")
                .param(email)
                .query(ROW_MAPPER)
                .optional();
    }

    // Inserts a new user and returns it with the generated id.
    public User insert(String email, String passwordHash) {
        return jdbc.sql("""
                        INSERT INTO users (email, password_hash)
                        VALUES (?, ?)
                        RETURNING user_id, email, password_hash
                        """)
                .params(email, passwordHash)
                .query(ROW_MAPPER)
                .single();
    }
}
