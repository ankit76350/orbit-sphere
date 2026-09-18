package com.orbitastra.backend.dto.localuser.request;

import java.util.LinkedHashMap;
import java.util.Map;

import jakarta.validation.constraints.Size;

/**
 * Who this browser is acting as, to be stored in a cookie.
 *
 * <h2>This is a convenience, not a credential</h2>
 *
 * <p>Everything here is <b>supplied by the caller and stored unsigned</b>. A cookie set from this
 * request can be edited in the browser's developer tools in about four seconds, so it says who
 * somebody <i>claims</i> to be and nothing more. It exists so that a tester stops retyping three
 * ids into every request — see {@code LocalUserController} for what must never be built on it.
 *
 * <h2>Every field is optional</h2>
 *
 * <p>A tester who only wants the school in the cookie should not have to invent a staff id, and one
 * clearing a field should not have to send the other two. <b>An empty request is still a valid
 * request</b> — it stores an empty context, which is a real thing to want when checking what an
 * endpoint does without one.
 */
public record LocalUserContextRequest(

        /**
         * The staff member this browser is acting as — {@code Staff.id}.
         *
         * <p><b>Named {@code staffDocsId}, not {@code staffId}</b>, because this project's naming
         * rule is that a field holding another document's ObjectId carries the {@code DocsId}
         * suffix. It is the same field {@code TimetableEntry} and {@code AttendanceSession} store.
         */
        @Size(max = 120) String staffDocsId,

        /**
         * The school — {@code School.id}.
         *
         * <p><b>Named {@code schoolId}, not {@code schoolDocsId}</b>, matching
         * {@code SchoolBase.schoolId}, which is what every tenant-scoped document already calls it.
         * The two conventions sit side by side here because both are already in the codebase.
         *
         * <p><b>It is not a tenant boundary.</b> {@code X-School-Subdomain} still decides which
         * school a request acts on; this is a note to the browser, not an instruction to the
         * server.
         */
        @Size(max = 120) String schoolId,

        /** The academic year being worked in — {@code AcademicYear.name}, such as {@code 2026-2027}. */
        @Size(max = 40) String academicYear,

        /**
         * Anything else worth carrying — a role to act as, a section, a feature flag.
         *
         * <p><b>Capped, because a cookie is capped.</b> Browsers drop a cookie over about 4 KB
         * without telling anybody, so the controller refuses a context that would not fit rather
         * than setting one that silently vanishes.
         */
        @Size(max = 20, message = "at most 20 extra values")
        Map<String, String> extra,

        /**
         * How long the cookie should live, in seconds.
         *
         * <p>Absent means {@code 28800} — eight hours, about a working day, so a tester who set it
         * in the morning still has it after lunch. <b>Zero expires it immediately</b>, which is how
         * this endpoint clears a context without a second endpoint existing.
         */
        Integer maxAgeSeconds) {

    /** Eight hours: long enough for a working day, short enough not to outlive the branch. */
    public static final int DEFAULT_MAX_AGE_SECONDS = 28_800;

    /** Never null, so the controller does not have to ask twice. */
    public Map<String, String> safeExtra() {
        return extra == null ? new LinkedHashMap<>() : extra;
    }

    /** The requested lifetime, or the default when none was asked for. */
    public int resolvedMaxAgeSeconds() {
        return maxAgeSeconds == null ? DEFAULT_MAX_AGE_SECONDS : maxAgeSeconds;
    }
}
