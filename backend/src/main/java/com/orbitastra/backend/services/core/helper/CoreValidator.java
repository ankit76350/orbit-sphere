package com.orbitastra.backend.services.core.helper;

import java.time.ZoneId;
import java.util.Set;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

import com.orbitastra.backend.common.error.exception.BadRequestException;
import com.orbitastra.backend.common.error.exception.ConflictException;

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
 * <p><b>These all throw ConflictException, which is a 409, and that is not an oversight.</b>
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
     * Labels a school may not take.
     *
     * <p>This list is the reason the subdomain check exists at all. **A school that claims
     * {@code api} or {@code login} receives traffic and credentials meant for the platform** —
     * a parent typing their password into what they believe is the login page would be sending
     * it to whoever owns that tenant. Not a naming preference.
     */
    private static final Set<String> RESERVED_SUBDOMAINS = Set.of(
            "www", "api", "admin", "administrator", "app", "apps", "platform", "status",
            "mail", "email", "smtp", "imap", "ftp", "cdn", "static", "assets", "media",
            "login", "logout", "auth", "oauth", "sso", "signup", "register",
            "support", "help", "helpdesk", "docs", "documentation", "blog", "news",
            "test", "testing", "staging", "stage", "dev", "development", "demo", "sandbox",
            "internal", "system", "root", "billing", "payments", "webhook", "webhooks");

    /**
     * Normalises and vets a tenant subdomain.
     *
     * <p>{@code School.subdomain} is the globally unique label that resolves a request to a
     * tenant, so it is the one string in this system where two spellings of the same thing is a
     * security problem rather than an annoyance. Normalising on the way in means
     * {@code "Orbit-Astra "} and {@code "orbit-astra"} cannot become two schools.
     *
     * @return the normalised label, ready to store
     * @throws ConflictException codes {@code SUBDOMAIN_REQUIRED}, {@code SUBDOMAIN_INVALID},
     *                           {@code SUBDOMAIN_RESERVED}
     */
    public String validateSubdomain(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new BadRequestException("SUBDOMAIN_REQUIRED", "A subdomain is required.");
        }
        String normalized = raw.trim().toLowerCase().replaceAll("[\\s_]+", "-");

        if (!SUBDOMAIN_SHAPE.matcher(normalized).matches()) {
            throw new ConflictException("SUBDOMAIN_INVALID",
                    "A subdomain must be 1 to 63 characters of lowercase letters, digits and "
                            + "inner hyphens. Received: " + normalized);
        }
        if (RESERVED_SUBDOMAINS.contains(normalized)) {
            throw new ConflictException("SUBDOMAIN_RESERVED",
                    "The subdomain '" + normalized + "' is reserved by the platform.");
        }
        return normalized;
    }


    //! time zone ---------------------------------------------------------------- 

    /**
     * Checks that a time zone is one the JVM actually knows.
     *
     * <p>Rejected on the way in rather than stored, because a bad zone is not discovered until
     * something tries to work out which calendar date a timestamp falls on — and by then
     * attendance has been taken. {@code School.defaultTimeZone} is read by attendance,
     * timetables, holidays, transport trips and fee due dates, so a wrong value there is wrong
     * in every one of them at once.
     *
     * <p>A Jakarta annotation cannot do this job. Nothing in the validation API knows the IANA
     * list, and a regex over it would be wrong within a year — zones are added and renamed.
     * Asking the JVM is the only check that stays correct.
     *
     * @return the trimmed zone id
     * @throws ConflictException codes {@code TIME_ZONE_REQUIRED}, {@code TIME_ZONE_INVALID}
     */
    public String validateTimeZone(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new ConflictException("TIME_ZONE_REQUIRED", "A time zone is required.");
        }
        String candidate = raw.trim();
        if (!ZoneId.getAvailableZoneIds().contains(candidate)) {
            throw new ConflictException("TIME_ZONE_INVALID",
                    "'" + candidate + "' is not a known IANA time zone id.");
        }
        return candidate;
    }
}
