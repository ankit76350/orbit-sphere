package com.orbitastra.backend.services.core;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.orbitastra.backend.common.audit.AuditTrail;
import com.orbitastra.backend.common.current.CurrentSchoolResolver;
import com.orbitastra.backend.common.error.exception.ApiException;
import com.orbitastra.backend.dto.core.academicyear.AcademicYearCreateRequest;
import com.orbitastra.backend.dto.core.academicyear.AcademicYearDatesRequest;
import com.orbitastra.backend.dto.core.academicyear.AcademicYearResponse;
import com.orbitastra.backend.dto.core.academicyear.GenerateWeeklyOffRequest;
import com.orbitastra.backend.dto.core.academicyear.HolidayCalendarResponse;
import com.orbitastra.backend.dto.core.academicyear.HolidayRequest;
import com.orbitastra.backend.dto.core.academicyear.HolidayUpdateRequest;
import com.orbitastra.backend.dto.core.academicyear.ResultsUnlockRequest;
import com.orbitastra.backend.dto.core.academicyear.WeeklyOffGenerateResponse;
import com.orbitastra.backend.models.audit.AuditEvent;
import com.orbitastra.backend.models.audit.embedded.AuditFieldChange;
import com.orbitastra.backend.models.audit.enums.AuditEventType;
import com.orbitastra.backend.models.audit.enums.AuditOutcome;
import com.orbitastra.backend.models.core.AcademicYear;
import com.orbitastra.backend.models.core.School;
import com.orbitastra.backend.models.core.embedded.HolidayDetail;
import com.orbitastra.backend.models.core.embedded.HolidayEvent;
import com.orbitastra.backend.models.core.enums.HolidayType;
import com.orbitastra.backend.repositories.core.AcademicYearRepository;
import com.orbitastra.backend.services.core.helper.CoreValidator;
import com.orbitastra.backend.services.core.helper.TextHelper;

import lombok.RequiredArgsConstructor;

/**
 * The school's academic years. Endpoints #18 to #27, plus the two calendar {@code DELETE}s.
 *
 * <p>On the school surface, so the tenant comes from CurrentSchoolResolver and never from the
 * URL. A year is addressed by <b>name</b> rather than id — {@code /academic-years/2026-2027} —
 * because that is how the whole system refers to one: every other collection stores the name as
 * a string in its own {@code academicYear} field.
 *
 * <p><b>There is no rename, and there must never be one.</b> A rename would not fail and would
 * not cascade. It would leave every stored {@code "2026-2027"} naming a year that no longer
 * answers to it, with every row still looking perfectly valid — discovered when a fee report
 * comes back empty. A URL that cannot change is a daily reminder that the thing it names cannot
 * either.
 */
@Service
@RequiredArgsConstructor
public class AcademicYearService {

    private static final String TARGET_COLLECTION = "academic_years";
    private static final String MODULE_CODE = "CORE";

    /** Repeated on every gate response until permissions exist. Deliberately hard to miss. */
    private static final String NO_AUTHORIZATION_YET =
            "No authorization is enforced on this endpoint yet: any caller who can reach it can "
                    + "run it. Audit rows are being written, so the history will be there when "
                    + "permissions arrive.";

    private final AcademicYearRepository academicYears;
    private final CurrentSchoolResolver currentSchool;
    private final CoreValidator coreValidator;
    private final AuditTrail auditTrail;

    //! endpoint 18 — create a year ----------------------------------------------------

