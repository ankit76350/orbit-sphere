package com.orbitastra.backend.common.current;

import org.springframework.stereotype.Component;

import com.orbitastra.backend.common.error.exception.ApiException;
import com.orbitastra.backend.models.core.School;
import com.orbitastra.backend.models.core.enums.SchoolStatus;
import com.orbitastra.backend.repositories.core.school.SchoolRepository;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

/**
 * Works out which school the caller belongs to, for every {@code /schools/current} endpoint.
 *
 * <p>This is Phase 0.2 of the plan in {@code controllers/core/README.md}, and it exists as one
 * class on purpose. Every school-facing endpoint needs the answer, and resolving it inline in
 * each controller would mean changing four places — then forty — the day real sessions arrive.
 * <b>That is what paid off twice:</b> the tenant moved from a header to a signed cookie, and then
 * the header was deleted outright, and the 116 call sites across 22 files never changed.
 *
 * <h2>There is exactly one source: the {@code idtoken} cookie</h2>
 *
 * <p>The {@code schoolId} claim of the verified cookie, and nothing else. <b>The
 * {@code X-School-Subdomain} header is gone</b> — not deprecated, not a fallback, not read. A
 * caller that sends it gets the same {@code TENANT_NOT_RESOLVED} refusal as a caller that sends
 * nothing, because as far as this class is concerned they did send nothing.
 *
 * <p><b>The practical consequence, stated plainly:</b> a client must call {@code POST /local-user}
 * before it can reach any school-scoped endpoint. In the API tester that is the Sign in button.
 * There is no way to name a school on a single request any more, which is the point.
 *
 * <p><b>The cookie carries a document id, not a subdomain</b>, so the lookup is {@code findById}.
 * {@code School.id} is what every response calls {@code schoolId} and what {@code SchoolBase
 * .schoolId} is copied from, so the claim is already the right key — no translation, and one fewer
 * place to get a tenant wrong.
 *
 * <h2>What this is not</h2>
 *
 * <p><b>Authentication.</b> {@code POST /local-user} mints a token for anybody who asks, asserting
 * any {@code schoolId}. The signature proves this server issued the token; it says nothing about
 * whether the holder is entitled to that school. So the tenant is still, in the end, whatever the
 * caller asked for — tamper-evident rather than trustworthy.
 *
 * <p>That is no weaker than the header it replaces, which was plain text anybody could type. But it
 * <i>looks</i> like identity now, and that is the trap: a signed JWT in a cookie called
 * {@code idtoken} reads like a credential to everybody who meets it later. It becomes a real
 * boundary when {@code POST /local-user} starts refusing unauthenticated callers, and only then.
 *
 * <p><b>Why the tenant is still not a path parameter.</b> {@code /schools/{id}} invites the bug
 * where an admin passes somebody else's id and edits their school. Resolving the tenant outside the
 * request path makes that structurally impossible — a caller cannot name a school they do not
 * belong to, because they never name one at all. The cookie keeps that property. Keep it.
 */
@Component
@RequiredArgsConstructor
public class CurrentSchoolResolver {

    /** The claim the cookie carries. Written by {@code LocalUserController}. */
    public static final String SCHOOL_CLAIM = "schoolId";

    private final SchoolRepository schools;
    private final IdTokenCookie idToken;
    private final HttpServletRequest request; //! ← injected ONCE, at startup this is helping to pass the request http here..

    /**
     * Returns the caller's school from the {@code idtoken} cookie.
     *
     * <p>400 when there is no usable cookie, 404 when it names a school that is not there. Unknown
     * ids are 404 rather than 403 so that nothing here reveals whether another school exists.
     */
    public School require() {
        //! A BAD COOKIE THROWS OUT OF HERE - forged, expired or corrupt is a refusal naming the
        //! reason, not a quiet "no school". There is nothing left to fall through to anyway.
        String schoolId = idToken.claim(request, SCHOOL_CLAIM);

        if (schoolId == null) {
            throw ApiException.badRequest("TENANT_NOT_RESOLVED",
                    "No school could be resolved for this request. Sign in first — POST "
                            + "/local-user with a schoolId — so the " + IdTokenCookie.COOKIE_NAME
                            + " cookie carries one. Headers are no longer read.");
        }

        return schools.findById(schoolId)
                .orElseThrow(() -> ApiException.notFound("SCHOOL_NOT_FOUND",
                        "No school found for the id in the " + IdTokenCookie.COOKIE_NAME
                                + " cookie. It may have been deleted since you signed in — "
                                + "sign in again."));
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
