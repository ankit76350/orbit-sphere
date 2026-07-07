package com.orbitastra.backend.services.core;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.orbitastra.backend.exceptions.ConflictException;
import com.orbitastra.backend.exceptions.ResourceNotFoundException;
import com.orbitastra.backend.models.core.Holiday;
import com.orbitastra.backend.models.core.HolidayDetail;
import com.orbitastra.backend.models.core.enums.HolidayType;
import com.orbitastra.backend.repositories.core.HolidayRepository;
import com.orbitastra.backend.repositories.core.SchoolRepository;

@Service
public class HolidayService {

    private final HolidayRepository holidayRepository;
    private final SchoolRepository schoolRepository;

    @Autowired
    public HolidayService(HolidayRepository holidayRepository, SchoolRepository schoolRepository) {
        this.holidayRepository = holidayRepository;
        this.schoolRepository = schoolRepository;
    }

    public Holiday createHolidayCalendar(Holiday calendar) {
        validateCalendar(calendar);
        if (holidayRepository.existsBySchoolIdAndAcademicYear(calendar.getSchoolId(), calendar.getAcademicYear())) {
            throw new ConflictException("Holiday calendar already exists for school " + calendar.getSchoolId()
                    + " in academic year " + calendar.getAcademicYear());
        }
        return holidayRepository.save(calendar);
    }

    public List<Holiday> getAllHolidayCalendars() {
        return holidayRepository.findAll();
    }

    public Holiday getHolidayCalendarById(String id) {
        return holidayRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Holiday calendar not found with id: " + id));
    }

    public List<Holiday> getHolidayCalendarsBySchool(String schoolId) {
        return holidayRepository.findBySchoolId(schoolId);
    }

    public Holiday getHolidayCalendarBySchoolAndAcademicYear(String schoolId, String academicYear) {
        return holidayRepository.findBySchoolIdAndAcademicYear(schoolId, academicYear)
                .orElseThrow(() -> new ResourceNotFoundException("Holiday calendar not found for school " + schoolId
                        + " in academic year " + academicYear));
    }

    public Holiday updateHolidayCalendar(String id, Holiday details) {
        Holiday calendar = getHolidayCalendarById(id);
        validateCalendar(details);

        holidayRepository.findBySchoolIdAndAcademicYear(details.getSchoolId(), details.getAcademicYear())
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new ConflictException("Holiday calendar already exists for school " + details.getSchoolId()
                            + " in academic year " + details.getAcademicYear());
                });

        calendar.setSchoolId(details.getSchoolId());
        calendar.setAcademicYear(details.getAcademicYear());
        calendar.setHolidays(details.getHolidays());

        return holidayRepository.save(calendar);
    }

    public Holiday addHolidayToCalendar(String id, HolidayDetail detail) {
        Holiday calendar = getHolidayCalendarById(id);
        validateDetail(detail);
        if (calendar.getHolidays() == null) {
            calendar.setHolidays(new ArrayList<>());
        }
        if (detail.getType() == HolidayType.WEEKLY_OFF
                && calendar.getHolidays().stream()
                        .anyMatch(h -> h.getType() == HolidayType.WEEKLY_OFF
                                && h.getDayOfWeek() == detail.getDayOfWeek())) {
            throw new ConflictException("Weekly off already exists for " + detail.getDayOfWeek() + " in this calendar.");
        }
        calendar.getHolidays().add(detail);
        return holidayRepository.save(calendar);
    }

    public void deleteHolidayCalendar(String id) {
        Holiday calendar = getHolidayCalendarById(id);
        holidayRepository.delete(calendar);
    }

    /**
     * Returns every holiday entry that applies within the given date range:
     * dated holidays falling inside the range, plus weekly offs
     * (e.g. Saturday/Sunday) whose day occurs within the range.
     */
    public List<HolidayDetail> getHolidaysInRange(String schoolId, String academicYear, LocalDate start, LocalDate end) {
        if (start.isAfter(end)) {
            throw new IllegalArgumentException("Start date must be on or before end date.");
        }
        Holiday calendar = getHolidayCalendarBySchoolAndAcademicYear(schoolId, academicYear);
        List<HolidayDetail> result = new ArrayList<>();
        if (calendar.getHolidays() == null) {
            return result;
        }
        for (HolidayDetail detail : calendar.getHolidays()) {
            if (detail.getType() == HolidayType.WEEKLY_OFF) {
                if (detail.getDayOfWeek() != null && occursInRange(detail.getDayOfWeek(), start, end)) {
                    result.add(detail);
                }
            } else if (detail.getDate() != null
                    && !detail.getDate().isBefore(start) && !detail.getDate().isAfter(end)) {
                result.add(detail);
            }
        }
        return result;
    }

    /** True if the given date is a holiday for the school (weekly off or dated holiday). */
    public boolean isHoliday(String schoolId, String academicYear, LocalDate date) {
        return !getHolidaysInRange(schoolId, academicYear, date, date).isEmpty();
    }

    private void validateCalendar(Holiday calendar) {
        String schoolId = calendar.getSchoolId();
        if (schoolId == null || !schoolRepository.existsById(schoolId)) {
            throw new ResourceNotFoundException("School not found with id: " + schoolId);
        }
        if (calendar.getAcademicYear() == null || calendar.getAcademicYear().isBlank()) {
            throw new IllegalArgumentException("Academic year is required.");
        }
        if (calendar.getHolidays() != null) {
            calendar.getHolidays().forEach(this::validateDetail);
        }
    }

    private void validateDetail(HolidayDetail detail) {
        if (detail.getType() == null) {
            throw new IllegalArgumentException("Holiday type is required.");
        }
        if (detail.getType() == HolidayType.WEEKLY_OFF) {
            if (detail.getDayOfWeek() == null) {
                throw new IllegalArgumentException("Day of week is required for a weekly off.");
            }
        } else if (detail.getDate() == null) {
            throw new IllegalArgumentException("Date is required for a date-based holiday.");
        }
    }

    private boolean occursInRange(java.time.DayOfWeek dayOfWeek, LocalDate start, LocalDate end) {
        // Scanning at most 7 days covers every day of the week.
        LocalDate scanEnd = end.isBefore(start.plusDays(6)) ? end : start.plusDays(6);
        for (LocalDate d = start; !d.isAfter(scanEnd); d = d.plusDays(1)) {
            if (d.getDayOfWeek() == dayOfWeek) {
                return true;
            }
        }
        return false;
    }
}
