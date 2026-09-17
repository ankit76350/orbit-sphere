package com.orbitastra.backend.services.academics;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.stream.Collectors;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bson.types.ObjectId;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.orbitastra.backend.common.current.CurrentSchoolResolver;
import com.orbitastra.backend.common.error.exception.ApiException;
import com.orbitastra.backend.common.text.TextHelper;
import com.orbitastra.backend.common.web.PageResponse;
import com.orbitastra.backend.dto.academics.timetable.request.DailyTimetableCreateRequest;
import com.orbitastra.backend.dto.academics.timetable.request.DailyTimetableReplaceRequest;
import com.orbitastra.backend.dto.academics.timetable.request.DailyTimetableSearchRequest;
import com.orbitastra.backend.dto.academics.timetable.request.TimetableEntryReplaceRequest;
import com.orbitastra.backend.dto.academics.timetable.response.DailyTimetableDetailResponse;
import com.orbitastra.backend.dto.academics.timetable.response.DailyTimetableResponse;
import com.orbitastra.backend.dto.academics.timetable.response.DailyTimetableSummaryResponse;
import com.orbitastra.backend.dto.academics.timetable.response.SkippedDateResponse;
import com.orbitastra.backend.dto.academics.timetable.response.TimetableCreateResponse;
import com.orbitastra.backend.dto.academics.timetable.response.TimetableEntryDetailResponse;
import com.orbitastra.backend.dto.academics.timetable.response.TimetableReplaceResponse;
import com.orbitastra.backend.models.academics.enums.TimetableSlotType;
import com.orbitastra.backend.models.academics.structure.SchoolClass;
import com.orbitastra.backend.models.academics.timetable.DailyTimetable;
import com.orbitastra.backend.models.academics.timetable.embedded.TimetableEntry;
import com.orbitastra.backend.models.core.AcademicYear;
import com.orbitastra.backend.models.core.School;
import com.orbitastra.backend.models.people.staff.Staff;
import com.orbitastra.backend.repositories.academics.schoolclass.SchoolClassRepository;
import com.orbitastra.backend.repositories.academics.timetable.DailyTimetableRepository;
import com.orbitastra.backend.repositories.people.staff.StaffRepository;
import com.orbitastra.backend.services.academics.helper.TimetableHelper;
import com.orbitastra.backend.services.academics.utils.DailyTimetableServiceUtils;

import lombok.RequiredArgsConstructor;

/**
 * Where every child is meant to be, hour by hour — the endpoints in
 * {@code controllers/academics/timetable/README.md}. #1, #2, #7 and #10 are built.
 *
 * <p><b>All three gates run in the controller</b>, as everywhere else in {@code academics}. That
 * is true because the academic year is named in the URL: until 2026-09-17 it was derived from a
 * date in the body, a range could span two years, and gate 4 had nothing to ask about before the
 * request had been read — so the check lived here as a refusal. Stating the year removed both the
 * deviation and the per-date year lookup that went with it.
 */
@Service
@RequiredArgsConstructor
public class DailyTimetableService {

    private final DailyTimetableRepository timetables;
    private final StaffRepository staff;

    //! #7 READS EVERY CLASS OF A DAY IN ONE QUERY, which is why this is here and not behind
    //! utils.loadClassForYear: that one fetches a single class and refuses when it is missing,
    //! and a read putting names beside ids wants neither.
    private final SchoolClassRepository schoolClasses;
    private final DailyTimetableServiceUtils utils;
    private final TimetableHelper helper;
    private final CurrentSchoolResolver currentSchool;

    /** Repeated on every response until permissions exist. Deliberately hard to miss. */
    private static final String NO_AUTHORIZATION_YET =
            "No authorization is enforced on this endpoint yet: any caller who can reach it can "
                    + "run it.";

    /** How many days one request may cover. A term is about 90 working days. */
    private static final int MAX_RANGE_DAYS = 120;

    /**
     * What {@code ?sort=} accepts on #10, keyed by the lower-cased name a caller types.
     *
     * <p>An allowlist rather than a pass-through, for the reason every list in this project has
     * one: an arbitrary field name reaching a Mongo sort is how a caller orders by something
     * unindexed and makes the database read every document to answer one page.
     *
     * <p><b>None of the counts is sortable.</b> They are not on the document — the aggregation
     * computes them <i>after</i> the page has been chosen, so ordering by one would mean counting
     * the whole year first. "The busiest day" is a different endpoint, not a sort.
     */
    private static final Map<String, String> SORTABLE_DAY_FIELDS = new LinkedHashMap<>();

