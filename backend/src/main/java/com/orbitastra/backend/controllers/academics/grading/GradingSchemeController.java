package com.orbitastra.backend.controllers.academics.grading;

import java.math.BigDecimal;
import java.net.URI;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.orbitastra.backend.common.access.ActionGate;
import com.orbitastra.backend.common.current.CurrentSchoolResolver;
import com.orbitastra.backend.common.web.PageResponse;
import com.orbitastra.backend.dto.academics.gradingscheme.request.GradingSchemeCreateRequest;
import com.orbitastra.backend.dto.academics.gradingscheme.request.GradingSchemeSearchRequest;
import com.orbitastra.backend.dto.academics.gradingscheme.request.GradingSchemeUpdateRequest;
import com.orbitastra.backend.dto.academics.gradingscheme.response.GradeResolutionResponse;
import com.orbitastra.backend.dto.academics.gradingscheme.response.GradingSchemeResponse;
import com.orbitastra.backend.dto.academics.gradingscheme.response.GradingSchemeSummaryResponse;
import com.orbitastra.backend.models.core.School;
import com.orbitastra.backend.services.academics.GradingSchemeService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * The school's grading rulebooks. Endpoints #1 to #9 of the plan in this package's README; #1, #3,
 * #4, #5, #6 and #7 are built.
 *
 * <p><b>No {@code {year}} in the path</b>, unlike every route in
 * {@link com.orbitastra.backend.controllers.academics.structure.AcademicTermController} and its
 * neighbour. A rulebook outlives a year: the same scheme grades 2026-2027 and 2027-2028, and a
 * report card from either must reprint identically years later. {@code schemeVersion} is what
 * moves when the rules move.
 *
 * <p><b>Which is why only two gates run here.</b> Gate 4 asks whether a named academic year is the
 * school's working one, and there is no year in these paths to ask it about — applying it would
 * mean inventing a year the request never mentioned. So this is the one academics surface that
 * still answers after {@code POST /academic-years/{name}/end}, and it has to: correcting a 2026
 * report card means reading the 2026 scheme.
 *
 * <p><b>A scheme is addressed by its document id</b>, which is what three places already store as
 * {@code gradingSchemeDocsId} — {@code ClassSubject}, {@code Exam} and {@code ReportCard}. The
 * {@code name + schemeVersion} pair is the unique key a <i>person</i> reads, not the one a URL
 * carries.
 *
 * <p><b>There is a {@code PATCH} — #3 — and it is mostly refused.</b> The plan said there would be
 * none at all, on the grounds that every field is either half the key ({@code name},
 * {@code schemeVersion}), a reinterpretation of every band beneath it ({@code scaleType},
 * {@code maximumValue}), the history itself ({@code gradeBands}), or an event with its own
 * endpoints ({@code active}). That was right about a scheme something has <i>used</i> and wrong
 * about one nothing has, so the rule moved from the field to the state: <b>nothing references it
 * and every field is editable; something does and the answer is {@code 409}</b>, whatever the body
 * says. {@code active} is never on it either way — that is #4 and #5.
 *
 * <p>This paragraph said "there is no PATCH" until 2026-09-16, a day after #3 was built directly
 * beneath it.
 *
 * <p><b>There is no {@code DELETE}</b>, and that is an omission on purpose. Retiring is #4, for the
 * same reason nothing else here deletes: three collections store the id and none of those
 * references is a foreign key.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/schools/current/grading-schemes")
public class GradingSchemeController {

    private final GradingSchemeService gradingSchemeService;
    private final CurrentSchoolResolver currentSchool;
    private final ActionGate gate;

    /**
     * Endpoint #1 — create a rulebook and its bands in one write.
     *
     * <p>The only endpoint that creates a scheme from nothing. #2 creates one from another, which
     * is what an edit is here: moving a boundary in place would rewrite every report card ever
     * issued under it.
     *
     * <p><b>A {@code warning} may ride on the 201.</b> Bands that leave part of the scale ungraded
     * are reported rather than refused — a gap a school left on purpose is indistinguishable from
     * one it did not mean. An <i>overlap</i> is refused, because that one is a coin toss rather
     * than a hole.
     */
    @PostMapping
    public ResponseEntity<GradingSchemeResponse> create(
            @Valid @RequestBody GradingSchemeCreateRequest request) {

        //! Gate 1 — is the school itself live ---------------------------------------------
        //! Gate 2 — is the school paying --------------------------------------------------
        //! No gate 4: there is no academic year in this path to ask it about.
        School school = currentSchool.require();
        gate.requireActiveSchool(school);
        gate.requireUsableSubscription(school);

        GradingSchemeResponse response = gradingSchemeService.createScheme(request);

        //! The id, not the name+version pair. Every other endpoint in this module is
        //! /grading-schemes/{id}, so a Location built from the key would name a URL no route
        //! answers - the bug #1 of the term module shipped with and had to fix.
        return ResponseEntity
                .created(URI.create("/schools/current/grading-schemes/"
                        + response.gradingSchemeDocsId()))
                .body(response);
    }

