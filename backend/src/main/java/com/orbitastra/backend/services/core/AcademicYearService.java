package com.orbitastra.backend.services.core;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.orbitastra.backend.common.current.CurrentSchoolResolver;
import com.orbitastra.backend.common.error.exception.ApiException;
import com.orbitastra.backend.dto.core.academicyear.AcademicYearCreateRequest;
import com.orbitastra.backend.dto.core.academicyear.AcademicYearDatesRequest;
import com.orbitastra.backend.dto.core.academicyear.AcademicYearResponse;
import com.orbitastra.backend.models.core.AcademicYear;
import com.orbitastra.backend.models.core.School;
import com.orbitastra.backend.models.core.embedded.HolidayDetail;
import com.orbitastra.backend.repositories.core.AcademicYearRepository;
import com.orbitastra.backend.services.core.helper.CoreValidator;

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

}
