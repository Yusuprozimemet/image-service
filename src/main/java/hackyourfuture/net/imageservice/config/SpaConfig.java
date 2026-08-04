package hackyourfuture.net.imageservice.config;

import java.io.IOException;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

// Serves the React bundle that the frontend build drops into classpath:/static.
//
// A single-page app owns its own routes, so a reload on /my or a pasted link to
// /login arrives here as a normal GET for a file that does not exist. Those have
// to return index.html and let the router sort it out — otherwise every deep
// link 404s. Anything the app genuinely does not serve (a real API 404, a
// mistyped /assets file) must still 404, so the fallback is deliberately narrow.
@Configuration
public class SpaConfig implements WebMvcConfigurer {

    // Not ours to serve: the API answers for itself, and springdoc owns its docs.
    private static final String[] NOT_SPA_ROUTES = {"api/", "v3/api-docs", "swagger-ui", "webjars/"};

    private static final ClassPathResource INDEX = new ClassPathResource("static/index.html");

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/")
                .resourceChain(true)
                .addResolver(new PathResourceResolver() {
                    @Override
                    protected Resource getResource(String resourcePath, Resource location) throws IOException {
                        Resource requested = location.createRelative(resourcePath);
                        if (requested.exists() && requested.isReadable()) {
                            return requested;
                        }
                        return isSpaRoute(resourcePath) ? INDEX : null;
                    }
                });
    }

    // A client-side route: not someone else's path, and not a request for a file
    // (those carry an extension, and a missing file should stay a 404).
    private static boolean isSpaRoute(String resourcePath) {
        for (String prefix : NOT_SPA_ROUTES) {
            if (resourcePath.startsWith(prefix)) {
                return false;
            }
        }
        return !resourcePath.contains(".");
    }
}
