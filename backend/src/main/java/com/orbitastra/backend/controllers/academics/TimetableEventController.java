package com.orbitastra.backend.controllers.academics;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.orbitastra.backend.models.academics.TimetableEvent;
import com.orbitastra.backend.services.academics.TimetableEventService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/timetable-events")
@RequiredArgsConstructor
public class TimetableEventController {

    private final TimetableEventService timetableEventService;

    @PostMapping
    public ResponseEntity<TimetableEvent> createTimetableEvent(@RequestBody TimetableEvent event) {
        TimetableEvent created = timetableEventService.createTimetableEvent(event);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<TimetableEvent>> getAllTimetableEvents() {
        List<TimetableEvent> events = timetableEventService.getAllTimetableEvents();
        return ResponseEntity.ok(events);
    }

    @GetMapping("/{id}")
    public ResponseEntity<TimetableEvent> getTimetableEventById(@PathVariable String id) {
        TimetableEvent event = timetableEventService.getTimetableEventById(id);
        return ResponseEntity.ok(event);
    }

    @GetMapping("/school/{schoolId}")
    public ResponseEntity<List<TimetableEvent>> getTimetableEventsBySchool(@PathVariable String schoolId) {
        List<TimetableEvent> events = timetableEventService.getTimetableEventsBySchool(schoolId);
        return ResponseEntity.ok(events);
    }

    @GetMapping("/school/{schoolId}/class/{className}")
    public ResponseEntity<List<TimetableEvent>> getTimetableEventsByClassAndSchool(
            @PathVariable String schoolId, 
            @PathVariable String className) {
        List<TimetableEvent> events = timetableEventService.getTimetableEventsByClassAndSchool(className, schoolId);
        return ResponseEntity.ok(events);
    }

    @GetMapping("/school/{schoolId}/class-id/{classId}")
    public ResponseEntity<List<TimetableEvent>> getTimetableEventsByClassIdAndSchool(
            @PathVariable String schoolId, 
            @PathVariable String classId) {
        List<TimetableEvent> events = timetableEventService.getTimetableEventsByClassIdAndSchool(classId, schoolId);
        return ResponseEntity.ok(events);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<TimetableEvent> updateTimetableEvent(
            @PathVariable String id, 
            @RequestBody TimetableEvent eventDetails) {
        TimetableEvent updated = timetableEventService.updateTimetableEvent(id, eventDetails);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteTimetableEvent(@PathVariable String id) {
        timetableEventService.deleteTimetableEvent(id);
        return ResponseEntity.ok(Map.of("message", "Timetable event deleted successfully."));
    }
}
