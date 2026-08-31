package com.orbitastra.backend.services.core;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.orbitastra.backend.common.current.CurrentSchoolResolver;
import com.orbitastra.backend.common.error.exception.ApiException;
import com.orbitastra.backend.dto.core.academicyear.AcademicYearCreateRequest;
import com.orbitastra.backend.dto.core.academicyear.AcademicYearDatesRequest;
import com.orbitastra.backend.dto.core.academicyear.AcademicYearResponse;
import com.orbitastra.backend.dto.core.academicyear.GenerateWeeklyOffRequest;
import com.orbitastra.backend.dto.core.academicyear.HolidayCalendarResponse;
import com.orbitastra.backend.dto.core.academicyear.HolidayRequest;
import com.orbitastra.backend.dto.core.academicyear.HolidayUpdateRequest;
import com.orbitastra.backend.dto.core.academicyear.WeeklyOffGenerateResponse;
import com.orbitastra.backend.models.core.AcademicYear;
import com.orbitastra.backend.models.core.School;
import com.orbitastra.backend.models.core.embedded.HolidayDetail;
import com.orbitastra.backend.models.core.enums.HolidayType;
import com.orbitastra.backend.repositories.core.AcademicYearRepository;
import com.orbitastra.backend.services.core.helper.CoreValidator;
import com.orbitastra.backend.services.core.helper.TextHelper;

import lombok.RequiredArgsConstructor;

