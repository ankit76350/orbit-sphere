package com.orbitastra.backend.controllers.people.organization;

import java.net.URI;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.orbitastra.backend.common.access.ActionGate;
import com.orbitastra.backend.common.current.CurrentSchoolResolver;
import com.orbitastra.backend.dto.people.organization.request.DepartmentCreateRequest;
import com.orbitastra.backend.dto.people.organization.response.DepartmentResponse;
import com.orbitastra.backend.models.core.School;
import com.orbitastra.backend.services.people.OrganizationService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * The org chart a school hires into. Endpoints #9 to #15 of the plan in this package's README;
 * #9 is built.
 *
 * <p><b>One controller for two documents</b>, because a position outside a department is not a
 * thing. They only exist in relation to each other, which makes them one subject.
 *
 * <p><b>School surface only, and that is deliberate.</b> The tenant comes from
 * {@link CurrentSchoolResolver} and never from the URL. There is no platform surface anywhere in
 * {@code people}: a school's org chart is its own, and an operator route into it would be a
 * standing invitation to read one school's structure while holding another's session.
 *
 * <p><b>Two gates, not three.</b> Gate 4 asks whether a named academic year is the school's
 * working one, and no path here carries a year to ask it about. An org chart outlives any year —
 * a department exists before the first year opens and after the last one ends.
 *
 * <p><b>There is no {@code DELETE}</b>: a unit is deactivated (#11), and that is refused while
 * active positions remain. Positions reference a department by id and none of those references is
 * a foreign key, so "is this still used" is a query rather than a constraint.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/schools/current/departments")
public class OrganizationController {

    private final OrganizationService organizationService;
    private final CurrentSchoolResolver currentSchool;
    private final ActionGate gate;

    /**
     * Endpoint #9 — create an org unit, optionally under another.
     *
     * <p>The first call anyone makes against this module: a position needs a department, and the
     * write that employs somebody needs a position.
     */
    @PostMapping
    public ResponseEntity<DepartmentResponse> createDepartment(
            @Valid @RequestBody DepartmentCreateRequest request) {

        //! Gate 1 — is the school itself live ---------------------------------------------
        //! Gate 2 — is the school paying --------------------------------------------------
        //! No gate 4: there is no academic year in this path to ask it about.
        School school = currentSchool.require();
        gate.requireActiveSchool(school);
        gate.requireUsableSubscription(school);

        DepartmentResponse response = organizationService.createDepartment(request);

        //! The document id, not the code: every other endpoint in this package is
        //! /departments/{id}, so a Location built from the code would name a URL no route answers.
        return ResponseEntity
                .created(URI.create("/schools/current/departments/"
                        + response.departmentDocsId()))
                .body(response);
    }
}
