package com.orbitastra.backend.controllers.academics.structure;

import java.net.URI;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.orbitastra.backend.common.access.ActionGate;
import com.orbitastra.backend.common.current.CurrentSchoolResolver;
import com.orbitastra.backend.dto.academics.schoolclass.request.SchoolClassCreateRequest;
import com.orbitastra.backend.dto.academics.schoolclass.response.SchoolClassResponse;
import com.orbitastra.backend.models.core.School;
import com.orbitastra.backend.services.academics.SchoolClassService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * The classes taught in one academic year, and the sections and subjects inside them. Endpoints
 * #12 to #16 and #28 to #31 of the plan in this package's README.
 *
 * <p>School surface, so the tenant comes from CurrentSchoolResolver and never from the URL. There
 * is no platform surface for classes: a class list is a school's own teaching structure.
 *
 * <p><b>A class is addressed by its document id</b> —
 * {@code /academic-years/2026-2027/classes/{id}}. That is what twelve other documents already
 * store as {@code classDocsId}, so the URL and the database say "which class" the same way.
 *
 * <p><b>The year is still in the path.</b> A class belongs to one year, and having it there means
 * a class id pasted from last year's URL answers 404 rather than editing last year's structure.
 *
 * <p><b>Sections and subjects get no controller of their own.</b> They are embedded, so there is
 * no document to address and only a path into one — the way holidays belong to
 * AcademicYearController.
 *
 * <p><b>The name is editable, unlike the academic year's.</b> A year IS its name to every other
 * collection; a class is its id, so a rename joins nothing and breaks nothing. It only has to
 * stay unique inside the year.
 *
 * <p><b>There is no {@code DELETE}</b>: a class is deactivated (#15). "Is this still used?" is a
 * query across three modules rather than a foreign-key check, so nothing here is removed.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/schools/current/academic-years/{year}/classes")
public class SchoolClassController {

    private final SchoolClassService schoolClassService;

    /**
     * The gates, and the resolver they need.
     *
     * <p>Every <b>write</b> here runs gates 1, 2 and 4; reads run none. The reasoning, including
     * why gate 3 is on none of them, is in
     * {@link com.orbitastra.backend.controllers.core.AcademicYearController} and in this
     * package's README — a class list is normally built in February for a year that starts in
     * June, so requiring today to be inside the year would refuse the only call a school makes.
     */
    private final CurrentSchoolResolver currentSchool;
    private final ActionGate gate;

    /**
     * Endpoint #12 — creates a class for one academic year.
     *
     * <p>The class is created <b>empty</b>: no sections, no subjects. Both go on afterwards
     * through #17 and #22.
     *
     * <pre>
     * 404 ACADEMIC_YEAR_NOT_FOUND          the {year} in the path is not a year of this school
     * 409 CLASS_NAME_TAKEN                 that year already has a class with that name
     * 404 AFFILIATION_PROGRAMME_NOT_FOUND  no such programme in this school
     * </pre>
     */
    @PostMapping
    public ResponseEntity<SchoolClassResponse> create(
            @PathVariable String year,
            @Valid @RequestBody SchoolClassCreateRequest request) {

        //! Gate 1 — is the school itself live ---------------------------------------------
        //! Gate 2 — is the school paying --------------------------------------------------
        //! Gate 4 — is this the school's working year, whatever the calendar says ---------
        School school = currentSchool.require();
        gate.requireActiveSchool(school);
        gate.requireUsableSubscription(school);
        gate.requireYearMarkedAsRunning(school, year);

        SchoolClassResponse response = schoolClassService.createClass(year, request);
        return ResponseEntity
                .created(URI.create("/schools/current/academic-years/" + year + "/classes/"
                        + response.schoolClassId()))
                .body(response);
    }
}
