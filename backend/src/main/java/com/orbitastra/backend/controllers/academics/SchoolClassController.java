package com.orbitastra.backend.controllers.academics;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.orbitastra.backend.dto.academics.CreateSchoolClassRequest;
import com.orbitastra.backend.dto.academics.AddClassSubjectRequest;
import com.orbitastra.backend.dto.academics.UpdateSchoolClassRequest;
import com.orbitastra.backend.models.academics.SchoolClass;
import com.orbitastra.backend.services.academics.SchoolClassService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/classes")
@RequiredArgsConstructor
public class SchoolClassController {

    private final SchoolClassService schoolClassService;

    @PostMapping
    public ResponseEntity<SchoolClass> createClass(@Valid @RequestBody CreateSchoolClassRequest request) {
        SchoolClass schoolClass = SchoolClass.builder()
                .schoolId(request.getSchoolId())
                .name(request.getName())
                .classTeacherDocsId(request.getClassTeacherDocsId())
                .subjects(request.getSubjects())
                .academicYear(request.getAcademicYear())
                .sections(request.getSections())
                .build();
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

    @GetMapping("/school/{schoolId}/academic-year/{academicYear}")
    public ResponseEntity<List<SchoolClass>> getClassesBySchoolAndAcademicYear(
            @PathVariable String schoolId,
            @PathVariable String academicYear) {
        return ResponseEntity.ok(schoolClassService.getClassesBySchoolAndAcademicYear(schoolId, academicYear));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<SchoolClass> updateClass(@PathVariable String id, @Valid @RequestBody UpdateSchoolClassRequest request) {
        SchoolClass classDetails = SchoolClass.builder()
                .schoolId(request.getSchoolId())
                .name(request.getName())
                .classTeacherDocsId(request.getClassTeacherDocsId())
                .subjects(request.getSubjects())
                .academicYear(request.getAcademicYear())
                .sections(request.getSections())
                .build();
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
            @Valid @RequestBody AddClassSubjectRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Subject request is required.");
        }
        SchoolClass.ClassSubject subject = SchoolClass.ClassSubject.builder()
                .name(request.getName())
                .teacherDocsId(request.getTeacherDocsId())
                .build();
        SchoolClass updated = schoolClassService.addSubject(id, subject);
        return ResponseEntity.ok(updated);
    }

    @PostMapping("/{id}/sections")
    public ResponseEntity<SchoolClass> addSection(
            @PathVariable String id, 
            @RequestBody Map<String, List<String>> body) {
        List<String> sections = body.get("section");
        if (sections == null || sections.isEmpty()) {
            throw new IllegalArgumentException("Section list cannot be null or empty.");
        }
        SchoolClass updated = schoolClassService.addSections(id, sections);
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
