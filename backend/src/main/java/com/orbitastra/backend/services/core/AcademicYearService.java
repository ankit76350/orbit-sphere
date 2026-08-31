package com.orbitastra.backend.services.core;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.orbitastra.backend.common.current.CurrentSchoolResolver;
import com.orbitastra.backend.common.error.exception.ApiException;
import com.orbitastra.backend.dto.core.academicyear.AcademicYearCreateRequest;
import com.orbitastra.backend.dto.core.academicyear.AcademicYearDatesRequest;
import com.orbitastra.backend.dto.core.academicyear.AcademicYearResponse;
import com.orbitastra.backend.dto.core.academicyear.HolidayRequest;
import com.orbitastra.backend.models.core.AcademicYear;
import com.orbitastra.backend.models.core.School;
import com.orbitastra.backend.models.core.embedded.HolidayDetail;
import com.orbitastra.backend.repositories.core.AcademicYearRepository;

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

    /** A year is a year. Outside this range it is a typo, not a calendar. */
    private static final long MIN_DAYS = 30;
    private static final long MAX_DAYS = 800;

    private final AcademicYearRepository academicYears;
    private final CurrentSchoolResolver currentSchool;

    //! endpoint 18 — create a year -----------------------------------------------------

    /**
     * Creates an academic year, optionally with its calendar already filled in.
     *
     * <p>The overlap check is the one that matters. Two years covering one date means every
     * "which year is this?" lookup has two answers — and since AcademicYear deliberately has no
     * {@code current} flag, the dates are the only thing that can answer it.
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
        validateRange(request.startDate(), request.endDate());

        //! step 4 - and must not overlap a year that already exists
        checkNoOverlap(school.getId(), null, request.startDate(), request.endDate());

        //! step 5 - holidays, if any came with the request
        List<HolidayDetail> holidays = buildHolidays(
                request.holidaysOrEmpty(), request.startDate(), request.endDate());

        //! step 6 - build the document
        AcademicYear year = AcademicYear.builder()
                .schoolId(school.getId())
                .name(name)
                .startDate(request.startDate())
                .endDate(request.endDate())
                .holidays(holidays)
                .enrollmentEnabled(false)
                .resultsLocked(false)
                .build();

        //! step 7 - save
        AcademicYear savedYear = academicYears.save(year);

        return AcademicYearResponse.fromAcademicYear(savedYear,
                holidays.isEmpty()
                        ? "Add the holiday calendar. Weekly offs are dated entries, so use "
                                + "generate-weekly-off rather than entering ~52 dates by hand."
                        : "Calendar started with " + holidays.size() + " entries. Generate the "
                                + "weekly offs if you have not already.");
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
        validateRange(newStart, newEnd);

        //! step 5 - it must still not overlap another year
        checkNoOverlap(school.getId(), year.getId(), newStart, newEnd);

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

    // ---------------------------------------------------------------------------------

    /** A start before an end, and a span that looks like a school year rather than a typo. */
    private void validateRange(LocalDate start, LocalDate end) {
        if (!start.isBefore(end)) {
            throw ApiException.badRequest("INVALID_DATE_RANGE",
                    "startDate (" + start + ") must be before endDate (" + end + ").");
        }
        long days = ChronoUnit.DAYS.between(start, end) + 1;
        if (days < MIN_DAYS || days > MAX_DAYS) {
            throw ApiException.badRequest("IMPLAUSIBLE_DATE_RANGE",
                    "An academic year of " + days + " days is almost certainly a typo. Expected "
                            + "between " + MIN_DAYS + " and " + MAX_DAYS + " days.");
        }
    }

    /**
     * No two years of one school may cover the same date.
     *
     * <p>Two ranges overlap unless one ends before the other starts. Written that way rather than
     * as four comparisons because the four-way version is where off-by-one bugs live.
     *
     * @param ignoreId the year being edited, excluded so it does not overlap itself
     */
    private void checkNoOverlap(String schoolId, String ignoreId, LocalDate start, LocalDate end) {
        for (AcademicYear other : academicYears.findBySchoolId(schoolId)) {
            if (other.getId().equals(ignoreId)) {
                continue;
            }
            boolean apart = end.isBefore(other.getStartDate()) || start.isAfter(other.getEndDate());
            if (!apart) {
                throw ApiException.conflict("ACADEMIC_YEAR_OVERLAP",
                        "These dates overlap '" + other.getName() + "' ("
                                + other.getStartDate() + " to " + other.getEndDate()
                                + "). Two years cannot cover the same day.");
            }
        }
    }

    /** Holidays must sit inside the year, and no date may appear twice. */
    private List<HolidayDetail> buildHolidays(List<HolidayRequest> requests,
            LocalDate start, LocalDate end) {

        List<HolidayDetail> details = new ArrayList<>();
        Set<LocalDate> seen = new HashSet<>();

        for (HolidayRequest holiday : requests) {
            if (holiday.date().isBefore(start) || holiday.date().isAfter(end)) {
                throw ApiException.badRequest("HOLIDAY_OUTSIDE_YEAR",
                        "'" + holiday.name() + "' on " + holiday.date()
                                + " is outside " + start + " to " + end + ".");
            }
            if (!seen.add(holiday.date())) {
                throw ApiException.badRequest("DUPLICATE_HOLIDAY_DATE",
                        "More than one holiday sent for " + holiday.date() + ".");
            }
            details.add(holiday.toDetail());
        }
        return details;
    }
}
