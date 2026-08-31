package com.orbitastra.backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Lets the local API Battleground call this backend straight from the browser.
 *
 * <p>The tester runs on its own port, so the browser treats every call to port 3456 as a
 * cross-site request and blocks it unless we say it is allowed. This says it is allowed, but
 * only for pages served from localhost.
 *
 * <p>The tester can also go through its own dev proxy instead, which needs none of this. This
 * is here so that pointing it straight at http://localhost:3456 works too.
 *
 * <p>Only switched on for the dev profile. It must never be on in production: opening the
 * browser to an API that provisions tenants is exactly what we do not want.
 */
@Configuration
@Profile("dev")
public class DevCorsConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                // Any port on localhost, because the dev server port can change.
                .allowedOriginPatterns("http://localhost:[*]", "http://127.0.0.1:[*]")
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                // Without this the browser hides these from JavaScript, and the tester needs to
                // show the caller every header that came back.
                .exposedHeaders("Location", "Content-Type", "Content-Length")
                .allowCredentials(false)
                .maxAge(3600);
    }
}
