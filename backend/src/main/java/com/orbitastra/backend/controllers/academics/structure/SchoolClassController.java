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
import com.orbitastra.backend.dto.academics.schoolclass.request.SectionCreateRequest;
import com.orbitastra.backend.dto.academics.schoolclass.request.SubjectCreateRequest;
import com.orbitastra.backend.dto.academics.schoolclass.request.SubjectUpdateRequest;
import com.orbitastra.backend.common.web.PageResponse;
import com.orbitastra.backend.dto.academics.schoolclass.response.SchoolClassDetailResponse;
import com.orbitastra.backend.dto.academics.schoolclass.response.SchoolClassResponse;
import com.orbitastra.backend.dto.academics.schoolclass.response.SectionDetailResponse;
import com.orbitastra.backend.dto.academics.schoolclass.response.SectionListResponse;
import com.orbitastra.backend.dto.academics.schoolclass.response.SubjectListResponse;
import com.orbitastra.backend.models.core.School;
import com.orbitastra.backend.services.academics.SchoolClassService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * The classes taught in one academic year, and the sections and subjects inside them. Endpoints
 * #12 to #16, #22 to #27, #28 to #31 and #37 of the plan in this package's README; #12, #13,
 * #17, #22, #24, #28, #29, #30, #31 and #37 are built.
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
     * <p>The screen a school opens to see its own structure. Default order is {@code name}
     * ascending — a class carries no school-defined position, {@code displayOrder} having been
     * removed on 2026-09-11.
     *
     * <pre>
     * ?page=0&amp;size=20                    the first page, by name
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

    //! the sections inside a class — #17 to #21 ---------------------------------------

    /**
     * Endpoint #17 — adds one section to a class.
     *
     * <p><b>The endpoint the student module is waiting for.</b>
     * {@code StudentAcademicRecord} stores {@code sectionNo} as a plain string, so no student can
     * be placed anywhere until a section exists.
     *
     * <p><b>A section is embedded in its class</b>, so the document written is the class and the
     * response is the class's whole section list — the same shape every calendar endpoint in
     * {@code core} returns for a holiday.
     *
     * <p><b>{@code sectionNo} can never be changed.</b> Eight collections store it as a plain
     * string and a section has no id for them to reference instead, being embedded. There is no
     * rename endpoint and there must not be one.
     *
     * <p><b>Uniqueness is the service's job alone.</b> Mongo cannot make an array's contents
     * unique, so unlike a class name there is no index to fall back on. It is checked
     * case-insensitively: "A" and "a" in one class is a typo, not two sections.
     *
     * <pre>
     * 400 VALIDATION_FAILED        no sectionNo, one over 20 characters, or capacity below 1
     * 404 ACADEMIC_YEAR_NOT_FOUND  the {year} in the path is not a year of this school
     * 404 CLASS_NOT_FOUND          no class with that id in that year
     * 404 STAFF_NOT_FOUND          no such staff in this school — another school's real id too
     * 409 SECTION_ALREADY_EXISTS   that class already has a section with that number
     * </pre>
     */
    @PostMapping("/{id}/sections")
    public ResponseEntity<SectionListResponse> addSection(
            @PathVariable String year,
            @PathVariable String id,
            @Valid @RequestBody SectionCreateRequest request) {

        //! Gate 1 — is the school itself live ---------------------------------------------
        //! Gate 2 — is the school paying --------------------------------------------------
        //! Gate 4 — is this the school's working year, whatever the calendar says ---------
        School school = currentSchool.require();
        gate.requireActiveSchool(school);
        gate.requireUsableSubscription(school);
        gate.requireYearMarkedAsRunning(school, year);

        SectionListResponse response = schoolClassService.addSection(year, id, request);
        return ResponseEntity
                .created(URI.create("/schools/current/academic-years/" + year + "/classes/" + id
                        + "/sections"))
                .body(response);
    }

    /**
     * Endpoint #29 — one class in full, sections and subjects included.
     *
     * <p><b>One read, because they are embedded</b> — which is the whole reason they are
     * embedded. Were sections and subjects collections of their own this would be three queries,
     * and #28 would be three per row.
     *
     * <p><b>Four counts beside the two lists.</b> Active and total differ for both: a retired
     * section keeps its {@code sectionNo} and still appears, because records reference it, but it
     * is not one a student can be placed in.
     *
     * <p><b>Nothing is resolved to a name.</b> Class teachers, subject teachers and grading
     * schemes come back as raw ids — see {@code SectionView} for why that decision is made once
     * rather than in each response.
     *
     * <p><b>No gates.</b> A suspended school can read its own class and cannot change it.
     *
     * <pre>
     * 404 ACADEMIC_YEAR_NOT_FOUND  the {year} in the path is not a year of this school
     * 404 CLASS_NOT_FOUND          no class with that id in that year, or it is another school's
     * </pre>
     *
     * <p>Read-only, so no {@code @Transactional}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<SchoolClassDetailResponse> getOne(
            @PathVariable String year,
            @PathVariable String id) {

        return ResponseEntity.ok(schoolClassService.getClass(year, id));
    }

    /**
     * Endpoint #30 — just the sections, with capacity and class teacher.
     *
     * <p><b>What a "move this student" dropdown reads</b> instead of pulling the whole class. The
     * document read is identical to #29's — a section is embedded, so there is nothing cheaper to
     * read — but the response is a tenth of the size, and that is the part that crosses the
     * network.
     *
     * <pre>
     * ?active=true    only the sections a student can be placed in — what the dropdown sends
     * ?active=false   only the retired ones
     * (absent)        every section, retired included
     * </pre>
     *
     * <p><b>The counts describe the whole class, not the filtered view.</b>
     * {@code sectionCount} answers "how many does this class have", which does not change
     * because a caller asked to see some of them.
     *
     * <p><b>No gates</b>, same as #29.
     *
     * <pre>
     * 404 ACADEMIC_YEAR_NOT_FOUND  the {year} in the path is not a year of this school
     * 404 CLASS_NOT_FOUND          no class with that id in that year, or it is another school's
     * </pre>
     *
     * <p>A class with no sections is an <b>empty list</b>, never a 404 — and while #17 is the
     * only section endpoint built, that is the state most classes are in.
     *
     * <p>Read-only, so no {@code @Transactional}.
     */
    @GetMapping("/{id}/sections")
    public ResponseEntity<SectionListResponse> listSections(
            @PathVariable String year,
            @PathVariable String id,
            @RequestParam(required = false) Boolean active) {

        return ResponseEntity.ok(schoolClassService.listSections(year, id, active));
    }

    /**
     * Endpoint #31 — the class's subject assignments, or the ones one section studies.
     *
     * <p><b>{@code ?sectionNo=} names an audience, not a row.</b> Omitted or blank, the answer is
     * every assignment the class holds. Given, the answer is what that section is taught: its own
     * rows <i>and</i> the class-wide ones.
     *
     * <p><b>This is the one place {@code sectionNo} does not mean what it means on #24</b>, where
     * it is half of a row's key. Here it is the question "taught to whom".
     */
    @GetMapping("/{id}/subjects")
    public ResponseEntity<SubjectListResponse> listSubjects(
            @PathVariable String year,
            @PathVariable String id,
            @RequestParam(required = false) String sectionNo) {

        return ResponseEntity.ok(schoolClassService.listSubjects(year, id, sectionNo));
    }

    /**
     * Endpoint #37 — one section of one class.
     *
     * <p><b>{@code sectionNo} is a path segment here, not a query parameter</b>, because it names
     * the thing being fetched rather than filtering something else. Compare #30, where the
     * section list is the resource, and #31, where {@code ?sectionNo=} narrows an audience.
     *
     * <p><b>The section's subjects are not in this response</b> — that is #31 with
     * {@code ?sectionNo=}, which is the one place the class-wide union rule lives.
     */
    @GetMapping("/{id}/sections/{sectionNo}")
    public ResponseEntity<SectionDetailResponse> getSection(
            @PathVariable String year,
            @PathVariable String id,
            @PathVariable String sectionNo) {

        return ResponseEntity.ok(schoolClassService.getSection(year, id, sectionNo));
    }

    //! the subjects taught in a class — #22 to #27 ------------------------------------

    /**
     * Endpoint #22 — assigns a subject to a class, or to one section of it.
     *
     * <p><b>The row key is the pair {@code (subjectCode, sectionNo)}.</b> A class-wide assignment
     * leaves {@code sectionNo} out; a per-section one names it, and a named section has to be one
     * the class actually has.
     *
     * <p><b>A subject is class-wide OR per-section, never both.</b> The plan left this open and
     * this endpoint settles it, being the only one that can create the mixture: a section studies
     * its own rows <i>plus</i> the class's, so both would give one section the subject twice with
     * nothing to say which row's teacher wins. {@code 409 SUBJECT_ASSIGNMENT_CONFLICT} says which
     * way round it already is.
     *
     * <p><b>Every reference is checked</b> — the section, each teacher and the grading scheme —
     * and all three are checked against <i>this</i> school. {@code GradingSchemeRepository} was
     * built with this endpoint, the last of the three the module's README listed as missing.
     *
     * <p><b>{@code subjectCode} is normalised; {@code sectionNo} is not.</b> A subject has
     * {@code name} for display, which leaves its code free to be a code — "maths-2" is stored
     * {@code MATHS_2}. A section number is the display value as well as the reference.
     *
     * <pre>
     * 400 VALIDATION_FAILED           no code, no name, no type, or a field over its limit
     * 400 DUPLICATE_TEACHER           the same teacher twice in one list
     * 404 ACADEMIC_YEAR_NOT_FOUND     the {year} in the path is not a year of this school
     * 404 CLASS_NOT_FOUND             no class with that id in that year
     * 404 SECTION_NOT_FOUND           that class has no section with that number
     * 404 STAFF_NOT_FOUND             no such staff in this school — another school's real id too
     * 404 GRADING_SCHEME_NOT_FOUND    no such scheme in this school
     * 409 SUBJECT_CODE_INVALID        the code has no letter or digit in it
     * 409 SUBJECT_ALREADY_ASSIGNED    that exact (code, section) pair is already there
     * 409 SUBJECT_ASSIGNMENT_CONFLICT the subject is already assigned the other way round
     * </pre>
     */
    @PostMapping("/{id}/subjects")
    public ResponseEntity<SubjectListResponse> addSubject(
            @PathVariable String year,
            @PathVariable String id,
            @Valid @RequestBody SubjectCreateRequest request) {

        //! Gate 1 — is the school itself live ---------------------------------------------
        //! Gate 2 — is the school paying --------------------------------------------------
        //! Gate 4 — is this the school's working year, whatever the calendar says ---------
        School school = currentSchool.require();
        gate.requireActiveSchool(school);
        gate.requireUsableSubscription(school);
        gate.requireYearMarkedAsRunning(school, year);

        SubjectListResponse response = schoolClassService.addSubject(year, id, request);
        return ResponseEntity
                .created(URI.create("/schools/current/academic-years/" + year + "/classes/" + id
                        + "/subjects"))
                .body(response);
    }

    /**
     * Endpoint #24 — change one assignment's name, short name, type or grading scheme.
     *
     * <p><b>The pair is the address.</b> {@code subjectCode} is in the path and {@code sectionNo}
     * is a query parameter, because one of the two is usually absent: a class-wide subject has no
     * section, and asking for that ordinary case should not mean sending an empty segment.
     * {@code ?sectionNo=} blank is read the same as leaving it off.
     *
     * <p><b>Neither half of the key can be changed here</b> — see the request record for why a
     * move is #26 plus #22 rather than an edit.
     */
    @PatchMapping("/{id}/subjects/{subjectCode}")
    public ResponseEntity<SubjectListResponse> updateSubject(
            @PathVariable String year,
            @PathVariable String id,
            @PathVariable String subjectCode,
            @RequestParam(required = false) String sectionNo,
            @Valid @RequestBody SubjectUpdateRequest request) {

        //! Gate 1 — is the school itself live ---------------------------------------------
        //! Gate 2 — is the school paying --------------------------------------------------
        //! Gate 4 — is this the school's working year, whatever the calendar says ---------
        School school = currentSchool.require();
        gate.requireActiveSchool(school);
        gate.requireUsableSubscription(school);
        gate.requireYearMarkedAsRunning(school, year);

        return ResponseEntity.ok(
                schoolClassService.updateSubject(year, id, subjectCode, sectionNo, request));
    }
}
