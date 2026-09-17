package com.orbitastra.backend.controllers.academics.timetable;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.orbitastra.backend.common.access.ActionGate;
import com.orbitastra.backend.common.current.CurrentSchoolResolver;
import com.orbitastra.backend.dto.academics.timetable.request.DailyTimetableCreateRequest;
import com.orbitastra.backend.dto.academics.timetable.response.TimetableCreateResponse;
import com.orbitastra.backend.models.core.School;
import com.orbitastra.backend.services.academics.DailyTimetableService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Where every child is meant to be, hour by hour. Endpoints #1 to #12 of the plan in this package's
 * README; #1 is built.
 *
 * <p><b>{@code {year}} in the path, like every route in
 * {@link com.orbitastra.backend.controllers.academics.structure.SchoolClassController}</b> — since
 * 2026-09-17. It was absent before that, because the model's persistence contract said the year
 * must be <i>derived</i> from the date rather than trusted from the request.
 *
 * <p><b>Deriving it was wrong in practice.</b> A school holding two academic years whose ranges
 * both cover a September date had its timetable written into the year it had not chosen, and the
 * refusal it eventually saw named a year it had never mentioned. Every date resolves to
 * <i>something</i>, so "you asked for the wrong year" was not a sentence the endpoint could say.
 * Stating the year makes that mismatch a refusal — {@code 409 DATE_OUTSIDE_ACADEMIC_YEAR} — rather
 * than a quiet resolution.
 *
 * <p><b>It also removed a deviation.</b> With the year named in the URL, gate 4 runs here like
 * everywhere else instead of as a per-date refusal inside the service, and the service lost its
 * per-date year lookup with it.
 *
 * <p><b>No {@code {date}} in the path</b>, which is still a change from the plan. #1 writes a
 * <i>range</i> — {@code startDate} with an optional {@code endDate} — and a date in the path
 * beside a range in the body would be two sources for one fact.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/schools/current/academic-years/{year}/timetables")
public class DailyTimetableController {

    private final DailyTimetableService dailyTimetableService;
    private final CurrentSchoolResolver currentSchool;
    private final ActionGate gate;

    /**
     * Endpoint #1 — create a day's periods across one date or a range of them.
     *
     * <p><b>{@code startDate} alone writes one day.</b> Adding {@code endDate} writes every date
     * between them inclusive, each carrying the same set of periods — because a school does not
     * build one Tuesday, it builds a pattern and applies it to a term.
     *
     * <p><b>A holiday inside the range is skipped and named in the response</b>, not refused: any
     * range longer than about five days contains a weekly off, and refusing over one would make
     * ranges useless. **A date that already has a timetable refuses the whole request**, because
     * that means the caller is rebuilding something and half a range is worse than none.
     *
     * <p><b>A {@code LESSON} may only name a subject that section actually studies</b> — one
     * created without a {@code sectionNo} is class-wide, one created with it belongs to that
     * section alone.
     *
     * <p><b>Every date must fall inside {@code {year}}.</b> A range outside it is
     * {@code 409 DATE_OUTSIDE_ACADEMIC_YEAR} naming the year's own range — the refusal the old
     * date-derived version had no way to give.
     */
    @PostMapping
    public ResponseEntity<TimetableCreateResponse> create(
            @PathVariable String year,
            @Valid @RequestBody DailyTimetableCreateRequest request) {

        //! Gate 1 — is the school itself live ---------------------------------------------
        //! Gate 2 — is the school paying --------------------------------------------------
        //! Gate 4 — is this the year the school is running --------------------------------
        //! Gate 4 runs here now that the year is named in the URL. It was a per-date refusal in
        //! the service until 2026-09-17, when the year stopped being derived from the dates.
        School school = currentSchool.require();
        gate.requireActiveSchool(school);
        gate.requireUsableSubscription(school);
        gate.requireYearMarkedAsRunning(school, year);

        return ResponseEntity
                .status(201)
                .body(dailyTimetableService.createTimetables(year, request));
    }
}