    /**
     * Creates an academic year with an empty calendar.
     *
     * <p>The overlap check is the one that matters. Two years covering one date means every
     * "which year is this?" lookup has two answers — and since AcademicYear deliberately has no
     * {@code current} flag, the dates are the only thing that can answer it.
     *
     * <p>Holidays are deliberately not accepted here; they are their own resource with their own
     * endpoints. A year always starts with nothing in its calendar, which is also why the
     * response tells the caller to go and fill it in.
     */
    @Transactional
    public AcademicYearResponse createAcademicYear(AcademicYearCreateRequest request) {
        //! step 1 - who is asking
        School school = currentSchool.requireUsable();
        String name = request.name().trim();

        //! step 2 - the name must be free, and it can never be changed later
        if (academicYears.existsBySchoolIdAndName(school.getId(), name)) {
            throw ApiException.conflict("ACADEMIC_YEAR_NAME_TAKEN",
                    "This school already has a year called '" + name + "'.");
        }

        //! step 3 - the dates must make sense on their own
        coreValidator.validateAcademicYearRange(request.startDate(), request.endDate());

        //! step 4 - and must not overlap a year that already exists
        coreValidator.validateNoAcademicYearOverlap(
                school.getId(), null, request.startDate(), request.endDate());

        //! step 5 - build the document, with an empty calendar
        // Holidays are never set here. The calendar is its own resource with its own endpoints
        // (#20 to #23), and mixing it into creation meant one request that could fail for two
        // unrelated reasons — a bad date range or a stray holiday — with the caller having to
        // work out which.
        AcademicYear year = AcademicYear.builder()
                .schoolId(school.getId())
                .name(name)
                .startDate(request.startDate())
                .endDate(request.endDate())
                .holidays(new ArrayList<>())
                .enrollmentEnabled(false)
                .resultsLocked(false)
                .build();

        //! step 6 - save
        AcademicYear savedYear = academicYears.save(year);

        return AcademicYearResponse.fromAcademicYear(savedYear,
                "The year has no calendar yet. Add holidays next — and use "
                        + "generate-weekly-off for the weekly offs rather than entering ~52 "
                        + "dates by hand, because every non-working day is a dated entry.");
    }

    //! endpoint 19 — move the boundaries ----------------------------------------------

    /**
     * Moves a year's start or end date.
     *
     * <p>Riskier than it looks, and the risk is one-sided. Extending a year usually harms
     * nothing. <b>Shrinking one can leave data outside the year that owns it</b> — a holiday
     * today, an attendance record or an invoice once those exist.
     *
     * <p>What is checked here: the new range is still plausible, still does not overlap another
     * year, and still contains every holiday already on this year.
     *
     * <p><b>What is not checked, and should be:</b> attendance records, invoices, trips and
     * everything else that stores this year's name. None of those repositories exist yet. When
     * they do, this method must refuse to move a boundary past the earliest or latest row that
     * references the year — otherwise a shrink silently orphans them, and nothing anywhere will
     * complain.
     */
    @Transactional
    public AcademicYearResponse updateDates(String name, AcademicYearDatesRequest request) {
        //! step 1 - who is asking
        School school = currentSchool.requireUsable();

        //! step 2 - refuse a request that asks for nothing
        if (request.isEmpty()) {
            throw ApiException.badRequest("NOTHING_TO_UPDATE",
                    "Send startDate, endDate, or both.");
        }

        //! step 3 - find the year by its name
        AcademicYear year = academicYears.findBySchoolIdAndName(school.getId(), name.trim())
                .orElseThrow(() -> ApiException.notFound("ACADEMIC_YEAR_NOT_FOUND",
                        "No academic year called '" + name + "' in this school."));

        //! step 4 - work out the new range, keeping whichever date was not sent
        LocalDate newStart = request.startDate() == null ? year.getStartDate() : request.startDate();
        LocalDate newEnd = request.endDate() == null ? year.getEndDate() : request.endDate();
        coreValidator.validateAcademicYearRange(newStart, newEnd);

        //! step 5 - it must still not overlap another year
        coreValidator.validateNoAcademicYearOverlap(
                school.getId(), year.getId(), newStart, newEnd);

        //! step 6 - every holiday already on this year must still fall inside it
        // The only orphaning this method can currently detect. Everything else that references
        // a year does so by name, in collections with no repository yet.
        List<HolidayDetail> stranded = year.getHolidays() == null ? List.of()
                : year.getHolidays().stream()
                        .filter(h -> h.getDate().isBefore(newStart) || h.getDate().isAfter(newEnd))
                        .toList();
        if (!stranded.isEmpty()) {
            HolidayDetail first = stranded.get(0);
            throw ApiException.conflict("HOLIDAYS_OUTSIDE_NEW_RANGE",
                    stranded.size() + " closed day(s) would fall outside the new dates, starting "
                            + "with " + first.getDate() + " (" + describe(first)
                            + "). Remove them first, or choose different dates.");
        }

        //! step 7 - apply and save
        year.setStartDate(newStart);
        year.setEndDate(newEnd);
        AcademicYear savedYear = academicYears.save(year);

        return AcademicYearResponse.fromAcademicYear(savedYear,
                "Dates updated. Note that records in other collections reference this year by "
                        + "name and are not checked against the new range yet.");
    }

