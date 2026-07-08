package com.orbitastra.backend.controllers.academics;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.orbitastra.backend.dto.academics.DaySchedule;
import com.orbitastra.backend.dto.academics.TimetableCreateRequest;
import com.orbitastra.backend.dto.academics.TimetableCreationResult;
import com.orbitastra.backend.models.academics.DailyTimetable;
import com.orbitastra.backend.services.academics.DailyTimetableService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/timetables")
@RequiredArgsConstructor
public class DailyTimetableController {

    private final DailyTimetableService dailyTimetableService;

    /**
     * Creates the daily timetables of a school over a date range for any
     * number of class sections. Holidays and weekly offs are skipped and
     * reported in the response. See {@link TimetableCreateRequest}.
     */
    @PostMapping
    public ResponseEntity<TimetableCreationResult> createTimetable(@RequestBody TimetableCreateRequest request) {
        TimetableCreationResult result = dailyTimetableService.createTimetable(request);
        return new ResponseEntity<>(result, HttpStatus.CREATED);
    }

    /** The whole school's timetable for one date (empty entries when nothing is stored). */
    @GetMapping("/school/{schoolId}/date/{date}")
    public ResponseEntity<DailyTimetable> getDay(
            @PathVariable String schoolId,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(dailyTimetableService.getDay(schoolId, date));
    }

    /** All stored day documents of a school in a date range. */
    @GetMapping("/school/{schoolId}/range")
    public ResponseEntity<List<DailyTimetable>> getRange(
            @PathVariable String schoolId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity.ok(dailyTimetableService.getRange(schoolId, startDate, endDate));
    }

    /** All day documents of one academic year (referenced by name, e.g. "2026-2027"). */
    @GetMapping("/school/{schoolId}/academic-year/{academicYearName}")
    public ResponseEntity<List<DailyTimetable>> getByAcademicYear(
            @PathVariable String schoolId,
            @PathVariable String academicYearName) {
        return ResponseEntity.ok(dailyTimetableService.getByAcademicYear(schoolId, academicYearName));
    }

    /** One class section's timetable per date within a range. */
    @GetMapping("/school/{schoolId}/class/{classId}/section/{section}")
    public ResponseEntity<List<DaySchedule>> getSectionSchedule(
            @PathVariable String schoolId,
            @PathVariable String classId,
            @PathVariable String section,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity.ok(
                dailyTimetableService.getSectionSchedule(schoolId, classId, section, startDate, endDate));
    }

    /** One teacher's timetable per date within a range. */
    @GetMapping("/school/{schoolId}/teacher/{teacherId}")
    public ResponseEntity<List<DaySchedule>> getTeacherSchedule(
            @PathVariable String schoolId,
            @PathVariable String teacherId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity.ok(dailyTimetableService.getTeacherSchedule(schoolId, teacherId, startDate, endDate));
    }

    /** Deletes the whole timetable of one date. */
    @DeleteMapping("/school/{schoolId}/date/{date}")
    public ResponseEntity<?> deleteDay(
            @PathVariable String schoolId,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        dailyTimetableService.deleteDay(schoolId, date);
        return ResponseEntity.ok(Map.of("message", "Timetable of " + date + " deleted."));
    }

    /** Removes one class section's entries from every day in the range. */
    @DeleteMapping("/school/{schoolId}/class/{classId}/section/{section}")
    public ResponseEntity<?> clearSection(
            @PathVariable String schoolId,
            @PathVariable String classId,
            @PathVariable String section,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        long removed = dailyTimetableService.clearSection(schoolId, classId, section, startDate, endDate);
        return ResponseEntity.ok(Map.of("message", "Removed " + removed + " timetable entr(ies)."));
    }

    /** Removes a single entry from one date's timetable. */
    @DeleteMapping("/school/{schoolId}/date/{date}/entry/{entryId}")
    public ResponseEntity<?> deleteEntry(
            @PathVariable String schoolId,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @PathVariable String entryId) {
        dailyTimetableService.deleteEntry(schoolId, date, entryId);
        return ResponseEntity.ok(Map.of("message", "Entry deleted from the timetable of " + date + "."));
    }
}
