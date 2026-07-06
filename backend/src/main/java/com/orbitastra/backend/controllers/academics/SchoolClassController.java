package com.orbitastra.backend.controllers.academics;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.orbitastra.backend.models.academics.SchoolClass;
import com.orbitastra.backend.services.academics.SchoolClassService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/classes")
@RequiredArgsConstructor
public class SchoolClassController {

    private final SchoolClassService schoolClassService;

    @PostMapping
    public ResponseEntity<SchoolClass> createClass(@RequestBody SchoolClass schoolClass) {
        SchoolClass created = schoolClassService.createClass(schoolClass);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<SchoolClass>> getAllClasses() {
        List<SchoolClass> classes = schoolClassService.getAllClasses();
        return ResponseEntity.ok(classes);
    }

    @GetMapping("/{id}")
    public ResponseEntity<SchoolClass> getClassById(@PathVariable String id) {
        SchoolClass schoolClass = schoolClassService.getClassById(id);
        return ResponseEntity.ok(schoolClass);
    }

    @GetMapping("/school/{schoolId}")
    public ResponseEntity<List<SchoolClass>> getClassesBySchool(@PathVariable String schoolId) {
        List<SchoolClass> classes = schoolClassService.getClassesBySchool(schoolId);
        return ResponseEntity.ok(classes);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<SchoolClass> updateClass(@PathVariable String id, @RequestBody SchoolClass classDetails) {
        SchoolClass updated = schoolClassService.updateClass(id, classDetails);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteClass(@PathVariable String id) {
        schoolClassService.deleteClass(id);
        return ResponseEntity.ok(Map.of("message", "Class deleted successfully."));
    }

    @PostMapping("/{id}/subjects")
    public ResponseEntity<SchoolClass> addSubject(
            @PathVariable String id, 
            @RequestBody SchoolClass.ClassSubject subject) {
        SchoolClass updated = schoolClassService.addSubject(id, subject);
        return ResponseEntity.ok(updated);
    }

    @PostMapping("/{id}/sections")
    public ResponseEntity<SchoolClass> addSection(
            @PathVariable String id, 
            @RequestBody Map<String, String> body) {
        String section = body.get("section");
        if (section == null || section.isEmpty()) {
            throw new IllegalArgumentException("Section name cannot be null or empty.");
        }
        SchoolClass updated = schoolClassService.addSection(id, section);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}/sections/{section}")
    public ResponseEntity<SchoolClass> removeSection(
            @PathVariable String id, 
            @PathVariable String section) {
        SchoolClass updated = schoolClassService.removeSection(id, section);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}/subjects/{subjectName}")
    public ResponseEntity<SchoolClass> removeSubject(
            @PathVariable String id, 
            @PathVariable String subjectName) {
        SchoolClass updated = schoolClassService.removeSubject(id, subjectName);
        return ResponseEntity.ok(updated);
    }
}
