package com.orbitastra.backend.controllers.academics;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.orbitastra.backend.dto.academics.AssignHomeworkRequest;
import com.orbitastra.backend.dto.academics.CreateHomeworkRequest;
import com.orbitastra.backend.dto.academics.GradeHomeworkRequest;
import com.orbitastra.backend.dto.academics.StudentAssignmentRequest;
import com.orbitastra.backend.dto.academics.SubmitHomeworkRequest;
import com.orbitastra.backend.dto.academics.UpdateHomeworkRequest;
import com.orbitastra.backend.models.academics.Homework;
import com.orbitastra.backend.services.academics.HomeworkService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/homework")
@RequiredArgsConstructor
public class HomeworkController {

    private final HomeworkService homeworkService;

    @PostMapping
    public ResponseEntity<Homework> createHomework(@Valid @RequestBody CreateHomeworkRequest request) {
        Homework created = homeworkService.createHomework(toHomework(request));
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @PostMapping("/definition")
    public ResponseEntity<Homework> createHomeworkDefinition(@Valid @RequestBody CreateHomeworkRequest request) {
        Homework created = homeworkService.createHomeworkDefinition(toHomework(request));
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    private static Homework toHomework(CreateHomeworkRequest request) {
        return Homework.builder()
                .schoolId(request.getSchoolId())
                .classDocsId(request.getClassDocsId())
                .sectionNo(request.getSectionNo())
                .subject(request.getSubject())
                .title(request.getTitle())
                .instructions(request.getInstructions())
                .dueDate(request.getDueDate())
                .assignmentScope(request.getAssignmentScope())
                .maxMarks(request.getMaxMarks())
                .teacherDocsId(request.getTeacherDocsId())
                .studentAssignments(StudentAssignmentRequest.toModels(request.getStudentAssignments()))
                .build();
    }

    @PostMapping("/{id}/assign")
    public ResponseEntity<Homework> assignHomework(
            @PathVariable String id,
            @Valid @RequestBody AssignHomeworkRequest request) {
        Homework updated = homeworkService.assignHomework(
                id, request.getAssignmentScope(),
                StudentAssignmentRequest.toModels(request.getStudentAssignments()));
        return ResponseEntity.ok(updated);
    }

    @GetMapping
    public ResponseEntity<List<Homework>> getAllHomework() {
        List<Homework> homeworkList = homeworkService.getAllHomework();
        return ResponseEntity.ok(homeworkList);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Homework> getHomeworkById(@PathVariable String id) {
        Homework homework = homeworkService.getHomeworkById(id);
        return ResponseEntity.ok(homework);
    }

    @GetMapping("/school/{schoolId}")
    public ResponseEntity<List<Homework>> getHomeworkBySchool(@PathVariable String schoolId) {
        List<Homework> homeworkList = homeworkService.getHomeworkBySchool(schoolId);
        return ResponseEntity.ok(homeworkList);
    }

    @GetMapping("/school/{schoolId}/academic-year/{academicYear}")
    public ResponseEntity<List<Homework>> getHomeworkBySchoolAndAcademicYear(
            @PathVariable String schoolId,
            @PathVariable String academicYear) {
        return ResponseEntity.ok(homeworkService.getHomeworkBySchoolAndAcademicYear(schoolId, academicYear));
    }

    @GetMapping("/class/{classDocsId}")
    public ResponseEntity<List<Homework>> getHomeworkByClass(@PathVariable String classDocsId) {
        List<Homework> homeworkList = homeworkService.getHomeworkByClass(classDocsId);
        return ResponseEntity.ok(homeworkList);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<Homework> updateHomework(@PathVariable String id, @Valid @RequestBody UpdateHomeworkRequest request) {
        Homework homeworkDetails = Homework.builder()
                .schoolId(request.getSchoolId())
                .classDocsId(request.getClassDocsId())
                .sectionNo(request.getSectionNo())
                .subject(request.getSubject())
                .title(request.getTitle())
                .instructions(request.getInstructions())
                .dueDate(request.getDueDate())
                .assignmentScope(request.getAssignmentScope())
                .maxMarks(request.getMaxMarks())
                .teacherDocsId(request.getTeacherDocsId())
                .build();
        Homework updated = homeworkService.updateHomework(id, homeworkDetails);
        return ResponseEntity.ok(updated);
    }

    @GetMapping("/student/{studentId}")
    public ResponseEntity<List<Homework>> getHomeworkByStudent(@PathVariable String studentId) {
        List<Homework> homeworkList = homeworkService.getHomeworkByStudent(studentId);
        return ResponseEntity.ok(homeworkList);
    }

    @GetMapping("/school/{schoolId}/student/{studentId}")
    public ResponseEntity<List<Homework>> getHomeworkBySchoolAndStudent(
            @PathVariable String schoolId,
            @PathVariable String studentId) {
        List<Homework> homeworkList = homeworkService.getHomeworkBySchoolAndStudent(schoolId, studentId);
        return ResponseEntity.ok(homeworkList);
    }

    @PostMapping("/{id}/submit/{studentId}")
    public ResponseEntity<Homework> submitHomework(
            @PathVariable String id,
            @PathVariable String studentId,
            @Valid @RequestBody SubmitHomeworkRequest request) {
        Homework updated = homeworkService.submitHomework(
                id, studentId, request.getSubmissionText(), request.getSubmissionFileUrl());
        return ResponseEntity.ok(updated);
    }

    @PostMapping("/{id}/grade/{studentId}")
    public ResponseEntity<Homework> gradeHomework(
            @PathVariable String id,
            @PathVariable String studentId,
            @Valid @RequestBody GradeHomeworkRequest request) {
        Homework updated = homeworkService.gradeHomework(
                id, studentId, request.getObtainedMarks(), request.getFeedback());
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteHomework(@PathVariable String id) {
        homeworkService.deleteHomework(id);
        return ResponseEntity.ok(Map.of("message", "Homework deleted successfully."));
    }
}
