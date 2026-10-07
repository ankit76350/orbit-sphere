package com.orbitastra.backend.controllers.student;

import java.net.URI;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.orbitastra.backend.common.access.ActionGate;
import com.orbitastra.backend.common.current.CurrentSchoolResolver;
import com.orbitastra.backend.dto.student.guardian.request.GuardianCreateRequest;
import com.orbitastra.backend.dto.student.guardian.response.GuardianDetailResponse;
import com.orbitastra.backend.models.core.School;
import com.orbitastra.backend.services.student.GuardianService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * The people a school contacts about its children. Endpoint #7 of the plan in this package's
 * README; #8 to #13 are not built.
 *
 * <p><b>Its own controller because {@code guardians} is its own collection</b>, and because the
 * surface says so: {@code /schools/current/guardians} sits beside {@code /students} rather than
 * under it. A guardian is not owned by a child — the same person belongs to siblings — so nesting
 * the route would have made one of their children the one they "really" belong to.
 *
 * <p><b>A guardian is one real person per school</b>, and the database decided that:
 * {@code school_guardian_phone_uniq} and {@code school_guardian_email_uniq} are unique and partial.
 * Everything else here follows from that.
 *
 * <p><b>School surface only.</b> The school comes from the {@code idtoken} cookie and never from
 * the URL. There is no platform route into a school's contacts and there will not be one: these
 * documents are families' phone numbers and home addresses.
 *
 * <p><b>Two gates on a write, none on a read.</b> No gate 4 anywhere — a contact has nothing to do
 * with which academic year is running.
 *
 * <p><b>There is no {@code DELETE}.</b> A guardian entered by mistake is corrected — #8 — and
 * taking a person off a child is #13, which <i>unlinks</i> and never deletes: the same row may be
 * three other children's mother.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/schools/current/guardians")
public class GuardianController {

    private final GuardianService guardianService;
    private final CurrentSchoolResolver currentSchool;
    private final ActionGate gate;

    /**
     * Endpoint #7 — <b>add a guardian who is not being created with a child</b>.
     *
     * <p><b>For the guardian who turns up on their own</b>: a grandmother added before the child
     * she will collect, an emergency number the office wants on file, a family entered ahead of an
     * admission. #1 makes guardians too, but only as a side effect of admitting a child.
     *
     * <p><b>No relation and no flags.</b> "Father", "primary contact" and "may collect" are facts
     * about a person <i>and a child together</i>, so they belong to the link — #11. A guardian
     * created here is attached to nobody, which is a normal state.
     *
     * <p><b>It refuses a taken number where #1 matches one, and that is the design.</b> #1 is
     * describing a family — "this child's father is on 98765 43210" — so linking the man the
     * school already knows is right, and refusing would make a sibling's admission fail. This is
     * asserting a new person, so handing back an existing row would look like a successful create
     * and leave somebody believing a guardian exists that does not.
     *
     * <pre>
     * 400 VALIDATION_FAILED     fullName missing, or a field over its length
     * 409 GUARDIAN_PHONE_TAKEN  somebody in this school already has that number
     * 409 GUARDIAN_EMAIL_TAKEN  somebody already has that address
     * 409 SCHOOL_NOT_EDITABLE   the school is suspended or closed
     * 400 TENANT_NOT_RESOLVED   no idtoken cookie
     * </pre>
     */
    @PostMapping
    public ResponseEntity<GuardianDetailResponse> addGuardian(
            @Valid @RequestBody GuardianCreateRequest request) {

        //! Gate 1 — is the school itself live ---------------------------------------------
        //! Gate 2 — is the school paying --------------------------------------------------
        //! No gate 4: a contact has nothing to do with which year is running.
        School school = currentSchool.require();
        gate.requireActiveSchool(school);
        gate.requireUsableSubscription(school);

        GuardianDetailResponse response = guardianService.createGuardian(request);

        return ResponseEntity
                .created(URI.create("/schools/current/guardians/" + response.guardianDocsId()))
                .body(response);
    }
}
