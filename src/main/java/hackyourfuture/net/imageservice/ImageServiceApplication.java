package hackyourfuture.net.imageservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration;

// We use our own security (SecurityConfig), so turn off Spring's default login user.
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
public class ImageServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ImageServiceApplication.class, args);
    }

}
