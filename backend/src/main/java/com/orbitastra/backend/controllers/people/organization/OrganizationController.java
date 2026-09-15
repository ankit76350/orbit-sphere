package com.orbitastra.backend.controllers.people.organization;

import java.net.URI;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.orbitastra.backend.common.access.ActionGate;
import com.orbitastra.backend.common.current.CurrentSchoolResolver;
import com.orbitastra.backend.common.error.exception.ApiException;
import com.orbitastra.backend.common.web.PageResponse;
import com.orbitastra.backend.dto.people.organization.request.DepartmentCreateRequest;
import com.orbitastra.backend.dto.people.organization.request.DepartmentSearchRequest;
import com.orbitastra.backend.dto.people.organization.request.PositionCreateRequest;
import com.orbitastra.backend.dto.people.organization.response.DepartmentResponse;
import com.orbitastra.backend.dto.people.organization.response.PositionResponse;
import com.orbitastra.backend.models.core.School;
import com.orbitastra.backend.services.people.OrganizationService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * The org chart a school hires into. Endpoints #9 to #15 of the plan in this package's README;
 * #9, #12 and #13 are built.
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
@RequestMapping("/schools/current")
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
    @PostMapping("/departments")
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

    /**
     * Endpoint #13 — create an approved seat inside a department.
     *
     * <p><b>A seat has no code.</b> {@code positionCode} was removed on 2026-09-15, so this
     * answers with the document id — which is what {@code EmploymentRecord.positionDocsId} stores
     * and what #16 will need to employ anybody.
     *
     * <p><b>The department must be active</b>, not merely present: a seat nobody may be hired
     * into, inside a unit that no longer exists, is two problems rather than one.
     */
    @PostMapping("/positions")
    public ResponseEntity<PositionResponse> createPosition(
            @Valid @RequestBody PositionCreateRequest request) {

        //! Gate 1 — is the school itself live ---------------------------------------------
        //! Gate 2 — is the school paying --------------------------------------------------
        //! No gate 4: there is no academic year in this path to ask it about.
        School school = currentSchool.require();
        gate.requireActiveSchool(school);
        gate.requireUsableSubscription(school);

        PositionResponse response = organizationService.createPosition(request);
        return ResponseEntity
                .created(URI.create("/schools/current/positions/" + response.positionDocsId()))
                .body(response);
    }

    /**
     * Endpoint #12 — the tree, or one flat filtered page.
     *
     * <p><b>{@code ?tree=true} changes the shape of the response</b>, which is why it is the one
     * parameter here worth reading twice. Flat is a page envelope; the tree is nested roots.
     *
     * <p><b>A tree cannot be paged, and asking is refused rather than ignored.</b> A page boundary
     * in a tree cuts children off their parents — what comes back is not a partial tree but a
     * broken one, and a client that asked for page 2 would get something that looks like a chart
     * and is not. Silently dropping the parameter would hide that; a 400 says it.
     *
     * <p><b>No gate runs on a read.</b> A suspended or closed school still reads its own org
     * chart — the same rule every read in this project follows.
     */
    @GetMapping("/departments")
    public ResponseEntity<Object> listDepartments(DepartmentSearchRequest request) {

        if (Boolean.TRUE.equals(request.tree())) {
            if (request.page() != null || request.size() != null) {
                throw ApiException.badRequest("TREE_CANNOT_BE_PAGED",
                        "A tree has no page boundary: cutting one would separate children from "
                                + "their parents and return a broken chart rather than part of "
                                + "one. Drop page and size, or drop tree.");
            }

            return ResponseEntity.ok(organizationService.treeOfDepartments(request));
        }

        PageResponse<?> page = organizationService.listDepartments(request);
        return ResponseEntity.ok(page);
    }
}
