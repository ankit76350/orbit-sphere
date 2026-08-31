package com.orbitastra.backend.common.tenant;

import org.springframework.stereotype.Component;

import com.orbitastra.backend.common.error.exception.ApiException;
import com.orbitastra.backend.models.core.School;
import com.orbitastra.backend.models.core.enums.SchoolStatus;
import com.orbitastra.backend.repositories.core.SchoolRepository;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

/**
 * Works out which school the caller belongs to, for every {@code /schools/current} endpoint.
 *
 * <p>This is Phase 0.2 of the plan in {@code controllers/core/README.md}, and it exists as one
 * class on purpose. Every school-facing endpoint needs the answer, and resolving it inline in
 * each controller would mean changing four places — then forty — the day real sessions arrive.
 *
 * <p><b>It is a stand-in. There is no authentication yet.</b> Today the tenant comes from an
 * {@code X-School-Subdomain} header, which any caller can set to any value. That is fine on a
 * developer machine and completely unacceptable anywhere else: it means anybody can edit any
 * school by typing a different subdomain.
 *
 * <p>When sessions exist, only this class changes: read the school from the authenticated
 * session, and fall back to the subdomain of the host the request came in on. The controllers,
 * the services and the DTOs stay exactly as they are. That is the whole reason it is here rather
 * than spread across the endpoints.
 *
 * <p><b>Why the header and not {@code /schools/{id}}.</b> A path parameter invites the bug where
 * a school admin passes somebody else's id and edits their school. Resolving the tenant outside
 * the request path makes that structurally impossible — a caller cannot name a school they do
 * not belong to, because they never name one at all. Keep it that way when the header is
 * replaced.
 */
@Component
@RequiredArgsConstructor
public class CurrentSchoolResolver {

    /** Temporary. Replaced by the session, and by the host the request arrived on. */
    public static final String TENANT_HEADER = "X-School-Subdomain";

    private final SchoolRepository schools;
    private final HttpServletRequest request; //! ← injected ONCE, at startup this is helping to pass the request http here

    /**
     * Returns the caller's school using the subdomain and returns 400 or 404 for invalid requests.
     * Unknown subdomains return 404 to avoid revealing whether another school exists.
     */
    public School require() {
        String subdomain = request.getHeader(TENANT_HEADER);
        if (subdomain == null || subdomain.isBlank()) {
            throw ApiException.badRequest("TENANT_NOT_RESOLVED",
                    "No school could be resolved for this request. Send the "
                            + TENANT_HEADER + " header until authentication exists.");
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
        //! only edit if they are in ACTIVE , TRIAL or PROVISIONING state
        if (school.getStatus() != SchoolStatus.ACTIVE
                && school.getStatus() != SchoolStatus.TRIAL
                && school.getStatus() != SchoolStatus.PROVISIONING) {
            throw ApiException.conflict("SCHOOL_NOT_EDITABLE",
                    "This school is " + school.getStatus() + " and cannot be edited.");
        }
        return school;
    }
}