    static {
        SORTABLE_DAY_FIELDS.put("date", "date");
        SORTABLE_DAY_FIELDS.put("createdat", "createdAt");
        SORTABLE_DAY_FIELDS.put("updatedat", "updatedAt");
    }

    /** The same set as a sentence, for the refusal to list. */
    private static final String SORTABLE_DAY_FIELD_NAMES =
            SORTABLE_DAY_FIELDS.values().stream().collect(Collectors.joining(", "));

    /**
     * The default order, and the tiebreaker on every other sort.
     *
     * <p><b>Ascending, and one field is enough.</b> A school reads a week forwards, and
     * {@code school_timetable_date_uniq} makes the date unique per school — so it is already a
     * total order and needs no tiebreaker. That is unusual here: most lists in this project need
     * two fields because their first choice can tie.
     */
    private static final Sort DAY_ORDER = Sort.by(Sort.Order.asc("date"));

    //! endpoint 1 — create a day, or a range of them ----------------------------------

    /**
     * Endpoint #1 — write one day's periods across one date or a range of them.
     *
     * <h2>The range is the point</h2>
     *
     * <p>A school does not build one Tuesday; it builds a pattern and applies it. {@code startDate}
     * alone writes one day; {@code endDate} too writes every date between them inclusive, each
     * carrying the same periods.
     *
     * <h2>Holidays are skipped, collisions refuse everything</h2>
     *
     * <p>Those two look inconsistent and are not. A holiday inside a range is <b>expected</b> —
     * any range longer than about five days contains a weekly off — so refusing the whole request
     * over one would make ranges useless, and the skipped dates are named in the response instead.
     * A date that <b>already has a timetable</b> is not expected: it means the caller is rebuilding
     * something, and writing half a range would leave a school unable to tell which days came from
     * which request. All of it or none of it, with every colliding date named.
     *
     * <p>A range in which <i>every</i> date is a holiday writes nothing, and that is a refusal —
     * there is no partial success to report.
     *
     * <h2>Validated once, written many times</h2>
     *
     * <p>The periods are identical for every date, so the shape rules — times, slot fields, period
     * codes, and the three overlap checks — are run <b>once</b> against the built entries rather
     * than once per date. What is genuinely per date is the academic year, the holiday check, and
     * the structure lookups, because a range can cross a year boundary and a class belongs to one
     * year.
     *
     * <h2>Entry ids are generated here</h2>
     *
     * <p>MongoDB does not generate {@code _id} for embedded documents — rule 1 of the persistence
     * contract — and <b>each date gets its own ids</b>: they are different periods on different
     * days, and sharing an id would make {@code AttendanceSession.timetableEntryId} ambiguous
     * across dates.
     */
    @Transactional
    public TimetableCreateResponse createTimetables(String academicYear,
            DailyTimetableCreateRequest request) {

        //! step 1 - who is asking
        School school = currentSchool.requireUsable();

        //! step 2 - the year this timetable belongs to, STATED rather than worked out from the
        //! dates. Read again here because the controller's gate 4 does not hand the document down;
        //! one extra read, against the alternative of a controller passing domain objects into a
        //! service, which nothing else in this project does.
        AcademicYear year = utils.loadYearByName(school, academicYear);

        //! step 3 - the range. An absent endDate means one day.
        LocalDate start = request.startDate();
        LocalDate end = request.resolvedEndDate();

        if (end.isBefore(start)) {
            throw ApiException.badRequest("INVALID_DATE_RANGE",
                    "endDate " + end + " is before startDate " + start + ".");
        }

        long days = start.datesUntil(end.plusDays(1)).count();
        if (days > MAX_RANGE_DAYS) {
            throw ApiException.badRequest("DATE_RANGE_TOO_LONG",
                    "This range covers " + days + " days. One request may cover at most "
                            + MAX_RANGE_DAYS + " — about a term of working days. Split it.");
        }

        //! step 4 - THE CHECK THE DERIVATION COULD NOT MAKE. Every date has to fall inside the
        //! year the caller named. Working the year out from the date meant every date resolved to
        //! SOMETHING, so "you asked for the wrong year" was not a sentence this endpoint could
        //! say; it wrote into whichever year happened to contain the date instead.
        //!
        //! Checked on the range's ENDS rather than every date, because a range is contiguous: if
        //! both ends are inside the year, everything between them is too.
        if (start.isBefore(year.getStartDate()) || end.isAfter(year.getEndDate())) {
            throw ApiException.conflict("DATE_OUTSIDE_ACADEMIC_YEAR",
                    "'" + year.getName() + "' runs from " + year.getStartDate() + " to "
                            + year.getEndDate() + ", so " + start
                            + (start.equals(end) ? "" : " to " + end)
                            + " is outside it. Pick dates inside the year, or name the year those "
                            + "dates belong to.");
        }

        List<LocalDate> dates = start.datesUntil(end.plusDays(1)).toList();

        //! step 5 - no date in the range may already have a timetable. ONE query for the whole
        //! range; fourteen existsBy... calls is the N+1 that makes a longer range slower for no
        //! reason a caller can see.
        // TODO: read daily timetables
        List<DailyTimetable> existing = timetables.findBySchoolIdAndDateIn(school.getId(), dates);

        if (!existing.isEmpty()) {
            List<String> taken = existing.stream()
                    .map(one -> String.valueOf(one.getDate()))
                    .sorted()
                    .toList();

            throw ApiException.conflict("TIMETABLE_ALREADY_EXISTS",
                    "A timetable already exists for " + String.join(", ", taken)
                            + ". Nothing was written: a range is created whole or not at all, so "
                            + "that a school can always tell which days came from which request.");
        }

        //! step 6 - build the entries ONCE, before any of the shape checks, so that what is
        //! validated is what will be stored rather than what was sent. The same order #1 of
        //! grading settled on for bands.
        //!
        //! The ids here are placeholders: each date gets its own set in step 12, because two dates
        //! sharing an entry id would make AttendanceSession.timetableEntryId ambiguous.
        List<TimetableEntry> shape = request.entries().stream()
                .map(utils::toEntry)
                .toList();

        //! step 7 - the structure every period has to fit: the class, the section, and a subject
        //! that section actually studies. It also NORMALISES the section to the class's own
        //! spelling, which is why it has to run before the shape checks below - see the method.
        //!
        //! ONCE, not once per date. The periods are identical for every date and a class belongs
        //! to one academic year, so the answer cannot differ between dates of the same year — and
        //! a range IS one year, because step 4 refuses anything outside it.
        utils.normaliseAgainstStructure(school, year, shape);

        //! step 8 - the rules that depend only on the periods themselves, run once for the whole
        //! range because every date carries the same set. Order matters: an inverted period
        //! checked for overlap reports a clash, which is true and names the wrong problem.
        helper.validateTimes(shape);
        helper.validateSlotFields(shape);
        helper.validatePeriodCodesUnique(shape);
        helper.validateNoSectionOverlap(shape);
        helper.validateNoTeacherOverlap(shape);
        helper.validateNoRoomOverlap(shape);

        //! step 9 - every teacher named has to be this school's, in ONE query.
        utils.requireTeachersExist(school, shape);

        //! step 10 - which dates are working days. One year now, so there is no per-date year
        //! lookup and no running check here: gate 4 asked that once, in the controller.
        List<LocalDate> workingDates = new ArrayList<>();
        List<SkippedDateResponse> skipped = new ArrayList<>();

        for (LocalDate date : dates) {
            //! A HOLIDAY IS SKIPPED, NOT REFUSED - see the class note. Nothing about the day of
            //! the week is consulted: a school that runs on Sunday is a normal school.
            String holiday = utils.holidayNameFor(year, date);
            if (holiday != null) {
                skipped.add(new SkippedDateResponse(date, "NOT_A_WORKING_DAY", holiday));
                continue;
            }
            workingDates.add(date);
        }

        //! step 11 - nothing to write means every date was a holiday. A refusal rather than an
        //! empty success: the caller asked for a timetable and has none, and a 201 saying "0
        //! created" reads as though something worked.
        if (workingDates.isEmpty()) {
            throw ApiException.conflict("NOT_A_WORKING_DAY",
                    "Every date from " + start + " to " + end + " is a holiday or weekly off for "
                            + "this school, so no timetable was written.");
        }

        //! step 12 - build a document per working date. Fresh entry ids for each: they are
        //! different periods on different days, and two dates sharing an id would make
        //! AttendanceSession.timetableEntryId ambiguous.
        //!
        //! schoolId explicitly, like every write in this project - nothing validates a document on
        //! save, and one written without it is invisible to every tenant-scoped query afterwards.
        List<DailyTimetable> toInsert = new ArrayList<>();

        for (LocalDate date : workingDates) {
            List<TimetableEntry> entries = new ArrayList<>(shape.size());
            for (TimetableEntry one : shape) {
                entries.add(TimetableEntry.builder()
                        .id(new ObjectId().toHexString())
                        .periodCode(one.getPeriodCode())
                        .classDocsId(one.getClassDocsId())
                        .sectionNo(one.getSectionNo())
                        .slotType(one.getSlotType())
                        .subjectCode(one.getSubjectCode())
                        .teacherDocsId(one.getTeacherDocsId())
                        .slotLabel(one.getSlotLabel())
                        .startTime(one.getStartTime())
                        .endTime(one.getEndTime())
                        .facilityResourceDocsId(one.getFacilityResourceDocsId())
                        .build());
            }

            toInsert.add(DailyTimetable.builder()
                    .schoolId(school.getId())
                    .academicYear(year.getName())
                    .date(date)
                    .entries(entries)
                    .build());
        }

        //! step 13 - insert them. One save for the range, inside the transaction opened above, so
        //! a refusal on the last date leaves none of the earlier ones behind.
        // TODO: insert daily timetables
        List<DailyTimetable> saved = timetables.saveAll(toInsert);

        //! step 14 - the answer. A summary rather than every period of every day: a fortnight of a
        //! four-hundred-period school is 4,000 entries the caller already holds. One date is the
        //! exception, because that is the common call and re-reading it would be a round trip for
        //! something the server had in its hand.
        List<LocalDate> createdDates = saved.stream().map(DailyTimetable::getDate).sorted().toList();

        return new TimetableCreateResponse(
                start,
                end,
                saved.size(),
                shape.size(),
                createdDates,
                skipped,
                saved.size() == 1 ? DailyTimetableResponse.of(saved.get(0)) : null,
                "Correct one period with #4 and read a day back with #7. Keep each period's "
                        + "timetableEntryId: it is what an attendance session stores and what "
                        + "every later edit addresses. " + NO_AUTHORIZATION_YET);
    }
    //! endpoint 2 — replace a whole day ------------------------------------------------

