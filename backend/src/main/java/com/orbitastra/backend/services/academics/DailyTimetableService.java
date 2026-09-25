package com.orbitastra.backend.services.academics;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
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
import com.orbitastra.backend.dto.academics.timetable.request.TimetableCopyRequest;
import com.orbitastra.backend.dto.academics.timetable.request.TimetableEntryPatchRequest;
import com.orbitastra.backend.dto.academics.timetable.request.TimetableEntryReplaceRequest;
import com.orbitastra.backend.dto.academics.timetable.request.TimetableEntryRequest;
import com.orbitastra.backend.dto.academics.timetable.response.DailyTimetableDetailResponse;
import com.orbitastra.backend.dto.academics.timetable.response.DailyTimetableResponse;
import com.orbitastra.backend.dto.academics.timetable.response.DailyTimetableSummaryResponse;
import com.orbitastra.backend.dto.academics.timetable.response.SectionDayResponse;
import com.orbitastra.backend.dto.academics.timetable.response.SkippedDateResponse;
import com.orbitastra.backend.dto.academics.timetable.response.TeacherDayResponse;
import com.orbitastra.backend.dto.academics.timetable.response.TimetableCopyResponse;
import com.orbitastra.backend.dto.academics.timetable.response.TimetableCreateResponse;
import com.orbitastra.backend.dto.academics.timetable.response.TimetableEntryDetailResponse;
import com.orbitastra.backend.dto.academics.timetable.response.TimetableReplaceResponse;
import com.orbitastra.backend.models.academics.enums.TimetableSlotType;
import com.orbitastra.backend.models.academics.structure.SchoolClass;
import com.orbitastra.backend.models.academics.structure.embedded.ClassSection;
import com.orbitastra.backend.models.academics.timetable.DailyTimetable;
import com.orbitastra.backend.models.academics.timetable.embedded.TimetableEntry;
import com.orbitastra.backend.models.core.AcademicYear;
import com.orbitastra.backend.models.core.School;
import com.orbitastra.backend.models.people.staff.Staff;
import com.orbitastra.backend.repositories.academics.attendance.AttendanceSessionRepository;
import com.orbitastra.backend.repositories.academics.timetable.DailyTimetableRepository;
import com.orbitastra.backend.repositories.people.staff.StaffRepository;
import com.orbitastra.backend.services.academics.helper.TimetableHelper;
import com.orbitastra.backend.services.academics.utils.DailyTimetableServiceUtils;

import lombok.RequiredArgsConstructor;

