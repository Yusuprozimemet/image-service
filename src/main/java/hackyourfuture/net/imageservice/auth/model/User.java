package hackyourfuture.net.imageservice.auth.model;

// A registered user. passwordHash is a bcrypt hash, never a raw password.
public record User(int userId, String email, String passwordHash) {
}
