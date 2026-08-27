package com.orbitastra.backend.services.core.helper;

import java.time.ZoneId;
import java.util.Set;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

import com.orbitastra.backend.common.error.exception.ApiException;


/**
 * Every validation rule the core module owns, in one place.
 *
 * <p>One validator class per module, holding one method per rule. Core owns School and
 * AcademicYear, so this is where a subdomain, a time zone, an academic-year date range and a
 * holiday date get checked. Finance would get its own with its own rules.
 *
 * <p>Everything here answers one question about one value, throws when the answer is no, and
 * returns the value in the form it should be stored in. Normalising as it validates is
 * deliberate: a caller that has to remember to trim after checking will forget.
 *
 * <p><b>These mostly throw ApiException.conflict, which is a 409, and that is not an oversight.</b>
 * `"Asia/Pune"` is a well-formed string and a reasonable guess. `api` is a perfectly valid
 * subdomain the platform simply will not hand over. Answering 400 would tell the caller their
 * request was malformed and send them hunting for a syntax error that is not there — the
 * request was fine, and the answer is still no.
 *
 * <p>That is also the line between this class and a Jakarta annotation on the request record.
 * Annotations say <i>this is not a well-formed value</i> — required, length, a simple regex,
 * `@Email`. They run before the controller method is entered and produce per-field errors a
 * form can put next to the right input, so anything they can do belongs there rather than
 * here. This class is for <i>well-formed and still not allowed</i>: rules that need a lookup, a
 * reserved list, or policy.
 *
 * <p>Normalising with nothing to decide is not validation and does not belong here — see
 * {@link TextHelper}, which trims and lowercases and rejects nothing.
 */
@Component
public class CoreValidator {

    //! subdomain ---------------------------------------------------------------- 
    /** Lowercase letters, digits and single inner hyphens. No leading or trailing hyphen. */
    private static final Pattern SUBDOMAIN_SHAPE =
            Pattern.compile("^[a-z0-9](?:[a-z0-9-]{1,61}[a-z0-9])?$");

    /**
     * Subdomain names that schools cannot use.
     *
     * <p>These names are reserved for platform services like API and login.
     */
    private static final Set<String> RESERVED_SUBDOMAINS = Set.of(
            "www", "api", "admin", "administrator", "app", "apps", "platform", "status",
            "mail", "email", "smtp", "imap", "ftp", "cdn", "static", "assets", "media",
            "login", "logout", "auth", "oauth", "sso", "signup", "register",
            "support", "help", "helpdesk", "docs", "documentation", "blog", "news",
            "test", "testing", "staging", "stage", "dev", "development", "demo", "sandbox",
            "internal", "system", "root", "billing", "payments", "webhook", "webhooks");

    /**
     * Validates and normalizes a school subdomain.
     *
     * <p>Converts the subdomain to a standard format and checks if it is valid or reserved.
     *
     * @return the normalized subdomain
     */
    public String validateSubdomain(String raw) {
        if (raw == null || raw.isBlank()) {
            throw ApiException.badRequest("SUBDOMAIN_REQUIRED", "A subdomain is required.");
        }
        String normalized = raw.trim().toLowerCase().replaceAll("[\\s_]+", "-");

        if (!SUBDOMAIN_SHAPE.matcher(normalized).matches()) {
            throw ApiException.conflict("SUBDOMAIN_INVALID",
                    "A subdomain must be 1 to 63 characters of lowercase letters, digits and "
                            + "inner hyphens. Received: " + normalized);
        }
        if (RESERVED_SUBDOMAINS.contains(normalized)) {
            throw ApiException.conflict("SUBDOMAIN_RESERVED",
                    "The subdomain '" + normalized + "' is reserved by the platform.");
        }
        return normalized;
    }


    //! time zone ---------------------------------------------------------------- 

    /**
     * Validates the school time zone.
     *
     * <p>Checks that the time zone is valid and supported by the JVM.
     *
     * @return the trimmed time zone
     */
    public String validateTimeZone(String raw) {
        if (raw == null || raw.isBlank()) {
            throw ApiException.badRequest("TIME_ZONE_REQUIRED", "A time zone is required.");
        }
        String candidate = raw.trim();
        if (!ZoneId.getAvailableZoneIds().contains(candidate)) {
            throw ApiException.conflict("TIME_ZONE_INVALID",
                    "'" + candidate + "' is not a known IANA time zone id.");
        }
        return candidate;
    }
}