    //! endpoint 20 — replace the whole calendar ---------------------------------------

    /**
     * #20 — replaces the whole calendar.
     *
     * <p>A PUT because it is the bulk-import case: a school publishes next year's calendar in
     * one go, from a spreadsheet. Sending the complete list makes a half-imported calendar
     * impossible, which a sequence of individual adds cannot promise.
     *
     * <p>Everything already there is discarded, including generated weekly offs. That is what
     * replace means, and it is why #21 exists for adding one entry in-year.
     *
     * <p><b>The request is flat, storage is grouped.</b> The caller sends one row per reason,
     * the way a spreadsheet holds it, and this groups them by date. So two rows sharing a date
     * is not an error — that is a Sunday that is also Holi, and it becomes one closed day with
     * two reasons. What is refused is the same <i>type</i> twice on one date, which is a
     * duplicated row rather than a second reason.
     */
    @Transactional
    public HolidayCalendarResponse replaceCalendar(String name, List<HolidayRequest> requests) {
        //! step 1 - find the year
        AcademicYear year = loadYear(name);
        List<HolidayRequest> incoming = requests == null ? List.of() : requests;

        //! step 2 - group the flat rows by date, keeping the order they arrived in
        // LinkedHashMap so the built calendar reads back in the order the school sent it.
        Map<LocalDate, HolidayDetail> byDate = new LinkedHashMap<>();
        for (HolidayRequest row : incoming) {
            coreValidator.validateHolidayWithinYear(
                    row.name(), row.date(), year.getStartDate(), year.getEndDate());

            HolidayDetail day = byDate.computeIfAbsent(row.date(),
                    d -> HolidayDetail.builder().date(d).events(new ArrayList<>()).build());

            if (hasType(day, row.type())) {
                throw ApiException.badRequest("DUPLICATE_HOLIDAY_ENTRY",
                        "Two " + row.type() + " entries sent for " + row.date()
                                + ". A day can hold several reasons, but not the same one twice.");
            }
            day.getEvents().add(row.toEvent());
        }

        //! step 3 - swap the whole list and save
        int daysBefore = sizeOf(year);
        List<HolidayDetail> replacement = new ArrayList<>(byDate.values());
        year.setHolidays(replacement);
        //TODO: Save
        AcademicYear savedYear = academicYears.save(year);

        return HolidayCalendarResponse.fromAcademicYear(savedYear,
                "Replaced the calendar: " + daysBefore + " closed days out, " + replacement.size()
                        + " in (" + incoming.size() + " reasons).");
    }

