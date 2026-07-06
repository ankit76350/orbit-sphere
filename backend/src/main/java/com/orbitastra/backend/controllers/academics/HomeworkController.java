package com.orbitastra.backend.controllers.academics;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.orbitastra.backend.models.academics.Homework;
import com.orbitastra.backend.models.academics.enums.AssignmentScope;
import com.orbitastra.backend.services.academics.HomeworkService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/homework")
@RequiredArgsConstructor
public class HomeworkController {

    private final HomeworkService homeworkService;

    @PostMapping
    public ResponseEntity<Homework> createHomework(@RequestBody Homework homework) {
        Homework created = homeworkService.createHomework(homework);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @PostMapping("/definition")
    public ResponseEntity<Homework> createHomeworkDefinition(@RequestBody Homework homework) {
        Homework created = homeworkService.createHomeworkDefinition(homework);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @PostMapping("/{id}/assign")
    public ResponseEntity<Homework> assignHomework(
            @PathVariable String id,
            @RequestBody Map<String, Object> payload) {
        String scopeStr = (String) payload.get("assignmentScope");
        AssignmentScope scope = AssignmentScope.valueOf(scopeStr.toUpperCase());

        List<Homework.StudentAssignment> studentAssignments = null;
        if (payload.containsKey("studentAssignments")) {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> list = (List<Map<String, Object>>) payload.get("studentAssignments");
            studentAssignments = new ArrayList<>();
            for (Map<String, Object> map : list) {
                Homework.StudentAssignment assignment = Homework.StudentAssignment.builder()
                        .studentId((String) map.get("studentId"))
                        .customInstructions((String) map.get("customInstructions"))
                        .build();
                studentAssignments.add(assignment);
            }
        }

        Homework updated = homeworkService.assignHomework(id, scope, studentAssignments);
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

    @GetMapping("/class/{classId}")
    public ResponseEntity<List<Homework>> getHomeworkByClass(@PathVariable String classId) {
        List<Homework> homeworkList = homeworkService.getHomeworkByClass(classId);
        return ResponseEntity.ok(homeworkList);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<Homework> updateHomework(@PathVariable String id, @RequestBody Homework homeworkDetails) {
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
            @RequestBody Map<String, String> submissionDetails) {
        String text = submissionDetails.get("submissionText");
        String fileUrl = submissionDetails.get("submissionFileUrl");
        Homework updated = homeworkService.submitHomework(id, studentId, text, fileUrl);
        return ResponseEntity.ok(updated);
    }

    @PostMapping("/{id}/grade/{studentId}")
    public ResponseEntity<Homework> gradeHomework(
            @PathVariable String id,
            @PathVariable String studentId,
            @RequestBody Map<String, Object> gradingDetails) {
        Integer obtainedMarks = (Integer) gradingDetails.get("obtainedMarks");
        String feedback = (String) gradingDetails.get("feedback");
        Homework updated = homeworkService.gradeHomework(id, studentId, obtainedMarks, feedback);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteHomework(@PathVariable String id) {
        homeworkService.deleteHomework(id);
        return ResponseEntity.ok(Map.of("message", "Homework deleted successfully."));
    }
}
