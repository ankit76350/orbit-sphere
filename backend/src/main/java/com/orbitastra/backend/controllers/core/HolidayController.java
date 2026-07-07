package com.orbitastra.backend.controllers.core;

import java.time.LocalDate;
import java.util.List;

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

import com.orbitastra.backend.models.core.Holiday;
import com.orbitastra.backend.models.core.HolidayDetail;
import com.orbitastra.backend.services.core.HolidayService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/holidays")
@RequiredArgsConstructor
public class HolidayController {

    private final HolidayService holidayService;

    @PostMapping
    public ResponseEntity<Holiday> createHolidayCalendar(@RequestBody Holiday calendar) {
        Holiday created = holidayService.createHolidayCalendar(calendar);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<Holiday>> getAllHolidayCalendars() {
        return ResponseEntity.ok(holidayService.getAllHolidayCalendars());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Holiday> getHolidayCalendarById(@PathVariable String id) {
        return ResponseEntity.ok(holidayService.getHolidayCalendarById(id));
    }

    @GetMapping("/school/{schoolId}")
    public ResponseEntity<List<Holiday>> getHolidayCalendarsBySchool(@PathVariable String schoolId) {
        return ResponseEntity.ok(holidayService.getHolidayCalendarsBySchool(schoolId));
    }

    @GetMapping("/school/{schoolId}/academic-year/{academicYear}")
    public ResponseEntity<Holiday> getHolidayCalendarBySchoolAndAcademicYear(
            @PathVariable String schoolId, @PathVariable String academicYear) {
        return ResponseEntity.ok(holidayService.getHolidayCalendarBySchoolAndAcademicYear(schoolId, academicYear));
    }

    @GetMapping("/school/{schoolId}/academic-year/{academicYear}/range")
    public ResponseEntity<List<HolidayDetail>> getHolidaysInRange(
            @PathVariable String schoolId,
            @PathVariable String academicYear,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        return ResponseEntity.ok(holidayService.getHolidaysInRange(schoolId, academicYear, start, end));
    }

    @GetMapping("/school/{schoolId}/academic-year/{academicYear}/check")
    public ResponseEntity<?> isHoliday(
            @PathVariable String schoolId,
            @PathVariable String academicYear,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        boolean holiday = holidayService.isHoliday(schoolId, academicYear, date);
        return ResponseEntity.ok(java.util.Map.of("date", date.toString(), "isHoliday", holiday));
    }

    @PostMapping("/{id}/holidays")
    public ResponseEntity<Holiday> addHolidayToCalendar(@PathVariable String id, @RequestBody HolidayDetail detail) {
        return ResponseEntity.ok(holidayService.addHolidayToCalendar(id, detail));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Holiday> updateHolidayCalendar(@PathVariable String id, @RequestBody Holiday details) {
        return ResponseEntity.ok(holidayService.updateHolidayCalendar(id, details));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteHolidayCalendar(@PathVariable String id) {
        holidayService.deleteHolidayCalendar(id);
        return ResponseEntity.ok(java.util.Map.of("message", "Holiday calendar deleted successfully."));
    }
}