    //! endpoint 21 — add one reason to a day ------------------------------------------
    /**
     * #21 — adds one reason to one day.
     *
     * <p>The in-year case: a bandh, an unexpected closure, a festival somebody missed.
     *
     * <p><b>A date that is already closed is not a conflict.</b> The reason is added to that day
     * alongside what is already there, which is how a Sunday becomes a weekly off that is also
     * Holi. The caller does not have to know whether the day exists yet.
     *
     * <p>What is refused is the same type twice on one day. A second WEEKLY_OFF on a Sunday that
     * already has one is a repeated request or a mistake, never a second reason.
     */
    @Transactional
    public HolidayCalendarResponse addHoliday(String name, HolidayRequest request) {
        //! step 1 - find the year
        AcademicYear year = loadYear(name);

        //! step 2 - inside the year, and this reason not already on that day
        coreValidator.validateHolidayWithinYear(
                request.name(), request.date(), year.getStartDate(), year.getEndDate());

        Optional<HolidayDetail> existing = findDay(year, request.date());
        if (existing.isPresent() && hasType(existing.get(), request.type())) {
            throw ApiException.conflict("HOLIDAY_ENTRY_EXISTS",
                    "There is already a " + request.type() + " entry on " + request.date()
                            + ". Edit or remove it first.");
        }

        //! step 3 - add to the day, creating the day if this is its first reason
        HolidayDetail day = existing.orElseGet(() -> {
            HolidayDetail created = HolidayDetail.builder()
                    .date(request.date())
                    .events(new ArrayList<>())
                    .build();
            ensureList(year).add(created);
            return created;
        });
        day.getEvents().add(request.toEvent());

        //TODO: Save
        AcademicYear savedYear = academicYears.save(year);

        return HolidayCalendarResponse.fromAcademicYear(savedYear,
                "Added '" + request.name() + "' on " + request.date()
                        + (existing.isPresent()
                                ? " alongside " + (day.getEvents().size() - 1) + " existing."
                                : "."));
    }

    //! endpoint 22 — edit one reason on a day -----------------------------------------
    /**
     * #22 — edits one reason on one day.
     *
     * <p>The date is the key and cannot be changed here. Moving a holiday is a delete followed
     * by an add, which leaves both dates visible instead of one silent edit.
     *
     * <p>{@code type} says which reason to edit. It may be omitted when the day has only one —
     * the common case, and making it mandatory there would be ceremony — and is required when
     * the day has several, because picking one for the caller would be wrong half the time.
     */
    @Transactional
    public HolidayCalendarResponse updateHoliday(String name, LocalDate date, HolidayType type,
            HolidayUpdateRequest request) {

        //! step 1 - find the year
        AcademicYear year = loadYear(name);

        //! step 2 - refuse a request that asks for nothing
        if (request.isEmpty()) {
            throw ApiException.badRequest("NOTHING_TO_UPDATE",
                    "Send at least one of name, description or newType.");
        }

        //! step 3 - find the day, then the one reason on it being edited
        HolidayDetail day = findDay(year, date)
                .orElseThrow(() -> ApiException.notFound("HOLIDAY_NOT_FOUND",
                        "No holiday on " + date + " in '" + year.getName() + "'."));
        HolidayEvent event = resolveEvent(day, type);

        //! step 4 - apply only what was sent
        if (request.name() != null) {
            String newName = request.name().trim();
            if (newName.isEmpty()) {
                throw ApiException.badRequest("HOLIDAY_NAME_REQUIRED",
                        "A holiday name cannot be removed. Send a new one, or omit the field.");
            }
            event.setName(newName);
        }
        if (request.description() != null) {
            event.setDescription(TextHelper.blankToNull(request.description()));
        }
        if (request.newType() != null && request.newType() != event.getType()) {
            // Retyping cannot collide with what the day already holds — one day, one reason of
            // each type.
            if (hasType(day, request.newType())) {
                throw ApiException.conflict("HOLIDAY_ENTRY_EXISTS",
                        "There is already a " + request.newType() + " entry on " + date
                                + ", so this one cannot become that.");
            }
            event.setType(request.newType());
        }

        //! step 5 - save
        //TODO: Save
        AcademicYear savedYear = academicYears.save(year);

        return HolidayCalendarResponse.fromAcademicYear(savedYear,
                "Updated '" + event.getName() + "' on " + date + ".");
    }