    /**
     * Endpoint #2 — replace every period of one day.
     *
     * <h2>The one full-document write, and the one to reach for last</h2>
     *
     * <p>Every other write in this module exists so that this one is not needed: #3 adds a period,
     * #4 corrects one, #5 removes one, #6 copies a day. This one overwrites <b>everything</b>,
     * including periods the caller may never have seen, and a period left out of the list is gone.
     * The module's plan puts it last and says it may never be needed; it is built because a school
     * that has typed a day wrongly in forty places wants one call, not forty.
     *
     * <h2>{@code version} is required, and it is what makes this safe</h2>
     *
     * <p>A targeted update touches one embedded entry and cannot lose somebody else's edit to
     * another. A replace can lose all of them. So the caller states which version of the day it is
     * replacing, and a day that has moved on is {@code 409 CONCURRENT_MODIFICATION} rather than a
     * silent overwrite of the other clerk's morning.
     *
     * <p><b>Checked twice, deliberately.</b> Once here against the document just read, which gives
     * the caller a message naming both versions; and once by the {@code @Version} field on the save
     * itself, which is what closes the window between this read and that write. The first is for
     * the person; the second is for the race.
     *
     * <h2>Ids are kept where they are sent and generated where they are not</h2>
     *
     * <p>That is what makes a replace survivable: a period the caller sends back with its id keeps
     * its identity, and an {@code AttendanceSession} already pointing at it still points at it. An
     * id that is not in the stored day is refused rather than accepted — one borrowed from another
     * date would make {@code timetableEntryId} ambiguous, which is the single thing generated ids
     * exist to prevent.
     *
     * <h2>What it does NOT check, and why</h2>
     *
     * <p><b>Not whether the date is now a holiday.</b> #1 skips holidays because it chooses its
     * dates; this endpoint is handed a date that already has a document, and the document's
     * existence is the fact. Refusing because a holiday was declared afterwards would leave a
     * school unable to correct a day it can no longer delete.
     *
     * <p><b>Not whether a removed period is referenced by attendance.</b> Open item 4 of the plan
     * is unsettled and there is no attendance repository yet, so the removed ids are <b>named in
     * the response</b> instead of being silently dropped. That is visibility, not enforcement, and
     * the plan should say so.
     */
    public TimetableReplaceResponse replaceTimetable(String academicYear, LocalDate date,
            DailyTimetableReplaceRequest request) {

        //! step 1 - who is asking. requireUsable, because this is a write.
        School school = currentSchool.requireUsable();

        //! step 2 - the year this day belongs to
        AcademicYear year = utils.loadYearByName(school, academicYear);

        //! step 3 - the day itself, read BEFORE anything is checked about the date, for the reason
        //! #7 does: the document carries the year it was written into, and that is the authority.
        //! A year whose dates were edited afterwards must not make a stored day unreachable.
        // TODO: read daily timetable
        DailyTimetable stored = timetables.findBySchoolIdAndDate(school.getId(), date)
                .orElseThrow(() -> ApiException.notFound("TIMETABLE_NOT_FOUND",
                        "No timetable has been written for " + date + " yet, so there is nothing "
                                + "to replace. Create it with #1."));

        //! step 4 - and it has to be this year's day
        if (!year.getName().equals(stored.getAcademicYear())) {
            throw ApiException.conflict("DATE_OUTSIDE_ACADEMIC_YEAR",
                    "The timetable for " + date + " belongs to '" + stored.getAcademicYear()
                            + "', not '" + year.getName() + "'. Replace it under the year it was "
                            + "written into.");
        }

        //! step 5 - THE VERSION, and this is the check the whole endpoint rests on. A replace
        //! overwrites entries the caller never saw, so a stale caller must be refused rather than
        //! allowed to erase somebody else's work.
        if (!request.version().equals(stored.getVersion())) {
            throw ApiException.conflict("CONCURRENT_MODIFICATION",
                    "This day is at version " + stored.getVersion() + " and the request replaces "
                            + "version " + request.version() + ". Somebody changed it first. Read "
                            + "it again with #7 and reapply the change - a replace sent against a "
                            + "stale version would erase whatever they did.");
        }

        //! step 6 - which ids the day currently has, so a sent id can be checked against them
        Set<String> existingIds = new LinkedHashSet<>();
        if (stored.getEntries() != null) {
            for (TimetableEntry entry : stored.getEntries()) {
                existingIds.add(entry.getId());
            }
        }

        //! step 7 - build the entries, keeping the ids that were sent back. Built BEFORE any of
        //! the shape checks, so that what is validated is what will be stored - the same order #1
        //! settled on.
        List<TimetableEntry> shape = new ArrayList<>(request.entries().size());
        Set<String> keptIds = new LinkedHashSet<>();
        int addedCount = 0;

        for (TimetableEntryReplaceRequest sent : request.entries()) {
            TimetableEntry entry = utils.toEntry(sent);
            String claimed = TextHelper.blankToNull(sent.timetableEntryId());

            if (claimed == null) {
                //! No id means a period being ADDED, and utils generated one for it.
                addedCount++;
            } else {
                //! AN ID SENT IS A CLAIM THAT THIS PERIOD ALREADY EXISTS HERE. One that is not in
                //! this day would be a borrowed id, and two days sharing one makes
                //! AttendanceSession.timetableEntryId ambiguous - the single thing the generated
                //! ids exist to prevent.
                if (!existingIds.contains(claimed)) {
                    throw ApiException.notFound("TIMETABLE_ENTRY_NOT_FOUND",
                            "No period with id '" + claimed + "' in the timetable for " + date
                                    + ". Leave timetableEntryId off for a period being added; send "
                                    + "it only for one that is already there.");
                }
                if (!keptIds.add(claimed)) {
                    throw ApiException.badRequest("DUPLICATE_TIMETABLE_ENTRY_ID",
                            "timetableEntryId '" + claimed + "' appears twice in this request. One "
                                    + "period cannot be in a day twice, and two periods cannot "
                                    + "share an id.");
                }
            }

            shape.add(entry);
        }

        //! step 8 - the structure every period has to fit, and the section normalised to the
        //! class's own spelling. Before the shape checks, for the reason the method explains.
        utils.normaliseAgainstStructure(school, year, shape);

        //! step 9 - the rules that depend only on the periods themselves. Order matters: an
        //! inverted period checked for overlap reports a clash, which is true and names the wrong
        //! problem.
        helper.validateTimes(shape);
        helper.validateSlotFields(shape);
        helper.validatePeriodCodesUnique(shape);
        helper.validateNoSectionOverlap(shape);
        helper.validateNoTeacherOverlap(shape);
        helper.validateNoRoomOverlap(shape);

        //! step 10 - every teacher named has to be this school's, in ONE query
        utils.requireTeachersExist(school, shape);

        //! step 11 - what is about to be lost. Worked out BEFORE the write, because afterwards the
        //! old list is gone - and this is the destructive half of the endpoint, so it is named
        //! rather than counted.
        List<String> removedEntryIds = new ArrayList<>();
        for (String id : existingIds) {
            if (!keptIds.contains(id)) {
                removedEntryIds.add(id);
            }
        }

        //! step 12 - replace the entries and save. Two steps, like every write in this project.
        //!
        //! THE SAVE CARRIES ITS OWN VERSION CHECK. `stored` was loaded at the version step 5
        //! approved, and @Version on the document makes the update match on it - so a writer that
        //! got in between that read and this write is an OptimisticLockingFailureException, which
        //! the global handler answers as 409 CONCURRENT_MODIFICATION. Step 5 is the good message;
        //! this is the guarantee.
        stored.setEntries(shape);

        // TODO: update daily timetable
        DailyTimetable saved = timetables.save(stored);

        //! step 13 - the answer: what the day is now, and what it cost.
        return new TimetableReplaceResponse(
                saved.getId(),
                saved.getVersion(),
                DailyTimetableResponse.of(saved),
                keptIds.size(),
                addedCount,
                removedEntryIds,
                (removedEntryIds.isEmpty()
                        ? "Nothing was removed. "
                        : removedEntryIds.size() + " period(s) no longer exist, and an attendance "
                                + "session naming one of them now points at nothing. ")
                        + "Send this response's version on the next replace. " + NO_AUTHORIZATION_YET);
    }
    //! endpoint 10 — a year's days, filtered ------------------------------------------

