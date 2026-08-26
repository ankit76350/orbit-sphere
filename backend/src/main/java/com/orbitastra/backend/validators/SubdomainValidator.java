package com.orbitastra.backend.services.core.helper;

import java.util.Set;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

import com.orbitastra.backend.common.exception.ConflictException;

/**
 * Normalises and vets a tenant subdomain.
 *
 * <p>{@code School.subdomain} is the globally unique label that resolves a request to a tenant,
 * so it is the one string in this system where two spellings of the same thing is a security
 * problem rather than an annoyance. Normalising on the way in means "Orbit-Astra " and
 * "orbit-astra" cannot become two schools.
 *
 * <p>The reserved list is the part worth reading. **A school that claims {@code api} or
 * {@code login} receives traffic and credentials meant for the platform** — a parent typing
 * their password into what they believe is the login page would be sending it to whoever owns
 * that tenant. That is not a naming preference; it is the reason this class exists.
 */
@Component
public class SubdomainPolicy {

    /** Lowercase letters, digits and single inner hyphens. No leading or trailing hyphen. */
    private static final Pattern SHAPE = Pattern.compile("^[a-z0-9](?:[a-z0-9-]{1,61}[a-z0-9])?$");

    private static final Set<String> RESERVED = Set.of(
            "www", "api", "admin", "administrator", "app", "apps", "platform", "status",
            "mail", "email", "smtp", "imap", "ftp", "cdn", "static", "assets", "media",
            "login", "logout", "auth", "oauth", "sso", "signup", "register",
            "support", "help", "helpdesk", "docs", "documentation", "blog", "news",
            "test", "testing", "staging", "stage", "dev", "development", "demo", "sandbox",
            "internal", "system", "root", "billing", "payments", "webhook", "webhooks");

    /**
     * Lowercases, trims, collapses whitespace to hyphens, then checks shape and reserved words.
     *
     * @throws ConflictException if the label is unusable — a 409 rather than a 400, because a
     *                           reserved word is a perfectly valid string the platform is
     *                           refusing to hand over
     */
    public String validateSubdomain(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new ConflictException("SUBDOMAIN_REQUIRED", "A subdomain is required.");
        }
        String normalized = raw.trim().toLowerCase().replaceAll("[\\s_]+", "-");

        if (!SHAPE.matcher(normalized).matches()) {
            throw new ConflictException("SUBDOMAIN_INVALID",
                    "A subdomain must be 1 to 63 characters of lowercase letters, digits and "
                            + "inner hyphens. Received: " + normalized);
        }
        if (RESERVED.contains(normalized)) {
            throw new ConflictException("SUBDOMAIN_RESERVED",
                    "The subdomain '" + normalized + "' is reserved by the platform.");
        }
        return normalized;
    }

    public boolean isReserved(String normalized) {
        return RESERVED.contains(normalized);
    }
}
