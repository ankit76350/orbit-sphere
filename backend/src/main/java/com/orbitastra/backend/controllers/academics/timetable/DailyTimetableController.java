package com.orbitastra.backend.controllers.academics.timetable;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;
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
import com.orbitastra.backend.dto.academics.timetable.request.DailyTimetableCreateRequest;
import com.orbitastra.backend.dto.academics.timetable.request.DailyTimetableSearchRequest;
import com.orbitastra.backend.dto.academics.timetable.response.DailyTimetableDetailResponse;
import com.orbitastra.backend.dto.academics.timetable.response.DailyTimetableSummaryResponse;
import com.orbitastra.backend.dto.academics.timetable.response.TimetableCreateResponse;
import com.orbitastra.backend.models.core.School;
import com.orbitastra.backend.services.academics.DailyTimetableService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Where every child is meant to be, hour by hour. Endpoints #1 to #12 of the plan in this package's
 * README; #1, #7 and #10 are built.
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
 * <p><b>No {@code {date}} in the path on #1</b>, which is still a change from the plan. #1 writes
 * a <i>range</i> — {@code startDate} with an optional {@code endDate} — and a date in the path
 * beside a range in the body would be two sources for one fact. <b>#7 keeps it</b>, because one
 * date is the whole of what it is addressed by.
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
    /**
     * Endpoint #10 — one page of this year's days, date-wise.
     *
     * <p><b>Counts, not periods.</b> A row says how full a day is, how much of it is taught, how
     * many classes, sections and staff it takes — and nothing about what is in it. A full day is
     * about 120 KB, so a page of twenty carrying its periods would be two and a half megabytes
     * shipped to render twenty dates. <b>#7 is one call away</b> for a day in full.
     *
     * <p><b>{@code ?from=} and {@code ?to=} are independent.</b> Either alone is meaningful —
     * "everything from here on", "everything up to here" — and neither is required.
     *
     * <p><b>The other filters reach inside the periods</b>: {@code ?teacherDocsId=} gives one
     * person's working days, {@code ?facilityResourceDocsId=} one room's, and
     * {@code ?classDocsId=} with {@code ?sectionNo=} are matched as <b>one period</b> rather than
     * as two separate conditions.
     *
     * <p><b>No gate runs on a read</b>, and a year the school has ended still answers — reading
     * last year's Tuesday is how an attendance record taken against it is explained.
     */
    @GetMapping
    public ResponseEntity<PageResponse<DailyTimetableSummaryResponse>> list(
            @PathVariable String year,
            DailyTimetableSearchRequest request) {

        //! No gates at all. Looking at a timetable is not an action on the school, and gate 4 in
        //! particular must not run: a finished year is exactly the one a school reads back.
        return ResponseEntity.ok(dailyTimetableService.listTimetables(year, request));
    }

    /**
     * Endpoint #7 — the whole school's day on one date, with the names behind its ids.
     *
     * <p><b>Addressed by the date, not by a document id</b>, which is unusual in this project and
     * right here for one reason: a caller always knows the date and never knows the id. A teacher's
     * app asks what is on today; nothing asks what is on in document 67aa15….
     *
     * <p><b>The periods come back with names attached</b> — the class, the subject and the staff
     * member behind each id — resolved in two extra queries rather than one request per id. A name
     * that no longer exists is left out and the id stays, because a class deleted after a day was
     * written must not stop that day from being read.
     *
     * <p><b>A holiday answers {@code 404 NOT_A_WORKING_DAY} and names the holiday.</b> "There is no
     * timetable" and "the school was closed" are different facts, and only one of them is something
     * a school has to act on. A working day with nothing written is
     * {@code 404 TIMETABLE_NOT_FOUND}, which is the one it does.
     *
     * <p><b>A date outside {@code {year}} is {@code 409 DATE_OUTSIDE_ACADEMIC_YEAR}</b>, the same
     * refusal #1 gives — the caller's year and date disagree, which is a mistake in the question
     * rather than an absence in the answer.
     *
     * <p><b>No gate runs on a read.</b> A suspended school still reads its own timetable, and last
     * year's Tuesday still answers, because attendance taken against it has to stay explicable.
     */
    @GetMapping("/{date}")
    public ResponseEntity<DailyTimetableDetailResponse> getOne(
            @PathVariable String year,
            @PathVariable @DateTimeFormat(iso = ISO.DATE) LocalDate date) {

        //! No gates, for the reason #10 has none. Gate 4 in particular must not run here: the year
        //! a school has finished is exactly the one it reads back to explain an attendance record.
        return ResponseEntity.ok(dailyTimetableService.getTimetable(year, date));
    }
}