    //! endpoint DELETE — remove a reason, or the whole day ----------------------------
    /**
     * Removes one reason from a day, or the whole day.
     *
     * <p>With {@code type}, only that reason goes: a Sunday that was also Holi is still a weekly
     * off afterwards. <b>Removing the last reason removes the day</b>, because a closed day with
     * nothing saying why reads as corruption to whoever finds it.
     *
     * <p>Without {@code type}, the whole day is removed with every reason on it. That is allowed
     * — "the school is open that day after all" is a real correction — but the change summary
     * names what went, so a caller who meant to drop one reason sees that they dropped two.
     *
     * <p>A date with nothing on it is a 404, not a silent 200.
     */
    @Transactional
    public HolidayCalendarResponse removeHoliday(String name, LocalDate date, HolidayType type) {
        //! step 1 - find the year
        AcademicYear year = loadYear(name);

        //! step 2 - the day has to be there
        HolidayDetail day = findDay(year, date)
                .orElseThrow(() -> ApiException.notFound("HOLIDAY_NOT_FOUND",
                        "No holiday on " + date + " in '" + year.getName() + "'."));

        //! step 3 - drop one reason, or the whole day
        String summary;
        if (type == null) {
            summary = "Removed " + describe(day) + " on " + date + ".";
            ensureList(year).remove(day);
        } else {
            HolidayEvent event = resolveEvent(day, type);
            day.getEvents().remove(event);
            if (day.getEvents().isEmpty()) {
                // The last reason went, so the day is no longer a closed day.
                ensureList(year).remove(day);
                summary = "Removed '" + event.getName() + "' on " + date
                        + ", which is now a working day.";
            } else {
                summary = "Removed '" + event.getName() + "' on " + date + ", which stays closed for "
                        + describe(day) + ".";
            }
        }

        //TODO: Save
        AcademicYear savedYear = academicYears.save(year);

        return HolidayCalendarResponse.fromAcademicYear(savedYear, summary);
    }

    //! endpoint 23 — generate a weekday's offs across the year ------------------------
    /**
     * #23 — generates one weekday's non-working days across the year.
     *
     * <p>Required by the model rather than a convenience. There is no "weekly off day" field
     * anywhere in this system: schools here may run on Sunday with the off day on any other
     * weekday, so every non-working day is a dated entry and a year needs roughly 52 of them.
     * Without this, somebody types 52 dates or a developer hardcodes Sunday.
     *
     * <p><b>A date that already has a festival still gets its weekly off.</b> The two reasons sit
     * on the same day, which is the whole point of a day holding an array: the school was closed
     * for Diwali <i>and</i> it was their weekly off, and a report that only knows one of those is
     * wrong about the other. The old behaviour — skip anything already booked — quietly lost
     * every weekly off that landed on a festival.
     *
     * <p>Only an existing WEEKLY_OFF on that date is skipped, and the skipped dates are returned.
     * That is also what makes running this twice safe: the second run generates nothing.
     */
    @Transactional
    public WeeklyOffGenerateResponse generateWeeklyOff(String name, GenerateWeeklyOffRequest request) {
        //! step 1 - find the year
        AcademicYear year = loadYear(name);

        //! step 2 - the window, defaulting to the whole year
        LocalDate from = request.fromDate() == null ? year.getStartDate() : request.fromDate();
        LocalDate to = request.toDate() == null ? year.getEndDate() : request.toDate();
        if (from.isAfter(to)) {
            throw ApiException.badRequest("INVALID_DATE_RANGE",
                    "fromDate (" + from + ") must not be after toDate (" + to + ").");
        }
        coreValidator.validateHolidayWithinYear("fromDate", from,
                year.getStartDate(), year.getEndDate());
        coreValidator.validateHolidayWithinYear("toDate", to,
                year.getStartDate(), year.getEndDate());

        //! step 3 - walk the window, adding that weekday wherever no weekly off is on it yet
        DayOfWeek target = request.dayOfWeek();
        String label = request.nameOrDefault();
        List<HolidayDetail> holidays = ensureList(year);

        // The existing days by date, so each date in the window is one lookup rather than a scan.
        Map<LocalDate, HolidayDetail> byDate = new LinkedHashMap<>();
        holidays.forEach(h -> byDate.put(h.getDate(), h));

        List<LocalDate> skipped = new ArrayList<>();
        int generated = 0;
        for (LocalDate day = from; !day.isAfter(to); day = day.plusDays(1)) {
            if (day.getDayOfWeek() != target) {
                continue;
            }

            HolidayDetail existing = byDate.get(day);
            if (existing != null && hasType(existing, HolidayType.WEEKLY_OFF)) {
                skipped.add(day);
                continue;
            }

            HolidayDetail entry = existing;
            if (entry == null) {
                entry = HolidayDetail.builder().date(day).events(new ArrayList<>()).build();
                holidays.add(entry);
                byDate.put(day, entry);
            }
            entry.getEvents().add(HolidayEvent.builder()
                    .name(label)
                    .type(HolidayType.WEEKLY_OFF)
                    .build());
            generated++;
        }

        //! step 4 - save
        //TODO: Save
        AcademicYear savedYear = academicYears.save(year);

        return new WeeklyOffGenerateResponse(
                savedYear.getName(), target, from, to, generated, skipped.size(), skipped,
                sizeOf(savedYear), eventCount(savedYear),
                generated == 0
                        ? "Nothing generated — every " + target + " in that window already had a "
                                + "weekly off."
                        : "Generated " + generated + " " + target + " entries, skipped "
                                + skipped.size() + " that already had one.");
    }

