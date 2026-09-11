package com.orbitastra.backend.controllers.academics.structure;

import java.net.URI;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.orbitastra.backend.common.access.ActionGate;
import com.orbitastra.backend.common.current.CurrentSchoolResolver;
import com.orbitastra.backend.dto.academics.schoolclass.request.SchoolClassCreateRequest;
import com.orbitastra.backend.dto.academics.schoolclass.request.SchoolClassSearchRequest;
import com.orbitastra.backend.dto.academics.schoolclass.request.SchoolClassUpdateRequest;
import com.orbitastra.backend.common.web.PageResponse;
import com.orbitastra.backend.dto.academics.schoolclass.response.SchoolClassResponse;
import com.orbitastra.backend.models.core.School;
import com.orbitastra.backend.services.academics.SchoolClassService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * The classes taught in one academic year, and the sections and subjects inside them. Endpoints
 * #12 to #16 and #28 to #31 of the plan in this package's README; #12, #13 and #28 are built.
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

    /**
     * Endpoint #13 — edits a class.
     *
     * <p>Three fields, all optional: the display name, the sort order, and the affiliation
     * programme. A body that sends none of them is a {@code 400}, not a silent success.
     *
     * <p><b>The class is named by its MongoDB document id</b>, which is what twelve other
     * documents store as {@code classDocsId}. The id is globally unique, so the {@code {year}} is
     * not needed to find the class — it is in the path so that an id pasted from last year's URL
     * answers {@code 404} instead of quietly editing last year's structure.
     *
     * <p><b>The name is editable, and an academic year's is not.</b> A year <i>is</i> its name to
     * every other collection; a class is its id, so nothing joins on this name and a rename
     * cascades nowhere. It only has to stay unique inside the year — and a class may keep the
     * name it already has, because the check compares ids rather than names.
     *
     * <p><b>Nothing structural is reachable from here.</b> No section, no subject, no
     * {@code active}. Those are #15 to #27, and an edit that could replace forty embedded rows
     * while looking like a rename is exactly what this shape avoids.
     *
     * <pre>
     * 400 NOTHING_TO_UPDATE                the body asks for nothing
     * 400 CLASS_NAME_REQUIRED              "name": "" — a name cannot be removed
     * 404 ACADEMIC_YEAR_NOT_FOUND          the {year} in the path is not a year of this school
     * 404 CLASS_NOT_FOUND                  no class with that id in that year
     * 409 CLASS_NAME_TAKEN                 another class in that year already has that name
     * 404 AFFILIATION_PROGRAMME_NOT_FOUND  no such programme in this school
     * </pre>
     */
    @PatchMapping("/{id}")
    public ResponseEntity<SchoolClassResponse> update(
            @PathVariable String year,
            @PathVariable String id,
            @Valid @RequestBody SchoolClassUpdateRequest request) {

        //! Gate 1 — is the school itself live ---------------------------------------------
        //! Gate 2 — is the school paying --------------------------------------------------
        //! Gate 4 — is this the school's working year, whatever the calendar says ---------
        School school = currentSchool.require();
        gate.requireActiveSchool(school);
        gate.requireUsableSubscription(school);
        gate.requireYearMarkedAsRunning(school, year);

        return ResponseEntity.ok(schoolClassService.updateClass(year, id, request));
    }

    /**
     * Endpoint #28 — one year's classes, filtered, sorted and paged.
     *
     * <p>The screen a school opens to see its own structure. Default order is
     * {@code displayOrder} ascending, which is the order a school reads its classes in.
     *
     * <pre>
     * ?page=0&amp;size=20                    the first page, in displayOrder
     * ?active=true                        only the classes in use
     * ?search=grade                       name contains "grade", case-insensitive
     * ?hasSections=false                  THE SETUP CHECKLIST — classes nothing can be placed in
     * ?hasSubjects=false                  nothing is taught in these yet
     * ?affiliationProgrammeDocsId=67aa…   only one board's classes
     * ?sort=name,desc                     by name instead
     * </pre>
     *
     * <p><b>{@code ?hasSections=false} is the one worth knowing.</b> A class with no section
     * cannot hold a student — {@code StudentAcademicRecord} stores {@code sectionNo} — so that is
     * the query a school runs to find what it has not finished setting up.
     *
     * <p><b>Rows carry counts, not the embedded lists.</b> A twelve-class year with four sections
     * and ten subjects each is 168 embedded rows nobody reads. #29 is for one class in full.
     *
     * <p><b>No gates.</b> Reads run none — looking at a structure is not an action on it, and a
     * school that has stopped paying still has to be able to read its own records. Which makes
     * the year check inside the service the only thing that answers
     * {@code 404 ACADEMIC_YEAR_NOT_FOUND} here, where on #12 and #13 gate 4 answers it first.
     *
     * <p><b>A year with no classes is an empty page</b>, never a 404. An unknown year <i>is</i> a
     * 404: those are different answers.
     *
     * <p>Read-only, so no {@code @Transactional}.
     *
     * <pre>
     * 400 INVALID_PAGE             page is negative
     * 400 INVALID_PAGE_SIZE        size is below 1 or above 100 — refused, never clamped
     * 400 INVALID_SORT_FIELD       sort names something not on the allow-list
     * 400 INVALID_SORT_DIRECTION   neither asc nor desc
     * 404 ACADEMIC_YEAR_NOT_FOUND  the {year} in the path is not a year of this school
     * </pre>
     */
    @GetMapping
    public ResponseEntity<PageResponse<SchoolClassResponse>> list(
            @PathVariable String year,
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String affiliationProgrammeDocsId,
            @RequestParam(required = false) Boolean hasSections,
            @RequestParam(required = false) Boolean hasSubjects,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sort) {

        // Bound one at a time rather than through @ModelAttribute, so a value that is not a
        // boolean comes back through the type-mismatch handler naming the parameter and what it
        // accepts — the same reasoning as the two subscription lists.
        SchoolClassSearchRequest request = new SchoolClassSearchRequest(
                active, search, affiliationProgrammeDocsId, hasSections, hasSubjects,
                page, size, sort);

        return ResponseEntity.ok(schoolClassService.listClasses(year, request));
    }
}
