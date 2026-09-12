package com.orbitastra.backend.controllers.academics.structure;

import java.net.URI;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.orbitastra.backend.common.access.ActionGate;
import com.orbitastra.backend.common.current.CurrentSchoolResolver;
import com.orbitastra.backend.common.web.PageResponse;
import com.orbitastra.backend.dto.academics.academicterm.request.AcademicTermCreateRequest;
import com.orbitastra.backend.dto.academics.academicterm.request.AcademicTermSearchRequest;
import com.orbitastra.backend.dto.academics.academicterm.request.AcademicTermUpdateRequest;
import com.orbitastra.backend.dto.academics.academicterm.response.AcademicTermResponse;
import com.orbitastra.backend.models.core.School;
import com.orbitastra.backend.services.academics.AcademicTermService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * The reporting periods of one academic year. Endpoints #1 to #11 and #35 of the plan in this
 * package's README; #1, #3, #5, #6 and #9 are built.
 *
 * <p><b>Its own controller, not {@link SchoolClassController}'s.</b> A term and a class are
 * independent documents with independent keys, and the {@code {year}} prefix is all they share —
 * one controller holding both would be 36 endpoints deep.
 *
 * <p>School surface, so the tenant comes from {@link CurrentSchoolResolver} and never from the
 * URL. There is no platform surface for terms: a term structure is a school's own calendar.
 *
 * <p><b>A term is addressed by its document id</b>, the same as a class. The module README
 * settled this on 2026-09-10 and the rule is not "prefer codes" — it is <i>use whatever other
 * collections already store</i>, and six documents across three modules store
 * {@code termDocsId}. A section and a subject are codes only because they are embedded and so
 * have no id to be referenced by.
 *
 * <p><b>Which is also what keeps {@code name} editable and {@code termCode} not.</b> Nothing
 * joins on either, but the code is what a school-facing report, export or saved filter names,
 * so moving it breaks those silently while the database stays perfectly consistent.
 *
 * <p>This javadoc said the opposite until 2026-09-12, and #1 built a {@code Location} header
 * from the code to match — a URL that no route answered.
 *
 * <p><b>There is no {@code DELETE}</b>: a term is deactivated (#7). Six documents across three
 * modules reference one by {@code termDocsId}, and "is this still used" is a query across three
 * modules rather than a foreign-key check.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/schools/current/academic-years/{year}/terms")
public class AcademicTermController {

    private final AcademicTermService academicTermService;
    private final CurrentSchoolResolver currentSchool;
    private final ActionGate gate;

    /**
     * Endpoint #1 — add one reporting period to the year.
     *
     * <p>The ordinary way a term is created once the year is running and somebody realises a
     * period is missing. #2 replaces the whole set, which is the year-setup call.
     */
    @PostMapping
    public ResponseEntity<AcademicTermResponse> create(
            @PathVariable String year,
            @Valid @RequestBody AcademicTermCreateRequest request) {

        //! Gate 1 — is the school itself live ---------------------------------------------
        //! Gate 2 — is the school paying --------------------------------------------------
        //! Gate 4 — is this the school's working year, whatever the calendar says ---------
        School school = currentSchool.require();
        gate.requireActiveSchool(school);
        gate.requireUsableSubscription(school);
        gate.requireYearMarkedAsRunning(school, year);

        AcademicTermResponse response = academicTermService.createTerm(year, request);
        return ResponseEntity
                .created(URI.create("/schools/current/academic-years/" + year + "/terms/"
                        + response.termCode()))
                .body(response);
    }

    /**
     * Endpoint #3 — fix one term's name, dates or weight.
     *
     * <p><b>Addressed by the document id</b>, like every write that follows it. Not by
     * {@code termCode}: the module README settled on 2026-09-10 that the rule is "use whatever
     * other collections already store", and six documents across three modules store
     * {@code termDocsId}.
     *
     * <p><b>The same three gates as #1 and #2.</b> Editing a term is a write on the year's
     * structure, so a school that is suspended, unpaid, or looking at a year it has already ended
     * does not get to make one.
     *
     * <p><b>A weight that breaks the year's total is accepted, with a {@code warning}.</b> The
     * refusal lives on #1 and #2, which can see enough to be sure; this one cannot, because
     * 20/80 → 30/70 passes through 110.
     */
    @PatchMapping("/{termId}")
    public ResponseEntity<AcademicTermResponse> update(
            @PathVariable String year,
            @PathVariable String termId,
            @Valid @RequestBody AcademicTermUpdateRequest request) {

        //! Gate 1 — is the school itself live ---------------------------------------------
        //! Gate 2 — is the school paying --------------------------------------------------
        //! Gate 4 — is this the school's working year, whatever the calendar says ---------
        School school = currentSchool.require();
        gate.requireActiveSchool(school);
        gate.requireUsableSubscription(school);
        gate.requireYearMarkedAsRunning(school, year);

        return ResponseEntity.ok(academicTermService.updateTerm(year, termId, request));
    }

    /**
     * Endpoint #5 — freeze this term's results while another is still being marked.
     *
     * <p><b>Idempotent, and takes no body.</b> Already locked is a {@code 200} saying so. A caller
     * that wants "make sure this is locked" should not have to read the state before daring to
     * ask.
     *
     * <p><b>The same three gates as every write here.</b> Gate 4 is the interesting one: a year
     * that has been ended cannot have its terms locked or unlocked at all, which is the freeze
     * this package's README warns about under "lock results before ending the year".
     */
    @PostMapping("/{termId}/results/lock")
    public ResponseEntity<AcademicTermResponse> lockResults(
            @PathVariable String year,
            @PathVariable String termId) {

        //! Gate 1 — is the school itself live ---------------------------------------------
        //! Gate 2 — is the school paying --------------------------------------------------
        //! Gate 4 — is this the school's working year, whatever the calendar says ---------
        School school = currentSchool.require();
        gate.requireActiveSchool(school);
        gate.requireUsableSubscription(school);
        gate.requireYearMarkedAsRunning(school, year);

        return ResponseEntity.ok(academicTermService.lockResults(year, termId));
    }

    /**
     * Endpoint #6 — reopen this term's results so a mark can be corrected.
     *
     * <p><b>Idempotent, and takes no body.</b>
     *
     * <p><b>Nothing is recorded about who unlocked, or why</b> — the same hole core's #27 carries.
     * It wants a reason on the request and an {@code AuditEvent}, which wants an audit writer.
     * See the service, and open item 7 in this package's README.
     */
    @PostMapping("/{termId}/results/unlock")
    public ResponseEntity<AcademicTermResponse> unlockResults(
            @PathVariable String year,
            @PathVariable String termId) {

        //! Gate 1 — is the school itself live ---------------------------------------------
        //! Gate 2 — is the school paying --------------------------------------------------
        //! Gate 4 — is this the school's working year, whatever the calendar says ---------
        School school = currentSchool.require();
        gate.requireActiveSchool(school);
        gate.requireUsableSubscription(school);
        gate.requireYearMarkedAsRunning(school, year);

        return ResponseEntity.ok(academicTermService.unlockResults(year, termId));
    }

    /**
     * Endpoint #9 — every term in the year, in {@code sequence} order.
     *
     * <p><b>The filters bind from the query string as a record</b>, so adding one is a field
     * rather than another parameter on this signature. Spring builds it from
     * {@code ?active=&search=&resultsLocked=&weighted=&coversDate=&page=&size=&sort=}.
     *
     * <p><b>No gate runs on a read.</b> A suspended or closed school still reads its own
     * calendar — the same rule every read in this module follows.
     */
    @GetMapping
    public ResponseEntity<PageResponse<AcademicTermResponse>> list(
            @PathVariable String year,
            AcademicTermSearchRequest request) {

        return ResponseEntity.ok(academicTermService.listTerms(year, request));
    }
}
