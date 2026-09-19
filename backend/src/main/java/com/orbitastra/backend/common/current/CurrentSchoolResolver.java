package com.orbitastra.backend.common.current;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.orbitastra.backend.common.error.exception.ApiException;
import com.orbitastra.backend.models.core.School;
import com.orbitastra.backend.models.core.enums.SchoolStatus;
import com.orbitastra.backend.repositories.core.school.SchoolRepository;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Works out which school the caller belongs to, for every {@code /schools/current} endpoint.
 *
 * <p>This is Phase 0.2 of the plan in {@code controllers/core/README.md}, and it exists as one
 * class on purpose. Every school-facing endpoint needs the answer, and resolving it inline in
 * each controller would mean changing four places — then forty — the day real sessions arrive.
 * <b>That is what just paid off:</b> the tenant moved from a header to a signed cookie and the 116
 * call sites across 22 files did not change at all.
 *
 * <h2>Where the answer comes from</h2>
 *
 * <ol>
 *   <li>The {@code schoolId} claim of the verified {@code idtoken} cookie. This is the route.</li>
 *   <li>Failing that, the {@code X-School-Subdomain} header — <b>a migration fallback</b>, see
 *       below.</li>
 * </ol>
 *
 * <p><b>The cookie carries a document id, the header carries a subdomain</b>, so the two take
 * different lookups: {@code findById} and {@code findBySubdomain}. {@code School.id} is what every
 * response calls {@code schoolId} and what {@code SchoolBase.schoolId} is copied from, so the claim
 * is already the right key — no translation, and one fewer place to get a tenant wrong.
 *
 * <h2>Why the header is still here</h2>
 *
 * <p>The brief was to read the cookie <i>instead of</i> the header. Cookie-first does that for
 * every caller that has signed in. Deleting the header outright would have broken, in the same
 * commit, the Postman collection, every verification suite, and every tester screen until somebody
 * pressed Sign in — with a 400 that looks like the new code is broken rather than like a tool that
 * needs re-pointing.
 *
 * <p>So it is a fallback, and it is built to be removed: set
 * {@code app.local-user.allow-header-fallback=false} to get cookie-only behaviour without touching
 * code — which is also how to <i>test</i> cookie-only refusals from the API tester. When the
 * collection and the suites have moved over, delete {@link #TENANT_HEADER}, the flag, and the
 * second half of {@link #require()}.
 *
 * <h2>What this does not become</h2>
 *
 * <p><b>Authentication.</b> {@code POST /local-user} mints a token for anybody who asks, asserting
 * any {@code schoolId}. The signature proves this server issued the token; it says nothing about
 * whether the holder is entitled to that school. Against a header anybody could type this is a
 * lateral move — tamper-evident rather than trustworthy — and calling it a login would be the
 * mistake that gets it shipped. It becomes a boundary when {@code POST /local-user} starts
 * refusing unauthenticated callers, and only then.
 *
 * <p><b>Why the tenant is still not a path parameter.</b> {@code /schools/{id}} invites the bug
 * where an admin passes somebody else's id and edits their school. Resolving the tenant outside the
 * request path makes that structurally impossible — a caller cannot name a school they do not
 * belong to, because they never name one at all. The cookie keeps that property. Keep it.
 */
@Component
public class CurrentSchoolResolver {

    /**
     * The migration fallback. Any caller can set it to any value; that is why it is on its way out.
     */
    public static final String TENANT_HEADER = "X-School-Subdomain";

    /** The claim the cookie carries. Written by {@code LocalUserController}. */
    public static final String SCHOOL_CLAIM = "schoolId";

    private final SchoolRepository schools;
    private final IdTokenCookie idToken;
    private final HttpServletRequest request; //! ← injected ONCE, at startup this is helping to pass the request http here..
    private final boolean allowHeaderFallback;

    public CurrentSchoolResolver(
            SchoolRepository schools,
            IdTokenCookie idToken,
            HttpServletRequest request,
            @Value("${app.local-user.allow-header-fallback:true}") boolean allowHeaderFallback) {
        this.schools = schools;
        this.idToken = idToken;
        this.request = request;
        this.allowHeaderFallback = allowHeaderFallback;
    }

    /**
     * Returns the caller's school from the {@code idtoken} cookie, falling back to the tenant
     * header while the older tools catch up.
     *
     * <p>Unknown schools are 404 either way, and the message says which of the two routes was
     * taken, because "no school found" without that is the least useful sentence in the system.
     */
    public School require() {
        //! A BAD COOKIE THROWS HERE, it does not fall through to the header. A caller presenting a
        //! forged or expired token is asserting something untrue, and silently demoting that to
        //! "not signed in" would hide exactly the case worth seeing - and would let anybody dodge
        //! a rejected token by also sending a header.
        String schoolId = idToken.claim(request, SCHOOL_CLAIM);

        if (schoolId != null) {
            return schools.findById(schoolId)
                    .orElseThrow(() -> ApiException.notFound("SCHOOL_NOT_FOUND",
                            "No school found for the id in the " + IdTokenCookie.COOKIE_NAME
                                    + " cookie. It may have been deleted since you signed in — "
                                    + "sign in again."));
        }

        if (!allowHeaderFallback) {
            throw ApiException.badRequest("TENANT_NOT_RESOLVED",
                    "No school could be resolved for this request. Sign in first, so the "
                            + IdTokenCookie.COOKIE_NAME + " cookie carries a " + SCHOOL_CLAIM + ".");
        }

        //! EVERYTHING BELOW IS THE FALLBACK and is meant to be deleted. See the class note.
        String subdomain = request.getHeader(TENANT_HEADER);
        if (subdomain == null || subdomain.isBlank()) {
            throw ApiException.badRequest("TENANT_NOT_RESOLVED",
                    "No school could be resolved for this request. Sign in so the "
                            + IdTokenCookie.COOKIE_NAME + " cookie carries a " + SCHOOL_CLAIM
                            + ", or send the " + TENANT_HEADER + " header.");
        }
        return schools.findBySubdomain(subdomain.trim().toLowerCase())
                .orElseThrow(() -> ApiException.notFound("SCHOOL_NOT_FOUND",
                        "No school found for subdomain '" + subdomain.trim() + "'."));
    }

    /**
     * Allows a school's admin to edit school details only when the school is ACTIVE.
     * Suspended, closed, or inactive schools cannot update their details.
     */
    public School requireUsable() {
        School school = require();
        //! only edit if they are in ACTIVE , PROVISIONING state
        if (school.getStatus() != SchoolStatus.ACTIVE
                && school.getStatus() != SchoolStatus.PROVISIONING) {
            throw ApiException.conflict("SCHOOL_NOT_EDITABLE",
                    "This school is " + school.getStatus() + " and cannot be edited.");
        }
        return school;
    }
}
