package com.orbitastra.backend.controllers.academics;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.orbitastra.backend.models.academics.DisciplineLog;
import com.orbitastra.backend.services.academics.DisciplineLogService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/discipline-logs")
@RequiredArgsConstructor
public class DisciplineLogController {

    private final DisciplineLogService disciplineLogService;

    @PostMapping
    public ResponseEntity<DisciplineLog> createDisciplineLog(@RequestBody DisciplineLog log) {
        DisciplineLog created = disciplineLogService.createDisciplineLog(log);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<DisciplineLog>> getAllDisciplineLogs() {
        List<DisciplineLog> logs = disciplineLogService.getAllDisciplineLogs();
        return ResponseEntity.ok(logs);
    }

    @GetMapping("/{id}")
    public ResponseEntity<DisciplineLog> getDisciplineLogById(@PathVariable String id) {
        DisciplineLog log = disciplineLogService.getDisciplineLogById(id);
        return ResponseEntity.ok(log);
    }

    @GetMapping("/school/{schoolId}")
    public ResponseEntity<List<DisciplineLog>> getDisciplineLogsBySchool(@PathVariable String schoolId) {
        List<DisciplineLog> logs = disciplineLogService.getDisciplineLogsBySchool(schoolId);
        return ResponseEntity.ok(logs);
    }

    @GetMapping("/school/{schoolId}/academic-year/{academicYear}")
    public ResponseEntity<List<DisciplineLog>> getDisciplineLogsBySchoolAndAcademicYear(
            @PathVariable String schoolId,
            @PathVariable String academicYear) {
        return ResponseEntity.ok(disciplineLogService.getDisciplineLogsBySchoolAndAcademicYear(schoolId, academicYear));
    }

    @GetMapping("/student/{studentId}")
    public ResponseEntity<List<DisciplineLog>> getDisciplineLogsByStudent(@PathVariable String studentId) {
        List<DisciplineLog> logs = disciplineLogService.getDisciplineLogsByStudent(studentId);
        return ResponseEntity.ok(logs);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<DisciplineLog> updateDisciplineLog(
            @PathVariable String id, 
            @RequestBody DisciplineLog logDetails) {
        DisciplineLog updated = disciplineLogService.updateDisciplineLog(id, logDetails);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteDisciplineLog(@PathVariable String id) {
        disciplineLogService.deleteDisciplineLog(id);
        return ResponseEntity.ok(Map.of("message", "Discipline log deleted successfully."));
    }
}
