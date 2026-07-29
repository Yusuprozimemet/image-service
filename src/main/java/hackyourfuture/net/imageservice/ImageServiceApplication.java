package hackyourfuture.net.imageservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration;

// We provide our own token-based security (SecurityConfig), so Spring Boot's
// default in-memory user is unused — excluding it removes the noisy startup
// "Using generated security password" line.
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
public class ImageServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ImageServiceApplication.class, args);
    }

}