/**
 * The school's academic years. Endpoints #18 and #19.
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

    private final AcademicYearRepository academicYears;
    private final CurrentSchoolResolver currentSchool;
    private final CoreValidator coreValidator;

    //! endpoint 18 — create a year -----------------------------------------------------

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

    //! endpoint 19 — move the boundaries -----------------------------------------------

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
                    stranded.size() + " holiday(s) would fall outside the new dates, starting "
                            + "with '" + first.getName() + "' on " + first.getDate()
                            + ". Remove them first, or choose different dates.");
        }

        //! step 7 - apply and save
        year.setStartDate(newStart);
        year.setEndDate(newEnd);
        AcademicYear savedYear = academicYears.save(year);

        return AcademicYearResponse.fromAcademicYear(savedYear,
                "Dates updated. Note that records in other collections reference this year by "
                        + "name and are not checked against the new range yet.");
    }

    //! endpoints 20 the adding holiday calendar ------------------------------------------

    /**
     * #20 — replaces the whole calendar.
     *
     * <p>A PUT because it is the bulk-import case: a school publishes next year's calendar in
     * one go, from a spreadsheet. Sending the complete list makes a half-imported calendar
     * impossible, which a sequence of individual adds cannot promise.
     *
     * <p>Everything already there is discarded, including generated weekly offs. That is what
     * replace means, and it is why #21 exists for adding one entry in-year.
     */
    @Transactional
    public HolidayCalendarResponse replaceCalendar(String name, List<HolidayRequest> requests) {
        //! step 1 - find the year
        AcademicYear year = loadYear(name);
        List<HolidayRequest> incoming = requests == null ? List.of() : requests;

        //! step 2 - every date inside the year, and no date twice
        Set<LocalDate> seen = new HashSet<>();
        List<HolidayDetail> replacement = new ArrayList<>();
        for (HolidayRequest holiday : incoming) {
            coreValidator.validateHolidayWithinYear(
                    holiday.name(), holiday.date(), year.getStartDate(), year.getEndDate());
            if (!seen.add(holiday.date())) {
                throw ApiException.badRequest("DUPLICATE_HOLIDAY_DATE",
                        "More than one holiday sent for " + holiday.date() + ".");
            }
            replacement.add(holiday.toDetail());
        }

        //! step 3 - swap the whole list and save
        int before = sizeOf(year);
        year.setHolidays(replacement);
        AcademicYear savedYear = academicYears.save(year);

        return HolidayCalendarResponse.fromAcademicYear(savedYear,
                "Replaced the calendar: " + before + " entries out, " + replacement.size()
                        + " in.");
    }

    /**
     * #21 — adds one holiday.
     *
     * <p>The in-year case: a bandh, an unexpected closure, a festival somebody missed. Refuses a
     * date that already has an entry rather than overwriting it — two reasons for one closure is
     * a question for a person, not something to resolve silently.
     */
    @Transactional
    public HolidayCalendarResponse addHoliday(String name, HolidayRequest request) {
        //! step 1 - find the year
        AcademicYear year = loadYear(name);

        //! step 2 - inside the year, and not already taken
        coreValidator.validateHolidayWithinYear(
                request.name(), request.date(), year.getStartDate(), year.getEndDate());
        if (findByDate(year, request.date()).isPresent()) {
            throw ApiException.conflict("HOLIDAY_DATE_TAKEN",
                    "There is already a holiday on " + request.date()
                            + ". Edit or remove it first.");
        }

        //! step 3 - append and save
        ensureList(year).add(request.toDetail());
        AcademicYear savedYear = academicYears.save(year);

        return HolidayCalendarResponse.fromAcademicYear(savedYear,
                "Added '" + request.name() + "' on " + request.date() + ".");
    }

    /**
     * #22 — edits the holiday on one date.
     *
     * <p>The date is the key and cannot be changed here. Moving a holiday is a delete followed
     * by an add, which leaves both dates visible instead of one silent edit.
     */
    @Transactional
    public HolidayCalendarResponse updateHoliday(String name, LocalDate date,
            HolidayUpdateRequest request) {

        //! step 1 - find the year
        AcademicYear year = loadYear(name);

        //! step 2 - refuse a request that asks for nothing
        if (request.isEmpty()) {
            throw ApiException.badRequest("NOTHING_TO_UPDATE",
                    "Send at least one of name, description or type.");
        }

        //! step 3 - find the entry on that date
        HolidayDetail holiday = findByDate(year, date)
                .orElseThrow(() -> ApiException.notFound("HOLIDAY_NOT_FOUND",
                        "No holiday on " + date + " in '" + year.getName() + "'."));

        //! step 4 - apply only what was sent
        if (request.name() != null) {
            String newName = request.name().trim();
            if (newName.isEmpty()) {
                throw ApiException.badRequest("HOLIDAY_NAME_REQUIRED",
                        "A holiday name cannot be removed. Send a new one, or omit the field.");
            }
            holiday.setName(newName);
        }
        if (request.description() != null) {
            holiday.setDescription(TextHelper.blankToNull(request.description()));
        }
        if (request.type() != null) {
            holiday.setType(request.type());
        }

        //! step 5 - save
        AcademicYear savedYear = academicYears.save(year);

        return HolidayCalendarResponse.fromAcademicYear(savedYear,
                "Updated the holiday on " + date + ".");
    }

    /** Removes the holiday on one date. A date with nothing on it is a 404, not a silent 200. */
    @Transactional
    public HolidayCalendarResponse removeHoliday(String name, LocalDate date) {
        //! step 1 - find the year
        AcademicYear year = loadYear(name);

        //! step 2 - it has to be there
        HolidayDetail holiday = findByDate(year, date)
                .orElseThrow(() -> ApiException.notFound("HOLIDAY_NOT_FOUND",
                        "No holiday on " + date + " in '" + year.getName() + "'."));

        //! step 3 - remove and save
        ensureList(year).remove(holiday);
        AcademicYear savedYear = academicYears.save(year);

        return HolidayCalendarResponse.fromAcademicYear(savedYear,
                "Removed '" + holiday.getName() + "' on " + date + ".");
    }

    /**
     * #23 — generates one weekday's non-working days across the year.
     *
     * <p>Required by the model rather than a convenience. There is no "weekly off day" field
     * anywhere in this system: schools here may run on Sunday with the off day on any other
     * weekday, so every non-working day is a dated entry and a year needs roughly 52 of them.
     * Without this, somebody types 52 dates or a developer hardcodes Sunday.
     *
     * <p><b>Dates that already carry a holiday are skipped, not overwritten.</b> A Sunday that is
     * also Diwali stays Diwali — the more specific reason is the more useful one. The skipped
     * dates are returned so the school can see what took precedence.
     *
     * <p>Safe to run twice: the second run generates nothing and reports everything skipped.
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

        //! step 3 - walk the window, adding that weekday where nothing is booked
        DayOfWeek target = request.dayOfWeek();
        String label = request.nameOrDefault();
        List<HolidayDetail> holidays = ensureList(year);
        Set<LocalDate> taken = new HashSet<>();
        holidays.forEach(h -> taken.add(h.getDate()));

        List<LocalDate> skipped = new ArrayList<>();
        int generated = 0;
        for (LocalDate day = from; !day.isAfter(to); day = day.plusDays(1)) {
            if (day.getDayOfWeek() != target) {
                continue;
            }
            if (taken.contains(day)) {
                skipped.add(day);
                continue;
            }
            holidays.add(HolidayDetail.builder()
                    .name(label)
                    .type(HolidayType.WEEKLY_OFF)
                    .date(day)
                    .build());
            generated++;
        }

        //! step 4 - save
        AcademicYear savedYear = academicYears.save(year);

        return new WeeklyOffGenerateResponse(
                savedYear.getName(), target, from, to, generated, skipped.size(), skipped,
                sizeOf(savedYear),
                generated == 0
                        ? "Nothing generated — every " + target + " in that window already had a "
                                + "holiday."
                        : "Generated " + generated + " " + target + " entries, skipped "
                                + skipped.size() + " that already had a holiday.");
    }

    /**
     * Removes every holiday of one type. The companion to #23.
     *
     * <p>It exists because the first thing anybody does with the generator is pick the wrong
     * weekday, and undoing that one date at a time across 52 entries is not a thing a person
     * should have to do.
     *
     * <p>The type is required. A bulk delete that cleared the whole calendar when a query
     * parameter was forgotten would be the most destructive accident in this package.
     */
    @Transactional
    public HolidayCalendarResponse removeHolidaysByType(String name, HolidayType type) {
        //! step 1 - find the year
        AcademicYear year = loadYear(name);

        //! step 2 - drop every entry of that type
        List<HolidayDetail> holidays = ensureList(year);
        int before = holidays.size();
        holidays.removeIf(h -> h.getType() == type);
        int removed = before - holidays.size();

        //! step 3 - save
        AcademicYear savedYear = academicYears.save(year);

        return HolidayCalendarResponse.fromAcademicYear(savedYear,
                removed == 0
                        ? "Nothing to remove — no " + type + " entries were on this calendar."
                        : "Removed " + removed + " " + type + " entries.");
    }

    // ---------------------------------------------------------------------------------

    /** The caller's year, by name, or a 404. */
    private AcademicYear loadYear(String name) {
        School school = currentSchool.requireUsable();
        return academicYears.findBySchoolIdAndName(school.getId(), name.trim())
                .orElseThrow(() -> ApiException.notFound("ACADEMIC_YEAR_NOT_FOUND",
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

    private int sizeOf(AcademicYear year) {
        return year.getHolidays() == null ? 0 : year.getHolidays().size();
    }

    private Optional<HolidayDetail> findByDate(AcademicYear year, LocalDate date) {
        return ensureList(year).stream().filter(h -> h.getDate().equals(date)).findFirst();
    }
}
