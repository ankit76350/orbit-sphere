package com.orbitastra.backend.controllers.people.staff;

import java.net.URI;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.orbitastra.backend.common.access.ActionGate;
import com.orbitastra.backend.common.current.CurrentSchoolResolver;
import com.orbitastra.backend.common.web.PageResponse;
import com.orbitastra.backend.dto.people.staff.request.StaffCreateRequest;
import com.orbitastra.backend.dto.people.staff.request.StaffSearchRequest;
import com.orbitastra.backend.dto.people.staff.response.StaffCreatedResponse;
import com.orbitastra.backend.dto.people.staff.response.StaffRowResponse;
import com.orbitastra.backend.models.core.School;
import com.orbitastra.backend.services.people.StaffService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * The people a school employs. Endpoints #1 to #8 and #16 to #21 of the plan in this package's
 * README; #1 and #7 are built.
 *
 * <p><b>School surface only.</b> The tenant comes from {@link CurrentSchoolResolver} and never
 * from the URL. There is no platform surface anywhere in {@code people}: a school's staff are its
 * own, and an operator route into them would be a standing invitation to read one school's people
 * while holding another's session. That matters more here than anywhere else in the product — a
 * {@code Staff} document carries a date of birth, an address and an emergency contact.
 *
 * <p><b>Two gates, not three.</b> Gate 4 asks whether a named academic year is the school's
 * working one, and no path here carries a year to ask it about. A person outlives any year.
 *
 * <p><b>There is no {@code DELETE}.</b> A profile created by mistake is archived — #6 — and that
 * is not how somebody leaves: leaving is an employment event, which is #17.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/schools/current")
public class StaffController {

    private final StaffService staffService;
    private final CurrentSchoolResolver currentSchool;
    private final ActionGate gate;

    /**
     * Endpoint #1 — create a person.
     *
     * <p><b>The write five other modules are waiting for.</b> Payroll, leave, reviews, development
     * and every teacher picker need a {@code staffDocsId}, and nothing else in this product
     * produces one.
     *
     * <p><b>{@code employeeNo} is generated, never sent.</b> A caller-supplied number lets two
     * conventions collide inside one tenant, and nobody should pick their own staff number.
     *
     * <p><b>This creates a person, not an employee.</b> No status, no department, no joining date
     * — that is #16, and the distinction is this package's whole design.
     */
    @PostMapping("/staff")
    public ResponseEntity<StaffCreatedResponse> createStaff(
            @Valid @RequestBody StaffCreateRequest request) {

        //! Gate 1 — is the school itself live ---------------------------------------------
        //! Gate 2 — is the school paying --------------------------------------------------
        //! No gate 4: there is no academic year in this path to ask it about.
        School school = currentSchool.require();
        gate.requireActiveSchool(school);
        gate.requireUsableSubscription(school);

        StaffCreatedResponse response = staffService.createStaff(request);

        //! The document id, not the employee number: every other endpoint in this package is
        //! /staff/{id}, so a Location built from the number would name a URL no route answers.
        return ResponseEntity
                .created(URI.create("/schools/current/staff/" + response.staffDocsId()))
                .body(response);
    }

    /**
     * Endpoint #7 — one page of the school's people.
     *
     * <p><b>The row is thin, and that is the security decision on this endpoint.</b> Name, number,
     * gender, phone, email — no date of birth, no address, no emergency contact. Those are #8, one
     * call away, and a list that carried them would put every employee's personal data into the
     * network tab of every dropdown that reads it.
     *
     * <p><b>The four filters a teacher picker actually wants are not here yet.</b>
     * {@code ?employed=}, {@code ?departmentDocsId=}, {@code ?positionDocsId=} and
     * {@code ?employmentType=} all live on {@code EmploymentRecord}, which #16 writes and which
     * does not exist. Accepting them now would be a filter that silently matches nothing.
     *
     * <p><b>No gate runs on a read.</b> A suspended or closed school still reads its own staff.
     */
    @GetMapping("/staff")
    public ResponseEntity<PageResponse<StaffRowResponse>> listStaff(StaffSearchRequest request) {
        return ResponseEntity.ok(staffService.listStaff(request));
    }
}
