package com.orbitastra.backend.controllers.academics.timetable;

import org.springframework.http.ResponseEntity;
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
 * <p><b>No {@code {year}} in the path</b>, unlike every route in
 * {@link com.orbitastra.backend.controllers.academics.structure.SchoolClassController}. The model's
 * persistence contract requires the academic year to be <i>derived</i> from the date rather than
 * trusted from the request, so a year in the URL would be a second source for one fact and the
 * first request that disagreed with itself would have no right answer.
 *
 * <p><b>No {@code {date}} in the path either</b>, which is a change from the plan. #1 writes a
 * <i>range</i> — {@code startDate} with an optional {@code endDate} — so the dates live in the body
 * for the same reason the year does not live in the URL.
 *
 * <p><b>Only two gates run in the controller, and gate 4's absence is deliberate.</b> Gate 4 asks
 * whether a named academic year is the school's running one. Here the year is worked out from a
 * date inside the body, and a range may span two years — so there is nothing to ask about before
 * the service has read the request. The equivalent check runs per date in
 * {@link DailyTimetableService#createTimetables}, as a refusal rather than a gate, and this is the
 * one place in the project where that rule bends.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/schools/current/timetables")
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
     */
    @PostMapping
    public ResponseEntity<TimetableCreateResponse> create(
            @Valid @RequestBody DailyTimetableCreateRequest request) {

        //! Gate 1 — is the school itself live ---------------------------------------------
        //! Gate 2 — is the school paying --------------------------------------------------
        //! No gate 4 here: the year is derived from a date in the body and a range may span two
        //! of them, so the check runs per date in the service. See the class note.
        School school = currentSchool.require();
        gate.requireActiveSchool(school);
        gate.requireUsableSubscription(school);

        return ResponseEntity
                .status(201)
                .body(dailyTimetableService.createTimetables(request));
    }
}