    //! endpoint DELETE — remove every reason of one type ------------------------------
    /**
     * Removes every reason of one type across the calendar. The companion to #23.
     *
     * <p>It exists because the first thing anybody does with the generator is pick the wrong
     * weekday, and undoing that one date at a time across 52 entries is not a thing a person
     * should have to do.
     *
     * <p>Strips the matching reason wherever it appears and then <b>drops the days left with
     * none</b>. A Sunday that was also Holi survives as Holi; a plain Sunday goes entirely.
     *
     * <p>The type is required. A bulk delete that cleared the whole calendar when a query
     * parameter was forgotten would be the most destructive accident in this package.
     */
    @Transactional
    public HolidayCalendarResponse removeHolidaysByType(String name, HolidayType type) {
        //! step 1 - find the year
        AcademicYear year = loadYear(name);

        //! step 2 - strip that reason everywhere, then drop the days it emptied
        List<HolidayDetail> holidays = ensureList(year);
        int daysBefore = holidays.size();
        int removed = 0;
        for (HolidayDetail day : holidays) {
            removed += day.getEvents().removeIf(e -> e.getType() == type) ? 1 : 0;
        }
        holidays.removeIf(day -> day.getEvents().isEmpty());
        int daysClosed = daysBefore - holidays.size();

        //! step 3 - save
        //TODO: Save
        AcademicYear savedYear = academicYears.save(year);

        return HolidayCalendarResponse.fromAcademicYear(savedYear,
                removed == 0
                        ? "Nothing to remove — no " + type + " entries were on this calendar."
                        : "Removed " + removed + " " + type + " entries; " + daysClosed
                                + " days became working days, "
                                + (removed - daysClosed) + " stayed closed for other reasons.");
    }

    //! endpoint 24 — open the year to enrollments -------------------------------------
    /**
     * #24 — opens the year to new enrollments.
     *
     * <p>Idempotent. A year already open comes back {@code 200} saying so, because the caller
     * asked for a state and that state holds — refusing a retry would only invite the caller to
     * check first and race.
     */
    @Transactional
    public AcademicYearResponse enableEnrollment(String name) {
        //! step 1 - find the year
        AcademicYear year = loadYear(name);

        //! step 2 - nothing to do if it is already open
        if (Boolean.TRUE.equals(year.getEnrollmentEnabled())) {
            return AcademicYearResponse.fromAcademicYear(year,
                    "Enrollment was already enabled for '" + year.getName() + "'. "
                            + NO_AUTHORIZATION_YET);
        }

        //! step 3 - open it and save
        year.setEnrollmentEnabled(true);
        AcademicYear savedYear = academicYears.save(year);

        return AcademicYearResponse.fromAcademicYear(savedYear,
                "Enrollment enabled for '" + savedYear.getName() + "'. " + NO_AUTHORIZATION_YET);
    }

