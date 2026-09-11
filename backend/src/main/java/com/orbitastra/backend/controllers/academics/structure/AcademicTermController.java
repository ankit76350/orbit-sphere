package com.orbitastra.backend.controllers.academics.structure;

import java.net.URI;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
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
import com.orbitastra.backend.dto.academics.academicterm.response.AcademicTermResponse;
import com.orbitastra.backend.models.core.School;
import com.orbitastra.backend.services.academics.AcademicTermService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * The reporting periods of one academic year. Endpoints #1 to #11 and #35 of the plan in this
 * package's README; #1 and #9 are built.
 *
 * <p><b>Its own controller, not {@link SchoolClassController}'s.</b> A term and a class are
 * independent documents with independent keys, and the {@code {year}} prefix is all they share —
 * one controller holding both would be 36 endpoints deep.
 *
 * <p>School surface, so the tenant comes from {@link CurrentSchoolResolver} and never from the
 * URL. There is no platform surface for terms: a term structure is a school's own calendar.
 *
 * <p><b>A term is addressed by {@code termCode}</b>, not by its document id — unlike a class.
 * The code is unique within the year and is what a person reads, while the id is what other
 * documents store. Both identify it; the URL uses the one a human can type.
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