    /**
     * Endpoint #4 — stop offering this scheme for new work.
     *
     * <p><b>Idempotent, and takes no body.</b> Already retired is a {@code 200} saying so.
     *
     * <p><b>It does not stop the scheme resolving.</b> #7 and #8 answer for a retired version,
     * because a report card issued under it has to reprint through its rules forever.
     *
     * <p><b>No reference check</b>, unlike #3 — retiring a scheme everything uses is exactly what
     * a school does when it publishes the next version.
     */
    @PostMapping("/{id}/deactivate")
    public ResponseEntity<GradingSchemeResponse> deactivate(@PathVariable String id) {
        //! Gate 1 — is the school itself live ---------------------------------------------
        //! Gate 2 — is the school paying --------------------------------------------------
        //! No gate 4: there is no academic year in this path to ask it about.
        School school = currentSchool.require();
        gate.requireActiveSchool(school);
        gate.requireUsableSubscription(school);

        return ResponseEntity.ok(gradingSchemeService.deactivateScheme(id));
    }

    /**
     * Endpoint #5 — offer this scheme for new work again.
     *
     * <p><b>Idempotent, and takes no body.</b> The pair exists because there is no {@code DELETE}:
     * a school that retired the wrong version needs a way back that is not a third version.
     */
    @PostMapping("/{id}/reactivate")
    public ResponseEntity<GradingSchemeResponse> reactivate(@PathVariable String id) {
        //! Gate 1 — is the school itself live ---------------------------------------------
        //! Gate 2 — is the school paying --------------------------------------------------
        School school = currentSchool.require();
        gate.requireActiveSchool(school);
        gate.requireUsableSubscription(school);

        return ResponseEntity.ok(gradingSchemeService.reactivateScheme(id));
    }

    /**
     * Endpoint #3 — change any field of a scheme, while nothing references it.
     *
     * <p><b>The plan said there would be no {@code PATCH} here at all.</b> It was right about a
     * scheme something has used and wrong about one nothing has — so the rule moved from the field
     * to the state: nothing references it and everything is editable, something does and only
     * {@code active} is.
     *
     * <p><b>The same two gates as #1.</b> Editing a rulebook is a write.
     *
     * <p><b>A {@code warning} may ride on the 200</b>, recomputed from what is now stored — the
     * same gap note #1 and #7 return.
     */
    @PatchMapping("/{id}")
    public ResponseEntity<GradingSchemeResponse> update(
            @PathVariable String id,
            @Valid @RequestBody GradingSchemeUpdateRequest request) {

        //! Gate 1 — is the school itself live ---------------------------------------------
        //! Gate 2 — is the school paying --------------------------------------------------
        //! No gate 4: there is no academic year in this path to ask it about.
        School school = currentSchool.require();
        gate.requireActiveSchool(school);
        gate.requireUsableSubscription(school);

        return ResponseEntity.ok(gradingSchemeService.updateScheme(id, request));
    }

    /**
     * Endpoint #7 — one scheme with every band, in stored order.
     *
     * <p><b>Addressed by the document id</b>, which is what three places already store as
     * {@code gradingSchemeDocsId} — {@code ClassSubject}, {@code Exam} and {@code ReportCard}. A
     * caller holding one of those has exactly what this endpoint needs.
     *
     * <p><b>The only endpoint that returns the bands.</b> #6 trims them to a count.
     *
     * <p><b>No gate runs on a read</b>, and a retired scheme answers: a report card issued in 2026
     * reprints through the 2026 rules long after the school moved on.
     */
    @GetMapping("/{id}")
    public ResponseEntity<GradingSchemeResponse> getOne(@PathVariable String id) {
        return ResponseEntity.ok(gradingSchemeService.getScheme(id));
    }

    /**
     * Endpoint #8 — turn a mark into a grade.
     *
     * <p><b>The whole purpose of the module</b>, and the only endpoint that exercises a band set
     * on read. Everything #1 enforces on write was unproven until this existed.
     *
     * <p><b>A {@code GET} with the value in the query string</b>, because it writes nothing: it is
     * arithmetic over one document. A resolved grade is stored by whatever records the mark.
     *
     * <p><b>Three refusals, in order.</b> {@code 409 SCHEME_NOT_RESOLVABLE_BY_VALUE} for a
     * {@code DESCRIPTOR} scheme, which has no arithmetic to do; {@code 400 VALUE_OUTSIDE_SCALE}
     * for a value this scheme could never produce; {@code 404 GRADE_NOT_RESOLVABLE} when the
     * scale has a hole there, naming the bands on either side.
     *
     * <p><b>No gate runs on a read</b>, and a retired scheme resolves — reprinting a 2026 report
     * card means reading the 2026 rules.
     */
    @GetMapping("/{id}/resolve")
    public ResponseEntity<GradeResolutionResponse> resolve(
            @PathVariable String id,
            @RequestParam BigDecimal value) {

        return ResponseEntity.ok(gradingSchemeService.resolveGrade(id, value));
    }

    /**
     * Endpoint #6 — one page of the school's schemes, filtered.
     *
     * <p><b>The filters bind from the query string as a record</b>, so adding one is a field
     * rather than another parameter on this signature. Spring builds it from
     * {@code ?active=&scaleType=&search=&page=&size=&sort=}.
     *
     * <p><b>No gate runs on a read.</b> A suspended or closed school still reads its own grading
     * rules — the same rule every read in this project follows.
     */
    @GetMapping
    public ResponseEntity<PageResponse<GradingSchemeSummaryResponse>> list(
            GradingSchemeSearchRequest request) {

        return ResponseEntity.ok(gradingSchemeService.listSchemes(request));
    }
}
