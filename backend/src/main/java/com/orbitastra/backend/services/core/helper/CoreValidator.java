package com.orbitastra.backend.services.core.helper;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Set;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

import com.orbitastra.backend.common.error.exception.ApiException;
import com.orbitastra.backend.models.core.AcademicYear;
import com.orbitastra.backend.repositories.core.AcademicYearRepository;

import lombok.RequiredArgsConstructor;


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
@RequiredArgsConstructor
public class CoreValidator {

    /**
     * Needed by the overlap check, which is the first rule here that cannot be answered from
     * the value alone — it has to know what the school already has. The subdomain and time-zone
     * checks stay pure.
     */
    private final AcademicYearRepository academicYears;

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

    //! academic year dates ------------------------------------------------------------

    /** A year is a year. Outside this range it is a typo, not a calendar. */
    private static final long MIN_YEAR_DAYS = 30;
    private static final long MAX_YEAR_DAYS = 800;

    /**
     * A start before an end, and a span that looks like a school year.
     *
     * <p>The length check is a typo guard, not a rule about how schools work. A three-day
     * "year" and a five-year one are both somebody mistyping a date, and letting either through
     * means every later question about which year a date belongs to has a strange answer.
     */
    public void validateAcademicYearRange(LocalDate start, LocalDate end) {
        if (!start.isBefore(end)) {
            throw ApiException.badRequest("INVALID_DATE_RANGE",
                    "startDate (" + start + ") must be before endDate (" + end + ").");
        }
        long days = ChronoUnit.DAYS.between(start, end) + 1;
        if (days < MIN_YEAR_DAYS || days > MAX_YEAR_DAYS) {
            throw ApiException.badRequest("IMPLAUSIBLE_DATE_RANGE",
                    "An academic year of " + days + " days is almost certainly a typo. Expected "
                            + "between " + MIN_YEAR_DAYS + " and " + MAX_YEAR_DAYS + " days.");
        }
    }

    /**
     * No two years of one school may cover the same date.
     *
     * <p>Two ranges overlap unless one ends before the other starts. Written that way rather
     * than as four comparisons because the four-way version is where off-by-one bugs live — and
     * adjacency must stay legal: a year ending 03-31 and the next starting 04-01 are fine.
     *
     * <p>This matters more than it looks. AcademicYear deliberately has no {@code current} flag,
     * so "which year is this date in" is answered from the dates alone. Two years covering one
     * day would give that question two answers.
     *
     * @param ignoreId the year being edited, excluded so it does not overlap itself
     */
    public void validateNoAcademicYearOverlap(String schoolId, String ignoreId,
            LocalDate start, LocalDate end) {

        for (AcademicYear other : academicYears.findBySchoolId(schoolId)) {
            if (other.getId().equals(ignoreId)) {
                continue;
            }
            boolean apart = end.isBefore(other.getStartDate()) || start.isAfter(other.getEndDate());
            if (!apart) {
                throw ApiException.conflict("ACADEMIC_YEAR_OVERLAP",
                        "These dates overlap '" + other.getName() + "' ("
                                + other.getStartDate() + " to " + other.getEndDate()
                                + "). Two years cannot cover the same day.");
            }
        }
    }
}
