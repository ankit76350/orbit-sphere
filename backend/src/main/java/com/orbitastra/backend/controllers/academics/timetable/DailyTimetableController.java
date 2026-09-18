package com.orbitastra.backend.controllers.academics.timetable;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.orbitastra.backend.common.access.ActionGate;
import com.orbitastra.backend.common.current.CurrentSchoolResolver;
import com.orbitastra.backend.common.web.PageResponse;
import com.orbitastra.backend.dto.academics.timetable.request.DailyTimetableCreateRequest;
import com.orbitastra.backend.dto.academics.timetable.request.DailyTimetableReplaceRequest;
import com.orbitastra.backend.dto.academics.timetable.request.TimetableCopyRequest;
import com.orbitastra.backend.dto.academics.timetable.request.DailyTimetableSearchRequest;
import com.orbitastra.backend.dto.academics.timetable.response.DailyTimetableDetailResponse;
import com.orbitastra.backend.dto.academics.timetable.response.DailyTimetableSummaryResponse;
import com.orbitastra.backend.dto.academics.timetable.response.SectionDayResponse;
import com.orbitastra.backend.dto.academics.timetable.response.TeacherDayResponse;
import com.orbitastra.backend.dto.academics.timetable.response.TimetableCopyResponse;
import com.orbitastra.backend.dto.academics.timetable.response.TimetableCreateResponse;
import com.orbitastra.backend.dto.academics.timetable.response.TimetableReplaceResponse;
import com.orbitastra.backend.models.core.School;
import com.orbitastra.backend.services.academics.DailyTimetableService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Where every child is meant to be, hour by hour. Endpoints #1 to #12 of the plan in this package's
 * README; #1, #2, #6, #7, #8, #9 and #10 are built.
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
     * Endpoint #2 — replace every period of one day.
     *
     * <p><b>The one full-document write this module has, and the one to reach for last.</b> Every
     * other write exists so that it is not needed — #3 adds a period, #4 corrects one, #5 removes
     * one, #6 copies a day. This one overwrites everything, and a period left out of the list is
     * gone.
     *
     * <p><b>{@code version} is required in the body</b>, unlike on any other write here. A replace
     * overwrites entries the caller may never have seen, so a day that moved on since it was read
     * is {@code 409 CONCURRENT_MODIFICATION} rather than a silent overwrite of another clerk's
     * work. #7 is where the version comes from.
     *
     * <p><b>Send each surviving period's {@code timetableEntryId} back</b> and it keeps its
     * identity, so an attendance session pointing at it still does. Leave it off for a period being
     * added. Ids not sent back are dropped, and <b>the response names them</b>.
     *
     * <p><b>The date is in the path here, unlike on #1.</b> #1 writes a range and had to put its
     * dates in the body; a replace is one day by definition.
     *
     * <p><b>A date with no timetable is {@code 404 TIMETABLE_NOT_FOUND}</b>, not an upsert. Creating
     * a day is #1, and the two refusals — "this date already has one" and "this date has none" —
     * are what keep the pair honest.
     */
    @PutMapping("/{date}")
    public ResponseEntity<TimetableReplaceResponse> replace(
            @PathVariable String year,
            @PathVariable @DateTimeFormat(iso = ISO.DATE) LocalDate date,
            @Valid @RequestBody DailyTimetableReplaceRequest request) {

        //! Gate 1 — is the school itself live ---------------------------------------------
        //! Gate 2 — is the school paying --------------------------------------------------
        //! Gate 4 — is this the year the school is running --------------------------------
        //! Every write runs all three, and this is the most destructive write in the module.
        School school = currentSchool.require();
        gate.requireActiveSchool(school);
        gate.requireUsableSubscription(school);
        gate.requireYearMarkedAsRunning(school, year);

        return ResponseEntity.ok(dailyTimetableService.replaceTimetable(year, date, request));
    }

    /**
     * Endpoint #6 — build the day in the path from another day.
     *
     * <p><b>What a school actually does.</b> Nobody types five days: Monday is built once and
     * Tuesday through Friday are copied from it and then corrected. Without this, a week of a
     * 400-period school is 2,000 periods typed by hand.
     *
     * <p><b>The target date is in the path; the source is in the body.</b> The day being
     * <i>built</i> is what this endpoint acts on, so it is the address.
     *
     * <p><b>Every copied period gets a new {@code timetableEntryId}.</b> They are different periods
     * on a different date, and two days sharing an id would make an attendance session's link
     * ambiguous.
     *
     * <p><b>The target is validated the way #1 builds a day</b> — its own year, its own holiday
     * check. A holiday is <b>refused</b> here where #1 skips it: #1 takes a range and any range
     * longer than about five days contains a weekly off, while a copy names one date and a caller
     * who named a festival meant a different day.
     *
     * <p><b>Optional filters copy part of a day</b>: {@code classDocsId} for one class, with
     * {@code sectionNo} for one section — which is what a school adding a section mid-term wants.
     * {@code sectionNo} alone is {@code 400 SECTION_WITHOUT_CLASS}.
     *
     * <p><b>{@code 409 TIMETABLE_ALREADY_EXISTS} unless {@code merge} is true</b>, and merging
     * re-runs every conflict check against the <i>combined</i> list — because a teacher free in
     * both days separately can still be in two places once they are put together.
     */
    @PostMapping("/{date}/copy-from")
    public ResponseEntity<TimetableCopyResponse> copyFrom(
            @PathVariable String year,
            @PathVariable @DateTimeFormat(iso = ISO.DATE) LocalDate date,
            @Valid @RequestBody TimetableCopyRequest request) {

        //! Gate 1 — is the school itself live ---------------------------------------------
        //! Gate 2 — is the school paying --------------------------------------------------
        //! Gate 4 — is this the year the school is running --------------------------------
        School school = currentSchool.require();
        gate.requireActiveSchool(school);
        gate.requireUsableSubscription(school);
        gate.requireYearMarkedAsRunning(school, year);

        //! 201 when it built a day, 200 when it merged into one that already existed - the same
        //! reading every other write in this project uses, where the status says whether something
        //! came into being.
        TimetableCopyResponse answer = dailyTimetableService.copyTimetable(year, date, request);
        return ResponseEntity.status(answer.merged() ? 200 : 201).body(answer);
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

    /**
     * Endpoint #8 — one section's periods on one date. <b>What a child's parent opens.</b>
     *
     * <p><b>Earliest first, where #7 is in stored order.</b> #7 returns the whole school's day, and
     * periods of different sections run at the same hour, so "by time" there is a tie with a hidden
     * second key. A section cannot be in two places at once, so within one section
     * {@code startTime} is a real order — and it is the one a parent reads the day in.
     *
     * <p><b>An empty answer is a {@code 200}.</b> The date has a timetable and this section has
     * nothing in it: a fact about the section, not a missing document. The three 404s belong to the
     * <i>day</i> and are the same three #7 gives.
     *
     * <p><b>The class and the section are checked</b>, so "nothing scheduled" and "you asked for
     * the wrong section" never look alike — a mistyped {@code classDocsId} is
     * {@code 404 CLASS_NOT_FOUND} and a section the class does not hold is
     * {@code 409 SECTION_NOT_IN_CLASS}. <b>A retired section still answers</b>: no gate runs on a
     * read, and attendance taken against February's Tuesday has to stay explicable in March.
     *
     * <p><b>No gate.</b> Last year's Tuesday still answers.
     */
    @GetMapping("/{date}/sections/{classDocsId}/{sectionNo}")
    public ResponseEntity<SectionDayResponse> getSectionDay(
            @PathVariable String year,
            @PathVariable @DateTimeFormat(iso = ISO.DATE) LocalDate date,
            @PathVariable String classDocsId,
            @PathVariable String sectionNo) {

        //! No gates. A parent opening a child's day is not an action on the school.
        return ResponseEntity.ok(
                dailyTimetableService.getSectionDay(year, date, classDocsId, sectionNo));
    }

    /**
     * Endpoint #9 — one teacher's periods on one date. <b>What a teacher's app opens.</b>
     *
     * <p><b>A break they supervise is in the list.</b> Since 2026-09-17 a non-lesson may carry a
     * {@code teacherDocsId} — somebody supervises lunch, runs the assembly, takes the activity —
     * and those periods count against their day, which is why {@code lessonCount} and
     * {@code entryCount} differ. A teacher with no lessons can still have a working day.
     *
     * <p><b>Earliest first</b>, for the reason #8 is: a teacher cannot be in two places at once, so
     * {@code startTime} is a total order within one person's day.
     *
     * <p><b>An unknown id is {@code 404 TEACHER_NOT_FOUND}, not an empty day.</b> An app that could
     * not tell those apart would show a free morning to somebody whose id it had got wrong.
     *
     * <p><b>It is not "who is free".</b> {@code firstStartTime} and {@code lastEndTime} are the
     * ends of what this person is committed to; the gaps between are not computed here, and #12 is
     * the endpoint that answers coverage — against every member of staff rather than one.
     *
     * <p><b>No gate.</b> Last year's Tuesday still answers.
     */
    @GetMapping("/{date}/teachers/{teacherDocsId}")
    public ResponseEntity<TeacherDayResponse> getTeacherDay(
            @PathVariable String year,
            @PathVariable @DateTimeFormat(iso = ISO.DATE) LocalDate date,
            @PathVariable String teacherDocsId) {

        //! No gates. A teacher opening their own day is not an action on the school.
        return ResponseEntity.ok(dailyTimetableService.getTeacherDay(year, date, teacherDocsId));
    }
}