    /**
     * Endpoint #10 — one page of a year's timetables, as counts rather than periods.
     *
     * <h2>A list carries what a list needs</h2>
     *
     * <p>A full school day measures about 120 KB, so a page of twenty carrying its periods would
     * be two and a half megabytes shipped to render twenty dates. The five counts are worked out
     * by an aggregation and the {@code entries} array never leaves the database — the same call #6
     * of grading makes about bands and #7 of positions about holders. <b>#7 is one call away</b>
     * for the caller that wants a day in full.
     *
     * <h2>The filters reach inside the periods</h2>
     *
     * <p>"Which days does this teacher work" and "which days is the lab used" are what a list of
     * days is actually opened to ask, and both live in the embedded array. A class and a section
     * are matched as <b>one period</b> rather than as two conditions — see the repository, where
     * that becomes an {@code $elemMatch}.
     *
     * <h2>No gates, and a year that has ended still answers</h2>
     *
     * <p>Reading last year's Tuesday is how a school explains an attendance record taken against
     * it. The year in the path must exist; nothing asks whether it is running.
     */
    public PageResponse<DailyTimetableSummaryResponse> listTimetables(String academicYear,
            DailyTimetableSearchRequest request) {

        //! step 1 - the paging and the order, validated before anything is read. Cheap checks
        //! with no I/O behind them go first, so a malformed request costs no round trip.
        Pageable pageable = PageResponse.pageableOf(request.page(), request.size(), request.sort(),
                SORTABLE_DAY_FIELDS, SORTABLE_DAY_FIELD_NAMES, DAY_ORDER);

        //! step 2 - who is asking. `require`, not `requireUsable`: a suspended school still reads
        //! its own timetable.
        School school = currentSchool.require();

        //! step 3 - the year has to be one of this school's, but NOT the running one. Reading a
        //! finished year is exactly what explains an attendance record taken against it.
        AcademicYear year = utils.loadYearByName(school, academicYear);

        //! step 4 - one page, filtered, sorted and counted in the database
        // TODO: read daily timetables
        return PageResponse.from(
                timetables.search(school.getId(), year.getName(), request, pageable),
                //! Already the response type: the rows are an aggregation's output rather than
                //! documents, so there is nothing left to map.
                one -> one);
    }
    //! endpoint 7 — one day in full ---------------------------------------------------