    //! endpoint 25 — close the year to enrollments ------------------------------------
    /**
     * #25 — closes the year to new enrollments.
     *
     * <p>Does not touch students already enrolled. This is a gate on new writes, not a
     * withdrawal: anything already in the year stays exactly as it is.
     */
    @Transactional
    public AcademicYearResponse disableEnrollment(String name) {
        //! step 1 - find the year
        AcademicYear year = loadYear(name);

        //! step 2 - nothing to do if it is already closed
        if (Boolean.FALSE.equals(year.getEnrollmentEnabled())) {
            return AcademicYearResponse.fromAcademicYear(year,
                    "Enrollment was already disabled for '" + year.getName()
                            + "'. Students already enrolled are unaffected. "
                            + NO_AUTHORIZATION_YET);
        }

        //! step 3 - close it and save
        year.setEnrollmentEnabled(false);
        AcademicYear savedYear = academicYears.save(year);

        return AcademicYearResponse.fromAcademicYear(savedYear,
                "Enrollment disabled for '" + savedYear.getName()
                        + "'. Students already enrolled are unaffected. " + NO_AUTHORIZATION_YET);
    }



    //* ---------------------------------------------------------------------------------


        /**
         * Loads an academic year for the current school, or throws 404 if not found.
         */
        private AcademicYear loadYear(String name) {
        School school = currentSchool.requireUsable();

        return academicYears.findBySchoolIdAndName(school.getId(), name.trim())
                .orElseThrow(() -> ApiException.notFound(
                        "ACADEMIC_YEAR_NOT_FOUND",
                        "No academic year called '" + name + "' in this school."));
        }

    /**
     * The holiday list, created if the document has none.
     *
     * <p>Mongo stores an empty list as an empty array, but a document written before the field
     * existed comes back null — and the builder default does not apply to what is read from the
     * database.
     */
    private List<HolidayDetail> ensureList(AcademicYear year) {
        if (year.getHolidays() == null) {
            year.setHolidays(new ArrayList<>());
        }
        return year.getHolidays();
    }

    /** How many days the school is closed. Not the same number as the reasons for it. */
    private int sizeOf(AcademicYear year) {
        return year.getHolidays() == null ? 0 : year.getHolidays().size();
    }

    /** How many reasons are recorded, which is larger whenever a day carries more than one. */
    private int eventCount(AcademicYear year) {
        return ensureList(year).stream().mapToInt(d -> d.getEvents().size()).sum();
    }

    /** The one entry for a date, if the school is closed that day. A date appears at most once. */
    private Optional<HolidayDetail> findDay(AcademicYear year, LocalDate date) {
        return ensureList(year).stream().filter(h -> h.getDate().equals(date)).findFirst();
    }

    private boolean hasType(HolidayDetail day, HolidayType type) {
        return day.getEvents().stream().anyMatch(e -> e.getType() == type);
    }

    /**
     * Which reason on a day an edit or a delete is aimed at.
     *
     * <p>With no type given, this only works when the day has exactly one reason. Where it has
     * more, it asks rather than guesses — silently editing the first of two would be wrong as
     * often as it was right, and the caller would not see it happen.
     */
    private HolidayEvent resolveEvent(HolidayDetail day, HolidayType type) {
        List<HolidayEvent> events = day.getEvents();

        if (type == null) {
            if (events.size() > 1) {
                throw ApiException.badRequest("HOLIDAY_TYPE_REQUIRED",
                        day.getDate() + " is closed for " + events.size() + " reasons ("
                                + describe(day) + "). Add ?type= to say which one you mean.");
            }
            return events.get(0);
        }

        return events.stream()
                .filter(e -> e.getType() == type)
                .findFirst()
                .orElseThrow(() -> ApiException.notFound("HOLIDAY_ENTRY_NOT_FOUND",
                        "No " + type + " entry on " + day.getDate() + ". That day is closed for "
                                + describe(day) + "."));
    }

    /** A day's reasons in one readable phrase, for error and change messages. */
    private String describe(HolidayDetail day) {
        return day.getEvents().stream()
                .map(e -> e.getName() + " (" + e.getType() + ")")
                .collect(Collectors.joining(", "));
    }

   
}
