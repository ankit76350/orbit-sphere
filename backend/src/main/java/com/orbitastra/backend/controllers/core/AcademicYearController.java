package com.orbitastra.backend.controllers.core;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.orbitastra.backend.dto.core.CreateAcademicYearRequest;
import com.orbitastra.backend.dto.core.UpdateAcademicYearRequest;
import com.orbitastra.backend.models.core.AcademicYear;
import com.orbitastra.backend.models.core.HolidayDetail;
import com.orbitastra.backend.services.core.AcademicYearService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/academic-years")
@RequiredArgsConstructor
public class AcademicYearController {

    private final AcademicYearService academicYearService;

    @PostMapping
    public ResponseEntity<AcademicYear> createAcademicYear(@Valid @RequestBody CreateAcademicYearRequest request) {
        AcademicYear year = AcademicYear.builder()
                .schoolId(request.getSchoolId())
                .name(request.getName())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .holidays(request.getHolidays())
                .build();
        AcademicYear created = academicYearService.createAcademicYear(year);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<AcademicYear>> getAllAcademicYears() {
        return ResponseEntity.ok(academicYearService.getAllAcademicYears());
    }

    @GetMapping("/{id}")
    public ResponseEntity<AcademicYear> getAcademicYearById(@PathVariable String id) {
        return ResponseEntity.ok(academicYearService.getAcademicYearById(id));
    }

    @GetMapping("/school/{schoolId}")
    public ResponseEntity<List<AcademicYear>> getAcademicYearsBySchool(@PathVariable String schoolId) {
        return ResponseEntity.ok(academicYearService.getAcademicYearsBySchool(schoolId));
    }

    @GetMapping("/school/{schoolId}/name/{name}")
    public ResponseEntity<AcademicYear> getAcademicYearBySchoolAndName(
            @PathVariable String schoolId, @PathVariable String name) {
        return ResponseEntity.ok(academicYearService.getAcademicYearBySchoolAndName(schoolId, name));
    }

    /** The academic year whose start/end dates contain the given date. */
    @GetMapping("/school/{schoolId}/for-date/{date}")
    public ResponseEntity<AcademicYear> getAcademicYearForDate(
            @PathVariable String schoolId,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(academicYearService.getAcademicYearForDate(schoolId, date));
    }

    @GetMapping("/{id}/holidays/range")
    public ResponseEntity<List<HolidayDetail>> getHolidaysInRange(
            @PathVariable String id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        return ResponseEntity.ok(academicYearService.getHolidaysInRange(id, start, end));
    }

    @GetMapping("/{id}/holidays/check")
    public ResponseEntity<?> isHoliday(
            @PathVariable String id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        boolean holiday = academicYearService.isHoliday(id, date);
        return ResponseEntity.ok(Map.of("date", date.toString(), "isHoliday", holiday));
    }

    @GetMapping("/school/{schoolId}/name/{name}/holidays/range")
    public ResponseEntity<List<HolidayDetail>> getHolidaysInRangeByName(
            @PathVariable String schoolId,
            @PathVariable String name,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        return ResponseEntity.ok(academicYearService.getHolidaysInRangeByName(schoolId, name, start, end));
    }

    @GetMapping("/school/{schoolId}/name/{name}/holidays/check")
    public ResponseEntity<?> isHolidayByName(
            @PathVariable String schoolId,
            @PathVariable String name,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        boolean holiday = academicYearService.isHolidayByName(schoolId, name, date);
        return ResponseEntity.ok(Map.of("date", date.toString(), "isHoliday", holiday));
    }

    /** Accepts a JSON array — add one or many dated holidays in a single call. */
    @PostMapping("/{id}/holidays")
    public ResponseEntity<AcademicYear> addHolidays(@PathVariable String id,
            @RequestBody List<HolidayDetail> details) {
        return ResponseEntity.ok(academicYearService.addHolidays(id, details));
    }

    /**
     * Adds a dated WEEKLY_OFF entry for every occurrence of the given day of
     * week in the academic year (e.g. all ~52 Sundays).
     */
    @PostMapping("/{id}/weekly-offs")
    public ResponseEntity<AcademicYear> addWeeklyOff(
            @PathVariable String id,
            @RequestParam DayOfWeek dayOfWeek,
            @RequestParam(required = false) String name) {
        return ResponseEntity.ok(academicYearService.addWeeklyOff(id, dayOfWeek, name));
    }

    /** Removes the holiday(s) on a date; optional "name" narrows the match when two holidays share the date. */
    @DeleteMapping("/{id}/holidays")
    public ResponseEntity<AcademicYear> removeHoliday(
            @PathVariable String id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) String name) {
        return ResponseEntity.ok(academicYearService.removeHoliday(id, date, name));
    }

    /** Removes the whole weekly-off series of a day (every dated WEEKLY_OFF entry on that weekday). */
    @DeleteMapping("/{id}/weekly-offs")
    public ResponseEntity<AcademicYear> removeWeeklyOff(
            @PathVariable String id,
            @RequestParam DayOfWeek dayOfWeek) {
        return ResponseEntity.ok(academicYearService.removeWeeklyOff(id, dayOfWeek));
    }

    @PutMapping("/{id}")
    public ResponseEntity<AcademicYear> updateAcademicYear(@PathVariable String id,
            @Valid @RequestBody UpdateAcademicYearRequest request) {
        AcademicYear details = AcademicYear.builder()
                .schoolId(request.getSchoolId())
                .name(request.getName())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .holidays(request.getHolidays())
                .build();
        return ResponseEntity.ok(academicYearService.updateAcademicYear(id, details));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteAcademicYear(@PathVariable String id) {
        academicYearService.deleteAcademicYear(id);
        return ResponseEntity.ok(Map.of("message", "Academic year deleted successfully."));
    }
}