/**
 * Where every child is meant to be, hour by hour — the endpoints in
 * {@code controllers/academics/timetable/README.md}. #1 to #10 are built except #11 and #12 —
 * that is #1, #2, #3, #4, #5, #6, #7, #8, #9 and #10.
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

    //! #5 ASKS ATTENDANCE ONE QUESTION, and only this. Removing a period an attendance session
    //! names would leave that session pointing at nothing, which is the dangling link open item 4
    //! is about. Nothing writes that collection yet, so the refusal cannot fire today - it is here
    //! so that it becomes live when attendance is built rather than being remembered then.
    private final AttendanceSessionRepository attendanceSessions;
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
    //! endpoint 6 — build one day from another -----------------------------------------

    /**
     * Endpoint #6 — build the day in the path from another day.
     *
     * <h2>What a school actually does</h2>
     *
     * <p>Nobody types five days. Monday is built once and Tuesday through Friday are copied from it
     * and then corrected. Without this, a week of a 400-period school is 2,000 periods typed by
     * hand — and the typing is where the mistakes come from.
     *
     * <h2>New ids for every copied period, always</h2>
     *
     * <p>They are different periods on a different date. Two days sharing an entry id would make
     * {@code AttendanceSession.timetableEntryId} ambiguous, which is the single thing generated ids
     * exist to prevent — so a copy generates rather than reuses, even though it is copying.
     *
     * <h2>The target is validated exactly like #1 builds a day</h2>
     *
     * <p>Its own year, its own holiday check. <b>Copying Monday onto a festival is refused</b>,
     * which is where this differs from #1: #1 <i>skips</i> a holiday inside a range because a range
     * is expected to contain one, while a copy names a single date and a caller who named a holiday
     * meant something else.
     *
     * <h2>Merging re-runs every check against the combined list</h2>
     *
     * <p>That is the whole risk of merging: a teacher free in Monday and free in Tuesday can be in
     * two places once Monday's periods are added to Tuesday's. So the checks run on the combined
     * list, not on what arrived.
     *
     * <p><b>No {@code version} is required, unlike #2.</b> A merge only ever <i>adds</i>, so it
     * cannot erase a period the caller never saw — and the save still carries {@code @Version}, so
     * a writer that got in between the read and the write is {@code 409 CONCURRENT_MODIFICATION}
     * rather than a lost edit. #2 needs the version because it removes; this one does not because
     * it does not.
     */
    @Transactional
    public TimetableCopyResponse copyTimetable(String academicYear, LocalDate date,
            TimetableCopyRequest request) {

        //! step 1 - who is asking. requireUsable, because this is a write.
        School school = currentSchool.requireUsable();

        //! step 2 - the year both days belong to
        AcademicYear year = utils.loadYearByName(school, academicYear);

        LocalDate sourceDate = request.sourceDate();

        //! step 3 - a day cannot be built from itself. Without a merge it is a no-op dressed as a
        //! write; with one it would duplicate every period of the day onto itself, and every
        //! duplicate would then clash with the original it came from.
        if (sourceDate.equals(date)) {
            throw ApiException.badRequest("SOURCE_IS_TARGET",
                    "sourceDate and the date in the path are both " + date + ". A day cannot be "
                            + "built from itself.");
        }

        //! step 4 - the filters have to make sense before anything is read. sectionNo alone is not
        //! a filter: "section A" is not one thing across a school, and matching every class's A
        //! would copy three classes where the caller meant one. The same pairing rule #10 applies.
        String classFilter = TextHelper.blankToNull(request.classDocsId());
        String sectionFilter = TextHelper.blankToNull(request.sectionNo());

        if (sectionFilter != null && classFilter == null) {
            throw ApiException.badRequest("SECTION_WITHOUT_CLASS",
                    "sectionNo '" + sectionFilter + "' needs a classDocsId beside it. A section "
                            + "belongs to a class, so 'A' on its own names one section in every "
                            + "class that has one.");
        }

        //! step 5 - the day being copied FROM. Read before anything is checked about the target,
        //! because a caller who named a source that does not exist has nothing to fix about the
        //! target.
        // TODO: read daily timetable
        DailyTimetable source = timetables.findBySchoolIdAndDate(school.getId(), sourceDate)
                .orElseThrow(() -> ApiException.notFound("TIMETABLE_NOT_FOUND",
                        "No timetable has been written for " + sourceDate + ", so there is nothing "
                                + "to copy from."));

        //! step 6 - and it has to be this year's. A class belongs to exactly ONE academic year, so
        //! last year's Monday names classDocsIds this year does not have - the copy would fail at
        //! the structure step with a message about a missing class rather than about the year.
        if (!year.getName().equals(source.getAcademicYear())) {
            throw ApiException.conflict("DATE_OUTSIDE_ACADEMIC_YEAR",
                    "The timetable for " + sourceDate + " belongs to '" + source.getAcademicYear()
                            + "', not '" + year.getName() + "'. A class belongs to one year, so a "
                            + "day can only be copied inside the year it was written in.");
        }

        //! step 7 - THE TARGET IS VALIDATED LIKE #1 BUILDS A DAY: its own year, its own holiday
        //! check.
        if (date.isBefore(year.getStartDate()) || date.isAfter(year.getEndDate())) {
            throw ApiException.conflict("DATE_OUTSIDE_ACADEMIC_YEAR",
                    "'" + year.getName() + "' runs from " + year.getStartDate() + " to "
                            + year.getEndDate() + ", so " + date + " is outside it. Pick a date "
                            + "inside the year, or name the year that date belongs to.");
        }

        //! A HOLIDAY IS REFUSED HERE, where #1 skips it. #1 takes a RANGE, and any range longer
        //! than about five days contains a weekly off - skipping is the only way ranges stay
        //! usable. A copy names ONE date, and a caller who named a festival meant a different day.
        String holiday = utils.holidayNameFor(year, date);
        if (holiday != null) {
            throw ApiException.conflict("NOT_A_WORKING_DAY",
                    date + " is " + holiday + " for this school, so no timetable was built for it. "
                            + "Copy onto a working day.");
        }

        //! step 8 - what the target already has, and whether that is allowed
        // TODO: read daily timetable
        DailyTimetable target = timetables.findBySchoolIdAndDate(school.getId(), date).orElse(null);
        boolean merge = request.mergeRequested();

        if (target != null && !merge) {
            throw ApiException.conflict("TIMETABLE_ALREADY_EXISTS",
                    "A timetable already exists for " + date + ". Send merge=true to add these "
                            + "periods to it, replace the whole day with #2, or pick an empty "
                            + "date.");
        }

        if (target != null && !year.getName().equals(target.getAcademicYear())) {
            throw ApiException.conflict("DATE_OUTSIDE_ACADEMIC_YEAR",
                    "The timetable for " + date + " belongs to '" + target.getAcademicYear()
                            + "', not '" + year.getName() + "'. Merge into it under the year it "
                            + "was written into.");
        }

        //! step 9 - which of the source's periods are being copied. FRESH IDS FOR EVERY ONE: they
        //! are different periods on a different date, and two days sharing an id would make
        //! AttendanceSession.timetableEntryId ambiguous.
        List<TimetableEntry> copied = new ArrayList<>();

        for (TimetableEntry one : (source.getEntries() == null ? List.<TimetableEntry>of()
                : source.getEntries())) {

            if (classFilter != null && !classFilter.equals(one.getClassDocsId())) {
                continue;
            }
            //! Case-insensitive, like every other reading of a sectionNo in this module - the
            //! stored spelling is the class's own, and a caller typing 'a' means section A.
            if (sectionFilter != null && !sectionFilter.equalsIgnoreCase(one.getSectionNo())) {
                continue;
            }

            copied.add(TimetableEntry.builder()
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

        //! step 10 - a copy that carried nothing is a refusal, not an empty success. A filter that
        //! matched no period means the caller named a class or a section the source day does not
        //! have, and a 201 saying "0 copied" reads as though something worked.
        if (copied.isEmpty()) {
            throw ApiException.conflict("NOTHING_TO_COPY",
                    "No period of " + sourceDate + " matches"
                            + (classFilter == null ? " — that day has no periods at all."
                                    : " class '" + classFilter + "'"
                                            + (sectionFilter == null ? "."
                                                    : " section '" + sectionFilter + "'.")));
        }

        //! step 11 - THE COMBINED LIST is what gets checked. That is the whole risk of merging: a
        //! teacher free in Monday and free in Tuesday can be in two places once Monday's periods
        //! are added to Tuesday's, and checking only what arrived would miss exactly that.
        List<TimetableEntry> kept = target == null || target.getEntries() == null
                ? List.of()
                : target.getEntries();

        List<TimetableEntry> combined = new ArrayList<>(kept.size() + copied.size());
        combined.addAll(kept);
        combined.addAll(copied);

        //! step 12 - the structure every period has to fit. Run even on a pure copy, because the
        //! source day may have been written before a section was retired or a subject dropped -
        //! copying it forward would carry a period the school no longer offers.
        utils.normaliseAgainstStructure(school, year, combined);

        //! step 13 - the rules that depend only on the periods themselves, on the COMBINED list
        helper.validateTimes(combined);
        helper.validateSlotFields(combined);
        helper.validatePeriodCodesUnique(combined);
        helper.validateNoSectionOverlap(combined);
        helper.validateNoTeacherOverlap(combined);
        helper.validateNoRoomOverlap(combined);

        //! step 14 - every teacher named has to still be this school's, in ONE query
        utils.requireTeachersExist(school, combined);

        //! step 15 - build the document, then save it. Two steps, like every write in this project.
        //!
        //! schoolId explicitly on a new one, like every write here - nothing validates a document
        //! on save, and one written without it is invisible to every tenant-scoped query.
        DailyTimetable toSave;
        if (target == null) {
            toSave = DailyTimetable.builder()
                    .schoolId(school.getId())
                    .academicYear(year.getName())
                    .date(date)
                    .entries(combined)
                    .build();
        } else {
            //! MERGING RE-SAVES THE WHOLE DOCUMENT, and @Version is what makes that safe: a writer
            //! that got in between step 8's read and this write is 409 CONCURRENT_MODIFICATION
            //! rather than a lost edit. No version is asked of the caller because a merge only
            //! ADDS - it cannot erase a period they never saw, which is what #2 needs one for.
            toSave = target;
            toSave.setEntries(combined);
        }

        // TODO: insert daily timetable
        DailyTimetable saved = timetables.save(toSave);

        //! step 16 - the answer: both dates, because a copy is about two days.
        return new TimetableCopyResponse(
                saved.getDate(),
                sourceDate,
                saved.getId(),
                saved.getVersion(),
                target != null,
                copied.size(),
                kept.size(),
                DailyTimetableResponse.of(saved),
                "Every copied period got a NEW timetableEntryId - they are different periods on a "
                        + "different date. Correct one with #4, or replace the day with #2. "
                        + NO_AUTHORIZATION_YET);
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

        //! step 3 - the day itself, with its three refusals. Read BEFORE anything is checked
        //! about the date, for the reason the method explains: the stored year is the authority,
        //! and the range only ever explains an absence.
        DailyTimetable stored = utils.loadDayOrExplain(school, year, date);

        List<TimetableEntry> entries = stored.getEntries() == null ? List.of() : stored.getEntries();

        //! step 4 - the periods, IN STORED ORDER, with the names behind their ids. Sorting by
        //! time looks obvious and is wrong HERE: periods of different sections run at the same
        //! hour, so "by time" is a tie with a hidden second key. #8 and #9 return ONE section's
        //! and ONE teacher's periods, where it is a total order, and they do sort.
        List<TimetableEntryDetailResponse> rows = utils.describeEntries(school, year, entries);

        //! step 5 - the same five counts a row of #10 carries, worked out in memory because the
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

        //! step 6 - the answer
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

    //! endpoint 8 — one section's day ------------------------------------------------

    /**
     * Endpoint #8 — one section's periods on one date. <b>What a child's parent opens.</b>
     *
     * <h2>Sorted by time, where #7 is not</h2>
     *
     * <p>#7 returns the whole school's day in stored order and says why: periods of different
     * sections run at the same hour, so "by time" there is a tie with a hidden second key.
     * <b>That objection does not apply to one section.</b> A section cannot be in two places at
     * once — {@code SECTION_PERIOD_OVERLAP} is refused on every write — so within one section
     * {@code startTime} is a <i>total</i> order, and it is the order a parent reads the day in.
     *
     * <h2>An empty answer is a 200</h2>
     *
     * <p>The date has a timetable and this section has nothing in it: a fact about the section, not
     * a missing document. The three 404s belong to the <b>day</b> and are the same three #7 gives.
     *
     * <h2>The class and the section are checked, so "nothing" and "wrong" never look alike</h2>
     *
     * <p>Returning an empty list for a mistyped {@code classDocsId} would leave a parent's app
     * unable to tell "no school today" from "I asked for the wrong child". The class is resolved in
     * the year, and the section has to be one the class holds.
     *
     * <p><b>A retired section still answers.</b> This is a read, and the rule this module states is
     * that no gate runs on one — a section retired in March must not make February's Tuesday
     * unreadable, because attendance taken against it has to stay explicable. That is why this does
     * not use {@code requireActiveSection}, which every write does.
     */
    public SectionDayResponse getSectionDay(String academicYear, LocalDate date, String classDocsId,
            String sectionNo) {

        //! step 1 - who is asking. `require`, not `requireUsable`: a suspended school still reads
        //! its own timetable.
        School school = currentSchool.require();

        //! step 2 - the year has to be one of this school's, but NOT the running one
        AcademicYear year = utils.loadYearByName(school, academicYear);

        //! step 3 - the class, resolved in the year. A mistyped id is a 404 here rather than an
        //! empty list below, so "nothing scheduled" and "wrong class" never look alike.
        SchoolClass schoolClass = utils.loadClassForYear(school, year.getName(), classDocsId);

        //! step 4 - the section, by the class's OWN spelling. Single-use, so it stays inline.
        //!
        //! NOT requireActiveSection, which every WRITE uses. A retired section's past still has to
        //! read back: no gate runs on a read, and attendance taken against February's Tuesday has
        //! to stay explicable in March.
        String wanted = sectionNo == null ? "" : sectionNo.trim();
        String canonical = null;
        for (ClassSection section : (schoolClass.getSections() == null
                ? List.<ClassSection>of() : schoolClass.getSections())) {
            if (section.getSectionNo() != null && section.getSectionNo().equalsIgnoreCase(wanted)) {
                canonical = section.getSectionNo();
                break;
            }
        }

        if (canonical == null) {
            throw ApiException.conflict("SECTION_NOT_IN_CLASS",
                    "'" + schoolClass.getName() + "' has no section '" + wanted + "'.");
        }

        //! step 5 - the day itself, with its three refusals
        DailyTimetable stored = utils.loadDayOrExplain(school, year, date);

        //! step 6 - this section's periods, EARLIEST FIRST. A section cannot be in two places at
        //! once, so startTime is a total order here - which is exactly what it is not in #7.
        List<TimetableEntry> mine = new ArrayList<>();
        for (TimetableEntry entry : (stored.getEntries() == null
                ? List.<TimetableEntry>of() : stored.getEntries())) {
            if (classDocsId.equals(entry.getClassDocsId())
                    && canonical.equalsIgnoreCase(entry.getSectionNo())) {
                mine.add(entry);
            }
        }
        mine.sort(Comparator.comparing(TimetableEntry::getStartTime));

        //! step 7 - the names behind the ids, in two queries
        List<TimetableEntryDetailResponse> rows = utils.describeEntries(school, year, mine);

        //! step 8 - what the day amounts to for this section
        int lessonCount = 0;
        Set<String> teachersSeen = new LinkedHashSet<>();
        for (TimetableEntry entry : mine) {
            if (entry.getSlotType() == TimetableSlotType.LESSON) {
                lessonCount++;
            }
            if (entry.getTeacherDocsId() != null) {
                teachersSeen.add(entry.getTeacherDocsId());
            }
        }

        //! step 9 - the answer
        return new SectionDayResponse(
                stored.getDate(),
                stored.getAcademicYear(),
                stored.getId(),
                schoolClass.getId(),
                schoolClass.getName(),
                canonical,
                mine.size(),
                lessonCount,
                teachersSeen.size(),
                rows,
                mine.isEmpty()
                        ? "This section has no periods on that date. The day exists — #7 shows what "
                                + "the rest of the school is doing. " + NO_AUTHORIZATION_YET
                        : "Periods are earliest first, which is a real order here: a section "
                                + "cannot be in two places at once. " + NO_AUTHORIZATION_YET);
    }

    //! endpoint 9 — one teacher's day -------------------------------------------------

    /**
     * Endpoint #9 — one teacher's periods on one date. <b>What a teacher's app opens.</b>
     *
     * <h2>A break they supervise is part of their day</h2>
     *
     * <p>Since 2026-09-17 a non-lesson may carry a {@code teacherDocsId} — somebody supervises
     * lunch, runs the assembly, takes the activity. Those periods are here, and they are why
     * {@code lessonCount} and {@code entryCount} differ: <b>a teacher with no lessons can still
     * have a working day</b>, and a teacher named on a break cannot also be teaching period 4.
     *
     * <h2>Sorted by time, for the reason #8 is</h2>
     *
     * <p>A teacher cannot be in two places at once — {@code TEACHER_PERIOD_OVERLAP} is refused on
     * every write — so within one person's day {@code startTime} is a total order.
     *
     * <h2>The person is checked, so "nothing" and "wrong" never look alike</h2>
     *
     * <p>An unknown {@code teacherDocsId} is {@code 404 TEACHER_NOT_FOUND} rather than an empty
     * day. An app that could not tell those apart would show a free morning to somebody whose id
     * it had got wrong.
     *
     * <p><b>It is not "who is free".</b> {@code firstStartTime} and {@code lastEndTime} are the
     * ends of what this person is committed to; the gaps between are not computed, and #12 is the
     * endpoint that answers coverage properly — against every member of staff rather than one.
     */
    public TeacherDayResponse getTeacherDay(String academicYear, LocalDate date,
            String teacherDocsId) {

        //! step 1 - who is asking
        School school = currentSchool.require();

        //! step 2 - the year has to be one of this school's
        AcademicYear year = utils.loadYearByName(school, academicYear);

        //! step 3 - the person has to be this school's. A mistyped id is a 404 here rather than an
        //! empty day below: an app that could not tell those apart would show a free morning to
        //! somebody whose id it had got wrong.
        // TODO: read staff
        Staff person = staff.findByIdAndSchoolId(teacherDocsId, school.getId())
                .orElseThrow(() -> ApiException.notFound("TEACHER_NOT_FOUND",
                        "No staff member with id '" + teacherDocsId + "' in this school."));

        //! step 4 - the day itself, with its three refusals
        DailyTimetable stored = utils.loadDayOrExplain(school, year, date);

        //! step 5 - their periods, EARLIEST FIRST. A BREAK THEY SUPERVISE COUNTS: it is part of
        //! their day and it is why they cannot also be teaching at that hour.
        List<TimetableEntry> mine = new ArrayList<>();
        for (TimetableEntry entry : (stored.getEntries() == null
                ? List.<TimetableEntry>of() : stored.getEntries())) {
            if (teacherDocsId.equals(entry.getTeacherDocsId())) {
                mine.add(entry);
            }
        }
        mine.sort(Comparator.comparing(TimetableEntry::getStartTime));

        //! step 6 - the names behind the ids, in two queries
        List<TimetableEntryDetailResponse> rows = utils.describeEntries(school, year, mine);

        //! step 7 - what the day amounts to for this person
        int lessonCount = 0;
        Set<List<String>> sectionsSeen = new LinkedHashSet<>();
        for (TimetableEntry entry : mine) {
            if (entry.getSlotType() == TimetableSlotType.LESSON) {
                lessonCount++;
            }
            //! PAIRED WITH THE CLASS - "A" of one class and "A" of another are two sections.
            sectionsSeen.add(List.of(String.valueOf(entry.getClassDocsId()),
                    String.valueOf(entry.getSectionNo())));
        }

        //! step 8 - the ends of the day. THE LIST IS ALREADY SORTED, so the first and last are the
        //! answer - and the last period's END is not the maximum of the starts.
        LocalTime firstStart = mine.isEmpty() ? null : mine.get(0).getStartTime();
        LocalTime lastEnd = null;
        for (TimetableEntry entry : mine) {
            if (lastEnd == null || entry.getEndTime().isAfter(lastEnd)) {
                lastEnd = entry.getEndTime();
            }
        }

        //! step 9 - the answer
        return new TeacherDayResponse(
                stored.getDate(),
                stored.getAcademicYear(),
                stored.getId(),
                person.getId(),
                person.getFullName(),
                mine.size(),
                lessonCount,
                sectionsSeen.size(),
                firstStart,
                lastEnd,
                rows,
                mine.isEmpty()
                        ? "This person has nothing on that date. The day exists — #7 shows what the "
                                + "school is doing. " + NO_AUTHORIZATION_YET
                        : "A break they supervise is in this list and counts against their day. "
                                + "Who is FREE to cover a period is #12, which is not built. "
                                + NO_AUTHORIZATION_YET);
    }

    //! endpoint 3 — add one period ---------------------------------------------------

    /**
     * Endpoint #3 — add one period to a day that already exists.
     *
     * <h2>A {@code $push}, never a re-save</h2>
     *
     * <p>A day is about 120 KB. Rewriting all of it to add one period would make every addition a
     * race with every other edit of that morning, and would overwrite periods the caller never
     * saw. The write touches the array and nothing else.
     *
     * <h2>Validated against the day as it is <i>now</i></h2>
     *
     * <p>The new period is checked against the stored ones — the same overlap, slot and structure
     * rules #1 applies — immediately before the write, because the read and the write are not
     * atomic together and a period can appear between them. <b>The period code is guarded in the
     * update itself</b>, which is the one conflict rule expressible without comparing times: a
     * stored {@code LocalTime} carries the day it was written, so Mongo cannot be asked whether
     * two periods overlap.
     *
     * <p>The residual race is narrow and real: two clerks adding <i>overlapping</i> periods with
     * <i>different</i> codes in the same instant would both be accepted. Closing it needs either a
     * version in the match — which open item 1 argues against, because it would also refuse two
     * clerks working on two sections — or times stored as something Mongo can compare.
     */
    @Transactional
    public TimetableEntryDetailResponse addEntry(String academicYear, LocalDate date,
            TimetableEntryRequest request) {

        //! step 1 - who is asking. requireUsable, because this is a write.
        School school = currentSchool.requireUsable();

        //! step 2 - the year, and the day, with its three refusals
        AcademicYear year = utils.loadYearByName(school, academicYear);
        DailyTimetable stored = utils.loadDayOrExplain(school, year, date);

        //! step 3 - the period as it will be saved, built before anything is checked so that what
        //! is validated is what gets stored - the order #1 settled on.
        TimetableEntry entry = utils.toEntry(request);

        //! step 4 - the structure it has to fit, which also normalises the section to the class's
        //! own spelling. Before the shape checks, for the reason the method explains.
        utils.normaliseAgainstStructure(school, year, List.of(entry));

        //! step 5 - THE COMBINED DAY is what gets checked. Checking the new period alone would
        //! miss the only thing worth checking: whether it fits beside the ones already there.
        List<TimetableEntry> combined = new ArrayList<>(stored.getEntries() == null
                ? List.of() : stored.getEntries());
        combined.add(entry);

        helper.validateTimes(combined);
        helper.validateSlotFields(combined);
        helper.validatePeriodCodesUnique(combined);
        helper.validateNoSectionOverlap(combined);
        helper.validateNoTeacherOverlap(combined);
        helper.validateNoRoomOverlap(combined);

        //! step 6 - the teacher has to be this school's
        utils.requireTeachersExist(school, List.of(entry));

        //! step 7 - the $push, guarded on the period code. 0 modified means the day went away
        //! between the read and the write, or somebody added this very code first.
        // TODO: update daily timetable (add one period)
        long modified = timetables.pushEntry(school.getId(), date, entry);

        if (modified == 0) {
            throw ApiException.conflict("PERIOD_CODE_TAKEN",
                    "Section " + entry.getSectionNo() + " already has a period '"
                            + entry.getPeriodCode() + "' on " + date + ", or the day was removed "
                            + "while this was being checked. Read it again with #7.");
        }

        //! step 8 - the period as it now stands, with the names behind its ids
        return utils.describeEntries(school, year, List.of(entry)).get(0);
    }

    //! endpoint 4 — correct one period ------------------------------------------------

    /**
     * Endpoint #4 — correct one period. <b>The substitution, and the write this module exists
     * for.</b>
     *
     * <h2>A teacher calls in sick at 07:40</h2>
     *
     * <p>Six periods need covering before 08:00. Each is one field of one period, so this is one
     * targeted {@code $set} through an array filter — not a re-save of 120 KB, and not a
     * replacement of the day.
     *
     * <h2>Exactly one document must match, and matched is not modified</h2>
     *
     * <p>The contract's rule 2, and the one thing to get right. Zero matched means the entry is
     * gone or the version moved — {@code 404} or {@code 409}, never a silent success. <b>Zero
     * <i>modified</i> means nothing of the sort</b>: a patch writing the value a field already
     * holds changes nothing and is a perfectly good no-op, so the count that decides is the
     * matched one.
     *
     * <h2>What it will not change</h2>
     *
     * <p>{@code classDocsId} and {@code sectionNo} — moving a period to another section is
     * deleting one and adding another. {@code slotType} — it decides which other fields are legal.
     * The id — it is what the write is aimed at.
     *
     * <h2>{@code ""} clears, absent leaves alone</h2>
     *
     * <p>A room is removed by sending {@code facilityResourceDocsId: ""}. There is no other way to
     * say it, and treating an absent key as a clear would empty a field every time somebody patched
     * a different one.
     */
    @Transactional
    public TimetableEntryDetailResponse patchEntry(String academicYear, LocalDate date,
            String entryId, TimetableEntryPatchRequest request) {

        //! step 1 - who is asking
        School school = currentSchool.requireUsable();

        //! step 2 - the year, and the day, with its three refusals
        AcademicYear year = utils.loadYearByName(school, academicYear);
        DailyTimetable stored = utils.loadDayOrExplain(school, year, date);

        //! step 3 - the period being corrected, and the rest of the day it has to keep fitting
        TimetableEntry before = null;
        List<TimetableEntry> others = new ArrayList<>();
        for (TimetableEntry one : (stored.getEntries() == null
                ? List.<TimetableEntry>of() : stored.getEntries())) {
            if (one.getId().equals(entryId)) {
                before = one;
            } else {
                others.add(one);
            }
        }

        if (before == null) {
            throw ApiException.notFound("TIMETABLE_ENTRY_NOT_FOUND",
                    "No period with id '" + entryId + "' in the timetable for " + date + ".");
        }

        //! step 4 - the version, when one was sent. Checked here for the message and again in the
        //! update's own match for the race - the same two-step #2 uses, and for the same reason.
        if (request.version() != null && !request.version().equals(stored.getVersion())) {
            throw ApiException.conflict("CONCURRENT_MODIFICATION",
                    "This day is at version " + stored.getVersion() + " and the correction was "
                            + "made against version " + request.version() + ". Somebody changed it "
                            + "first. Read it again with #7.");
        }

        //! step 5 - what the period WOULD become. Built as a whole entry so the same rules that
        //! validate a written day validate this one, rather than a second set that could drift.
        //!
        //! "" CLEARS, absent leaves alone. Nothing else can express "take the room away", and
        //! treating absent as a clear would empty a field every time somebody patched another.
        Map<String, Object> set = new LinkedHashMap<>();
        Set<String> unset = new LinkedHashSet<>();

        TimetableEntry after = TimetableEntry.builder()
                .id(before.getId())
                .classDocsId(before.getClassDocsId())
                .sectionNo(before.getSectionNo())
                .slotType(before.getSlotType())
                .periodCode(utils.pick(before.getPeriodCode(), request.periodCode(), set, unset,
                        "periodCode", false))
                .subjectCode(utils.pick(before.getSubjectCode(), request.subjectCode(), set, unset,
                        "subjectCode", true))
                .teacherDocsId(utils.pick(before.getTeacherDocsId(), request.teacherDocsId(), set, unset,
                        "teacherDocsId", true))
                .slotLabel(utils.pick(before.getSlotLabel(), request.slotLabel(), set, unset,
                        "slotLabel", true))
                .facilityResourceDocsId(utils.pick(before.getFacilityResourceDocsId(),
                        request.facilityResourceDocsId(), set, unset,
                        "facilityResourceDocsId", true))
                .startTime(before.getStartTime())
                .endTime(before.getEndTime())
                .build();

        if (request.startTime() != null) {
            after.setStartTime(request.startTime());
            set.put("startTime", request.startTime());
        }
        if (request.endTime() != null) {
            after.setEndTime(request.endTime());
            set.put("endTime", request.endTime());
        }

        if (set.isEmpty() && unset.isEmpty()) {
            throw ApiException.badRequest("NOTHING_TO_UPDATE",
                    "No field was sent to change. A correction has to say what it corrects.");
        }

        //! step 6 - the structure the corrected period has to fit. The subject rule does not relax
        //! because this is an edit: a section still only studies what it studies.
        utils.normaliseAgainstStructure(school, year, List.of(after));

        //! step 7 - THE WHOLE DAY AS IT WOULD BE. The corrected period is checked beside the
        //! others, which is the entire point of a substitution: the covering teacher must not
        //! already be somewhere else at that hour.
        List<TimetableEntry> combined = new ArrayList<>(others);
        combined.add(after);

        helper.validateTimes(combined);
        helper.validateSlotFields(combined);
        helper.validatePeriodCodesUnique(combined);
        helper.validateNoSectionOverlap(combined);
        helper.validateNoTeacherOverlap(combined);
        helper.validateNoRoomOverlap(combined);

        //! step 8 - the teacher has to be this school's
        utils.requireTeachersExist(school, List.of(after));

        //! step 9 - the targeted write. MATCHED, not modified: a patch writing the value a field
        //! already holds changes nothing and is still a success.
        // TODO: update daily timetable (correct one period)
        long matched = timetables.patchEntry(school.getId(), date, entryId, request.version(),
                set, unset);

        if (matched == 0) {
            //! The entry was there a moment ago, so either it has just been removed or the version
            //! moved under a caller who sent one. Both are somebody else having got there first.
            throw ApiException.conflict("CONCURRENT_MODIFICATION",
                    "The period could not be corrected: it was removed, or the day changed, while "
                            + "this correction was being checked. Read it again with #7.");
        }

        //! step 10 - the period as it now stands
        return utils.describeEntries(school, year, List.of(after)).get(0);
    }

    //! endpoint 5 — remove one period -------------------------------------------------

    /**
     * Endpoint #5 — remove one period with a {@code $pull} by {@code _id}.
     *
     * <h2>A 404 when it was not there, not an idempotent 204</h2>
     *
     * <p>A caller deleting a period that has already gone has a stale screen, and telling them it
     * worked would leave them believing they removed something somebody else had already dealt
     * with. The count that decides is the <b>modified</b> one — unlike #4 — because a {@code $pull}
     * that removes nothing modifies nothing.
     *
     * <h2>Refused when attendance names it</h2>
     *
     * <p>{@code AttendanceSession.timetableEntryId} is an optional link with no foreign key behind
     * it, so a {@code $pull} would leave any session naming that period pointing at nothing and
     * nothing would fail. Open item 4 proposed refusing instead, and that is what this does.
     *
     * <p><b>Nothing writes that collection yet</b>, so the refusal cannot fire through the API
     * today. It costs one query and becomes live the moment attendance is built.
     *
     * <h2>No version, by design</h2>
     *
     * <p>A {@code $pull} by id is position-independent and cannot lose a concurrent edit to another
     * period — open item 1's table says so. The removal still bumps the version, so #2 can still
     * tell that the day moved.
     */
    @Transactional
    public void removeEntry(String academicYear, LocalDate date, String entryId) {

        //! step 1 - who is asking
        School school = currentSchool.requireUsable();

        //! step 2 - the year, and the day, with its three refusals
        AcademicYear year = utils.loadYearByName(school, academicYear);
        DailyTimetable stored = utils.loadDayOrExplain(school, year, date);

        //! step 3 - the period has to be in this day. Checked before the $pull so that "it was not
        //! there" is a 404 with a message rather than a modified count of zero to interpret.
        boolean present = false;
        for (TimetableEntry one : (stored.getEntries() == null
                ? List.<TimetableEntry>of() : stored.getEntries())) {
            if (one.getId().equals(entryId)) {
                present = true;
                break;
            }
        }

        if (!present) {
            throw ApiException.notFound("TIMETABLE_ENTRY_NOT_FOUND",
                    "No period with id '" + entryId + "' in the timetable for " + date + ".");
        }

        //! step 4 - ATTENDANCE GETS A SAY. A session already taken against this period would be
        //! left naming nothing, and a dangling link is what this project refuses everywhere else.
        // TODO: read attendance sessions
        if (attendanceSessions.existsBySchoolIdAndTimetableEntryId(school.getId(), entryId)) {
            throw ApiException.conflict("ENTRY_STILL_REFERENCED",
                    "Attendance has been taken against this period, so removing it would leave "
                            + "that session pointing at nothing. Correct the session first.");
        }

        //! step 5 - the $pull. 0 modified means somebody removed it between step 3 and here.
        // TODO: update daily timetable (remove one period)
        long modified = timetables.pullEntry(school.getId(), date, entryId);

        if (modified == 0) {
            throw ApiException.conflict("CONCURRENT_MODIFICATION",
                    "The period was removed by somebody else while this was being checked.");
        }
    }
}
