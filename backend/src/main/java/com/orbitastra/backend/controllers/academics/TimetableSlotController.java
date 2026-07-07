package com.orbitastra.backend.controllers.academics;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.orbitastra.backend.dto.academics.BulkTimetableRequest;
import com.orbitastra.backend.dto.academics.SchoolTimetableRequest;
import com.orbitastra.backend.dto.academics.TimetableOccurrence;
import com.orbitastra.backend.models.academics.TimetableSlot;
import com.orbitastra.backend.services.academics.TimetableSlotService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/timetable-slots")
@RequiredArgsConstructor
public class TimetableSlotController {

    private final TimetableSlotService timetableSlotService;

    @PostMapping
    public ResponseEntity<TimetableSlot> createSlot(@RequestBody TimetableSlot slot) {
        TimetableSlot created = timetableSlotService.createSlot(slot);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    /** Weekly timetable for one class section. See {@link BulkTimetableRequest}. */
    @PostMapping("/bulk")
    public ResponseEntity<List<TimetableSlot>> createBulkTimetable(@RequestBody BulkTimetableRequest request) {
        List<TimetableSlot> created = timetableSlotService.createBulkTimetable(request);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    /**
     * Weekly timetables for many class sections of one school (e.g. Nursery
     * to 12th, sections A-D) in one call. See {@link SchoolTimetableRequest}.
     */
    @PostMapping("/bulk-school")
    public ResponseEntity<List<TimetableSlot>> createSchoolTimetable(@RequestBody SchoolTimetableRequest request) {
        List<TimetableSlot> created = timetableSlotService.createSchoolTimetable(request);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<TimetableSlot>> getAllSlots() {
        return ResponseEntity.ok(timetableSlotService.getAllSlots());
    }

    @GetMapping("/{id}")
    public ResponseEntity<TimetableSlot> getSlotById(@PathVariable String id) {
        return ResponseEntity.ok(timetableSlotService.getSlotById(id));
    }

    @GetMapping("/school/{schoolId}")
    public ResponseEntity<List<TimetableSlot>> getSlotsBySchool(@PathVariable String schoolId) {
        return ResponseEntity.ok(timetableSlotService.getSlotsBySchool(schoolId));
    }

    /** The stored weekly template of a class (all sections). */
    @GetMapping("/school/{schoolId}/class/{classId}")
    public ResponseEntity<List<TimetableSlot>> getSlotsByClass(
            @PathVariable String schoolId,
            @PathVariable String classId) {
        return ResponseEntity.ok(timetableSlotService.getSlotsByClass(schoolId, classId));
    }

    /** Concrete lessons of a class between two dates, computed from the template. */
    @GetMapping("/school/{schoolId}/class/{classId}/schedule")
    public ResponseEntity<List<TimetableOccurrence>> getClassSchedule(
            @PathVariable String schoolId,
            @PathVariable String classId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity.ok(timetableSlotService.getClassSchedule(schoolId, classId, startDate, endDate));
    }

    /** Concrete lessons of a teacher between two dates, computed from the template. */
    @GetMapping("/school/{schoolId}/teacher/{teacherId}/schedule")
    public ResponseEntity<List<TimetableOccurrence>> getTeacherSchedule(
            @PathVariable String schoolId,
            @PathVariable String teacherId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity.ok(timetableSlotService.getTeacherSchedule(schoolId, teacherId, startDate, endDate));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<TimetableSlot> updateSlot(
            @PathVariable String id,
            @RequestBody TimetableSlot slotDetails) {
        return ResponseEntity.ok(timetableSlotService.updateSlot(id, slotDetails));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteSlot(@PathVariable String id) {
        timetableSlotService.deleteSlot(id);
        return ResponseEntity.ok(Map.of("message", "Timetable slot deleted successfully."));
    }

    /** Clears the whole weekly timetable of one class section. */
    @DeleteMapping("/school/{schoolId}/class/{classId}/section/{section}")
    public ResponseEntity<?> clearSectionTimetable(
            @PathVariable String schoolId,
            @PathVariable String classId,
            @PathVariable String section) {
        long deleted = timetableSlotService.clearSectionTimetable(schoolId, classId, section);
        return ResponseEntity.ok(Map.of("message", "Deleted " + deleted + " timetable slot(s)."));
    }
}
