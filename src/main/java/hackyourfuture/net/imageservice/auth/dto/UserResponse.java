package hackyourfuture.net.imageservice.auth.dto;

// Returned on register: the created account (never includes the password).
public record UserResponse(int userId, String email) {
}