    /**
     * Endpoint #7 — the whole school's day on one date, with the names behind its ids.
     *
     * <h2>The date is the key, and that is deliberate</h2>
     *
     * <p>Every other detail read in this project takes a document id. This one takes a date,
     * because <b>a caller always knows the date and never knows the id</b>: a teacher's app asks
     * what is on today, and an attendance record explains itself by the day it was taken on.
     * {@code school_timetable_date_uniq} is what makes the date sufficient.
     *
     * <h2>The stored year decides, not today's version of the year's range</h2>
     *
     * <p>The day is read <b>first</b>, and what it says about itself is what answers. Checking the
     * date against the year's range before reading looks tidier and was measured to be wrong: a
     * school that edits its year's dates afterwards — shrinking a range past a day already written
     * — made this endpoint refuse a date that #10 was still listing. <b>Two reads of one module
     * disagreeing about whether a day exists is worse than either answer.</b> The range is only
     * ever used to explain an absence.
     *
     * <h2>Three refusals, and they say different things</h2>
     *
     * <p><b>A date outside the year is 409, not 404.</b> The caller named a year and a date that do
     * not go together, which is a mistake in the question rather than an absence in the answer —
     * the same refusal #1 gives, for the same reason.
     *
     * <p><b>A holiday is 404 and says which holiday.</b> "There is no timetable" and "the school
     * was closed" are different facts and only one of them needs acting on. A screen that showed
     * an empty day for Independence Day would be telling a school it had forgotten something.
     *
     * <p><b>A working day with nothing written is 404 too</b>, and says only that. That is the one
     * a school acts on.
     *
     * <h2>The names cost three queries, not seventy</h2>
     *
     * <p>A period stores {@code classDocsId}, {@code subjectCode} and {@code teacherDocsId}, none
     * of which a person can read. Resolving them one at a time would be one request per id — about
     * seventy for a day of four hundred periods across twelve classes and sixty staff. The classes
     * come back in one query and the staff in another, and the subject names are already inside the
     * classes.
     *
     * <p><b>A name that cannot be found is left out rather than refused.</b> A class deleted or a
     * staff member removed after the day was written leaves the id intact and the name absent,
     * which is the truth. Last year's Tuesday has to keep answering.
     */
    public DailyTimetableDetailResponse getTimetable(String academicYear, LocalDate date) {

        //! step 1 - who is asking. `require`, not `requireUsable`: a suspended school still reads
        //! its own timetable, and a finished year is exactly the one read back to explain an
        //! attendance record taken against it.
        School school = currentSchool.require();

        //! step 2 - the year has to be one of this school's
        AcademicYear year = utils.loadYearByName(school, academicYear);

        //! step 3 - the day itself, read BEFORE anything is checked about the date. Keyed by
        //! school and date alone, because that is what school_timetable_date_uniq is.
        //!
        //! THE ORDER HERE IS THE WHOLE CORRECTNESS OF THIS ENDPOINT, and the obvious order is
        //! wrong. Checking the date against the year's range first, and only then reading, was
        //! measured to hide a day that really exists: a school may edit its year's dates
        //! afterwards, and a range shrunk past an already-written day made #7 answer 409 for a
        //! date #10 was still listing. Two reads of one module disagreeing about whether a day
        //! exists is worse than either answer.
        //!
        //! The document itself carries the year it was written into. That is the authority, and
        //! the range is only ever used below to explain an ABSENCE.
        // TODO: read daily timetable
        DailyTimetable stored = timetables.findBySchoolIdAndDate(school.getId(), date).orElse(null);

        //! step 4 - a day that exists has to be this year's. The stored academicYear decides, not
        //! whether today's version of the year's range happens to contain the date.
        if (stored != null && !year.getName().equals(stored.getAcademicYear())) {
            throw ApiException.conflict("DATE_OUTSIDE_ACADEMIC_YEAR",
                    "The timetable for " + date + " belongs to '" + stored.getAcademicYear()
                            + "', not '" + year.getName() + "'. Ask for it under the year it was "
                            + "written into.");
        }

        //! step 5 - nothing there, and WHY. Three answers, because they are three different facts
        //! and only one of them is something a school has to act on.
        if (stored == null) {
            //! A date this year never covered. 409 rather than 404: the caller's year and date
            //! disagree, which is a mistake in the question rather than an absence in the answer.
            if (date.isBefore(year.getStartDate()) || date.isAfter(year.getEndDate())) {
                throw ApiException.conflict("DATE_OUTSIDE_ACADEMIC_YEAR",
                        "'" + year.getName() + "' runs from " + year.getStartDate() + " to "
                                + year.getEndDate() + ", so " + date + " is outside it. Pick a "
                                + "date inside the year, or name the year that date belongs to.");
            }

            //! WHICH HOLIDAY, when it is one. "No timetable" and "the school was closed" are
            //! different facts, and a screen that could not tell them apart would be reporting a
            //! gap on Independence Day.
            String holiday = utils.holidayNameFor(year, date);
            if (holiday != null) {
                throw ApiException.notFound("NOT_A_WORKING_DAY",
                        date + " is " + holiday + " for this school, so no timetable was written "
                                + "for it. Nothing is missing.");
            }

            //! A working day with nothing on it. THIS is the one a school acts on.
            throw ApiException.notFound("TIMETABLE_NOT_FOUND",
                    "No timetable has been written for " + date + " yet.");
        }

        List<TimetableEntry> entries = stored.getEntries() == null ? List.of() : stored.getEntries();

        //! step 6 - the classes named anywhere on the day, in ONE query. A day of four hundred
        //! periods across twelve classes is one read, not four hundred - and the subject names are
        //! already inside what comes back, so they cost nothing more.
        Set<String> classIds = new LinkedHashSet<>();
        Set<String> teacherIds = new LinkedHashSet<>();
        for (TimetableEntry entry : entries) {
            if (entry.getClassDocsId() != null) {
                classIds.add(entry.getClassDocsId());
            }
            if (entry.getTeacherDocsId() != null) {
                teacherIds.add(entry.getTeacherDocsId());
            }
        }

        //! THE SCOPE HERE IS UNTESTABLE AND STAYS ANYWAY - measured 2026-09-17. Replacing this
        //! with findAllById(classIds) passes the whole suite, because no request can produce a day
        //! naming a class that is not this school's and this year's: #1 resolves every classDocsId
        //! through loadClassForYear before it writes, nothing changes a class's school or year
        //! afterwards, and there is no delete. The mutation is unreachable, not harmless - an
        //! unscoped read by id is how another tenant's class name reaches this response the first
        //! time any of those three facts stops being true.
        Map<String, SchoolClass> classes = new LinkedHashMap<>();
        if (!classIds.isEmpty()) {
            // TODO: read school classes
            for (SchoolClass one : schoolClasses.findBySchoolIdAndAcademicYearAndIdIn(
                    school.getId(), year.getName(), classIds)) {
                classes.put(one.getId(), one);
            }
        }

        //! step 7 - the staff named anywhere on the day, in ONE query. A break's supervisor counts:
        //! whoever is named is a person the screen has to be able to write out.
        Map<String, String> teacherNames = new LinkedHashMap<>();
        if (!teacherIds.isEmpty()) {
            // TODO: read staff
            for (Staff person : staff.findBySchoolIdAndIdIn(school.getId(), teacherIds)) {
                teacherNames.put(person.getId(), person.getFullName());
            }
        }

        //! step 8 - the periods, IN STORED ORDER. Sorting by time looks obvious and is wrong:
        //! periods of different sections run at the same hour, so "by time" is a tie with a hidden
        //! second key. Which grouping a screen wants is the screen's question.
        //!
        //! A name that is not found is left null rather than refused - a class deleted or a staff
        //! member removed after the day was written must not stop last Tuesday from answering.
        List<TimetableEntryDetailResponse> rows = new ArrayList<>(entries.size());
        for (TimetableEntry entry : entries) {
            SchoolClass schoolClass = classes.get(entry.getClassDocsId());

            rows.add(TimetableEntryDetailResponse.of(
                    entry,
                    schoolClass == null ? null : schoolClass.getName(),
                    helper.subjectNameFor(schoolClass, entry.getSubjectCode(), entry.getSectionNo()),
                    teacherNames.get(entry.getTeacherDocsId())));
        }

        //! step 9 - the same five counts a row of #10 carries, worked out in memory because the
        //! entries are already here. Repeated rather than assumed to be in hand: a caller that
        //! reached this day by a link never saw the list, and a heading that only appeared when
        //! arrived at from somewhere else would be a heading that is sometimes missing.
        int lessonCount = 0;
        Set<String> classesSeen = new LinkedHashSet<>();
        Set<List<String>> sectionsSeen = new LinkedHashSet<>();
        Set<String> teachersSeen = new LinkedHashSet<>();

        for (TimetableEntry entry : entries) {
            if (entry.getSlotType() == TimetableSlotType.LESSON) {
                lessonCount++;
            }
            classesSeen.add(entry.getClassDocsId());
            //! PAIRED WITH THE CLASS - "A" of one class and "A" of another are two sections, so
            //! the pair is the key rather than the sectionNo on its own.
            sectionsSeen.add(List.of(String.valueOf(entry.getClassDocsId()),
                    String.valueOf(entry.getSectionNo())));
            if (entry.getTeacherDocsId() != null) {
                teachersSeen.add(entry.getTeacherDocsId());
            }
        }

        //! step 10 - the answer
        return new DailyTimetableDetailResponse(
                stored.getId(),
                stored.getDate(),
                stored.getAcademicYear(),
                stored.getVersion(),
                entries.size(),
                lessonCount,
                classesSeen.size(),
                sectionsSeen.size(),
                teachersSeen.size(),
                rows,
                "Correct one period with #4, addressing it by its timetableEntryId. "
                        + NO_AUTHORIZATION_YET);
    }
}
