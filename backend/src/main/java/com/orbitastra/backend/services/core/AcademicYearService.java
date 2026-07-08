package com.orbitastra.backend.services.core;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.orbitastra.backend.exceptions.ConflictException;
import com.orbitastra.backend.exceptions.ResourceNotFoundException;
import com.orbitastra.backend.models.core.AcademicYear;
import com.orbitastra.backend.models.core.HolidayDetail;
import com.orbitastra.backend.models.core.enums.HolidayType;
import com.orbitastra.backend.repositories.core.AcademicYearRepository;
import com.orbitastra.backend.repositories.core.SchoolRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AcademicYearService {

    private final AcademicYearRepository academicYearRepository;
    private final SchoolRepository schoolRepository;

    public AcademicYear createAcademicYear(AcademicYear year) {
        validateYear(year);
        if (academicYearRepository.existsBySchoolIdAndName(year.getSchoolId(), year.getName())) {
            throw new ConflictException(
                    "Academic year '" + year.getName() + "' already exists for this school.");
        }
        ensureNoOverlap(year, null);
        return academicYearRepository.save(year);
    }

    public List<AcademicYear> getAllAcademicYears() {
        return academicYearRepository.findAll();
    }

    public AcademicYear getAcademicYearById(String id) {
        return academicYearRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Academic year not found with id: " + id));
    }

    public List<AcademicYear> getAcademicYearsBySchool(String schoolId) {
        return academicYearRepository.findBySchoolId(schoolId);
    }

    public AcademicYear getAcademicYearBySchoolAndName(String schoolId, String name) {
        return academicYearRepository.findBySchoolIdAndName(schoolId, name)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Academic year '" + name + "' not found for this school."));
    }

    /** The academic year whose start/end dates contain the given date. */
    public AcademicYear getAcademicYearForDate(String schoolId, LocalDate date) {
        return academicYearRepository.findBySchoolId(schoolId).stream()
                .filter(y -> y.getStartDate() != null && y.getEndDate() != null
                        && !date.isBefore(y.getStartDate()) && !date.isAfter(y.getEndDate()))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No academic year of this school contains the date " + date + "."));
    }

    public AcademicYear updateAcademicYear(String id, AcademicYear details) {
        AcademicYear year = getAcademicYearById(id);
        validateYear(details);

        // The name is the key other collections reference (academicYear) —
        // renaming would orphan those references, so it is immutable.
        if (!year.getName().equals(details.getName())) {
            throw new IllegalArgumentException("The name of an academic year cannot be changed once created ('"
                    + year.getName() + "') — student records, classes and other data reference it by name. "
                    + "Create a new academic year instead.");
        }
        ensureNoOverlap(details, id);

        year.setSchoolId(details.getSchoolId());
        year.setStartDate(details.getStartDate());
        year.setEndDate(details.getEndDate());
        year.setHolidays(details.getHolidays());

        return academicYearRepository.save(year);
    }

    /** Adds one or many dated holidays in a single call — nothing is saved unless all of them are valid. */
    public AcademicYear addHolidays(String id, List<HolidayDetail> details) {
        if (details == null || details.isEmpty()) {
            throw new IllegalArgumentException("Please provide at least one holiday.");
        }
        AcademicYear year = getAcademicYearById(id);
        if (year.getHolidays() == null) {
            year.setHolidays(new ArrayList<>());
        }
        Set<String> seen = new HashSet<>();
        year.getHolidays().forEach(h -> seen.add(h.getName() + "::" + h.getDate()));
        for (HolidayDetail detail : details) {
            validateDetail(detail, year);
            if (!seen.add(detail.getName() + "::" + detail.getDate())) {
                throw new ConflictException("Holiday '" + detail.getName() + "' on " + detail.getDate()
                        + " already exists in this academic year.");
            }
        }
        year.getHolidays().addAll(details);
        return academicYearRepository.save(year);
    }

    /**
     * Adds one dated WEEKLY_OFF entry for every occurrence of the given day
     * of week between the year's start and end dates (e.g. all ~52 Sundays),
     * so single occurrences can be removed later.
     */
    public AcademicYear addWeeklyOff(String id, DayOfWeek dayOfWeek, String name) {
        if (dayOfWeek == null) {
            throw new IllegalArgumentException("Please choose the day of week for the weekly off (e.g. SUNDAY).");
        }
        AcademicYear year = getAcademicYearById(id);
        if (year.getHolidays() == null) {
            year.setHolidays(new ArrayList<>());
        }
        if (year.getHolidays().stream().anyMatch(h -> h.getType() == HolidayType.WEEKLY_OFF
                && h.getDate() != null && h.getDate().getDayOfWeek() == dayOfWeek)) {
            throw new ConflictException("A weekly off already exists for " + dayOfWeek + " in this academic year.");
        }
        String label = (name == null || name.isBlank())
                ? dayOfWeek.getDisplayName(TextStyle.FULL, Locale.ENGLISH)
                : name;
        int added = 0;
        for (LocalDate date = year.getStartDate(); !date.isAfter(year.getEndDate()); date = date.plusDays(1)) {
            if (date.getDayOfWeek() == dayOfWeek) {
                year.getHolidays().add(HolidayDetail.builder()
                        .name(label)
                        .type(HolidayType.WEEKLY_OFF)
                        .date(date)
                        .build());
                added++;
            }
        }
        if (added == 0) {
            throw new IllegalArgumentException(
                    "No " + dayOfWeek + " falls between " + year.getStartDate() + " and " + year.getEndDate() + ".");
        }
        return academicYearRepository.save(year);
    }

    /**
     * Removes the holiday(s) on the given date; the optional name narrows the
     * match when two holidays share a date (e.g. a festival on a Sunday).
     */
    public AcademicYear removeHoliday(String id, LocalDate date, String name) {
        if (date == null) {
            throw new IllegalArgumentException("Please give the date of the holiday to remove.");
        }
        AcademicYear year = getAcademicYearById(id);
        boolean removed = year.getHolidays() != null && year.getHolidays().removeIf(
                h -> date.equals(h.getDate())
                        && (name == null || name.isBlank() || name.equalsIgnoreCase(h.getName())));
        if (!removed) {
            throw new ResourceNotFoundException("No holiday" + (name != null && !name.isBlank()
                    ? " named '" + name + "'" : "") + " found on " + date + " in this academic year.");
        }
        return academicYearRepository.save(year);
    }

    /** Removes every dated WEEKLY_OFF entry falling on the given day of week (the whole series). */
    public AcademicYear removeWeeklyOff(String id, DayOfWeek dayOfWeek) {
        if (dayOfWeek == null) {
            throw new IllegalArgumentException("Please choose the day of week of the weekly off to remove.");
        }
        AcademicYear year = getAcademicYearById(id);
        boolean removed = year.getHolidays() != null && year.getHolidays().removeIf(
                h -> h.getType() == HolidayType.WEEKLY_OFF && h.getDate() != null
                        && h.getDate().getDayOfWeek() == dayOfWeek);
        if (!removed) {
            throw new ResourceNotFoundException(
                    "No weekly off found for " + dayOfWeek + " in this academic year.");
        }
        return academicYearRepository.save(year);
    }

    public void deleteAcademicYear(String id) {
        AcademicYear year = getAcademicYearById(id);
        academicYearRepository.delete(year);
    }

    /**
     * Returns every holiday entry whose date falls within the given range.
     * Weekly offs are stored as concrete dates (expanded at save time), so
     * this is a plain date filter.
     */
    public List<HolidayDetail> getHolidaysInRange(String academicYearId, LocalDate start, LocalDate end) {
        if (start.isAfter(end)) {
            throw new IllegalArgumentException("Start date must be on or before end date.");
        }
        AcademicYear year = getAcademicYearById(academicYearId);
        List<HolidayDetail> result = new ArrayList<>();
        if (year.getHolidays() == null) {
            return result;
        }
        for (HolidayDetail detail : year.getHolidays()) {
            if (detail.getDate() != null
                    && !detail.getDate().isBefore(start) && !detail.getDate().isAfter(end)) {
                result.add(detail);
            }
        }
        return result;
    }

    /** True if the given date is a holiday (weekly off or dated holiday) in this academic year. */
    public boolean isHoliday(String academicYearId, LocalDate date) {
        return !getHolidaysInRange(academicYearId, date, date).isEmpty();
    }

    /** Same as {@link #getHolidaysInRange} but addressed by school + year name (e.g. "2026-2027"). */
    public List<HolidayDetail> getHolidaysInRangeByName(String schoolId, String name,
            LocalDate start, LocalDate end) {
        return getHolidaysInRange(getAcademicYearBySchoolAndName(schoolId, name).getId(), start, end);
    }

    /** Same as {@link #isHoliday} but addressed by school + year name (e.g. "2026-2027"). */
    public boolean isHolidayByName(String schoolId, String name, LocalDate date) {
        return isHoliday(getAcademicYearBySchoolAndName(schoolId, name).getId(), date);
    }

    /**
     * Two academic years of one school must never overlap: the next year has
     * to start after the previous one ends.
     */
    private void ensureNoOverlap(AcademicYear year, String excludeId) {
        for (AcademicYear other : academicYearRepository.findBySchoolId(year.getSchoolId())) {
            if (other.getId().equals(excludeId)
                    || other.getStartDate() == null || other.getEndDate() == null) {
                continue;
            }
            boolean overlaps = !year.getStartDate().isAfter(other.getEndDate())
                    && !other.getStartDate().isAfter(year.getEndDate());
            if (overlaps) {
                throw new ConflictException("Academic year '" + year.getName() + "' (" + year.getStartDate()
                        + " to " + year.getEndDate() + ") overlaps with '" + other.getName() + "' ("
                        + other.getStartDate() + " to " + other.getEndDate()
                        + ") — a new academic year must start after the previous one ends.");
            }
        }
    }

    private void validateYear(AcademicYear year) {
        String schoolId = year.getSchoolId();
        if (schoolId == null || !schoolRepository.existsById(schoolId)) {
            throw new ResourceNotFoundException("School not found with id: " + schoolId);
        }
        if (year.getName() == null || year.getName().isBlank()) {
            throw new IllegalArgumentException("Please give the academic year a name (e.g. '2026-2027').");
        }
        if (year.getStartDate() == null) {
            throw new IllegalArgumentException("Please set the start date of the academic year.");
        }
        if (year.getEndDate() == null) {
            throw new IllegalArgumentException("Please set the end date of the academic year.");
        }
        if (!year.getEndDate().isAfter(year.getStartDate())) {
            throw new IllegalArgumentException("The end date of the academic year (" + year.getEndDate()
                    + ") must be after its start date (" + year.getStartDate() + ").");
        }
        if (year.getHolidays() != null) {
            year.getHolidays().forEach(detail -> validateDetail(detail, year));
        }
    }

    private void validateDetail(HolidayDetail detail, AcademicYear year) {
        if (detail == null || detail.getType() == null) {
            throw new IllegalArgumentException("Holiday type is required.");
        }
        if (detail.getDate() == null) {
            if (detail.getType() == HolidayType.WEEKLY_OFF) {
                throw new IllegalArgumentException("Weekly offs are stored as concrete dates — use "
                        + "POST /api/academic-years/{id}/weekly-offs?dayOfWeek=SUNDAY to add every "
                        + "occurrence of a day automatically.");
            }
            throw new IllegalArgumentException("Please give the holiday a date.");
        }
        if (year.getStartDate() != null && year.getEndDate() != null
                && (detail.getDate().isBefore(year.getStartDate()) || detail.getDate().isAfter(year.getEndDate()))) {
            throw new IllegalArgumentException("Holiday '" + detail.getName() + "' (" + detail.getDate()
                    + ") is outside the academic year " + year.getStartDate() + " to " + year.getEndDate() + ".");
        }
    }
}
