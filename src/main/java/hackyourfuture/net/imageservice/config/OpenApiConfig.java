package hackyourfuture.net.imageservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import hackyourfuture.net.imageservice.auth.security.SessionAuthFilter;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;

// Swagger UI settings. Documents the session cookie so "Try it out" works after login.
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI imageServiceOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Image Service API")
                        .description("Registration, login/logout and image endpoints.")
                        .version("v0"))
                .components(new Components()
                        .addSecuritySchemes("sessionCookie", new SecurityScheme()
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.COOKIE)
                                .name(SessionAuthFilter.SESSION_COOKIE)));
    }
}
