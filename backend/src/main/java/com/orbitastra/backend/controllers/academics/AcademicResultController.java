package com.orbitastra.backend.controllers.academics;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.orbitastra.backend.dto.academics.CreateAcademicResultRequest;
import com.orbitastra.backend.dto.academics.UpdateAcademicResultRequest;
import com.orbitastra.backend.models.academics.AcademicResult;
import com.orbitastra.backend.services.academics.AcademicResultService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/academic-results")
@RequiredArgsConstructor
public class AcademicResultController {

    private final AcademicResultService academicResultService;

    @PostMapping
    public ResponseEntity<AcademicResult> createAcademicResult(@Valid @RequestBody CreateAcademicResultRequest request) {
        AcademicResult academicResult = AcademicResult.builder()
                .schoolId(request.getSchoolId())
                .academicYear(request.getAcademicYear())
                .studentId(request.getStudentId())
                .grade(request.getGrade())
                .examName(request.getExamName())
                .marks(request.getMarks())
                .totalPercentage(request.getTotalPercentage())
                .overallGrade(request.getOverallGrade())
                .feedback(request.getFeedback())
                .build();
        AcademicResult created = academicResultService.createAcademicResult(academicResult);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<AcademicResult>> getAllAcademicResults() {
        List<AcademicResult> results = academicResultService.getAllAcademicResults();
        return ResponseEntity.ok(results);
    }

    @GetMapping("/{id}")
    public ResponseEntity<AcademicResult> getAcademicResultById(@PathVariable String id) {
        AcademicResult result = academicResultService.getAcademicResultById(id);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/school/{schoolId}")
    public ResponseEntity<List<AcademicResult>> getAcademicResultsBySchool(@PathVariable String schoolId) {
        List<AcademicResult> results = academicResultService.getAcademicResultsBySchool(schoolId);
        return ResponseEntity.ok(results);
    }

    @GetMapping("/school/{schoolId}/academic-year/{academicYear}")
    public ResponseEntity<List<AcademicResult>> getAcademicResultsBySchoolAndAcademicYear(
            @PathVariable String schoolId,
            @PathVariable String academicYear) {
        return ResponseEntity.ok(academicResultService.getAcademicResultsBySchoolAndAcademicYear(schoolId, academicYear));
    }

    @GetMapping("/student/{studentId}")
    public ResponseEntity<List<AcademicResult>> getAcademicResultsByStudent(@PathVariable String studentId) {
        List<AcademicResult> results = academicResultService.getAcademicResultsByStudent(studentId);
        return ResponseEntity.ok(results);
    }

    @GetMapping("/school/{schoolId}/student/{studentId}")
    public ResponseEntity<List<AcademicResult>> getAcademicResultsBySchoolAndStudent(
            @PathVariable String schoolId, 
            @PathVariable String studentId) {
        List<AcademicResult> results = academicResultService.getAcademicResultsBySchoolAndStudent(schoolId, studentId);
        return ResponseEntity.ok(results);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<AcademicResult> updateAcademicResult(
            @PathVariable String id,
            @Valid @RequestBody UpdateAcademicResultRequest request) {
        AcademicResult resultDetails = AcademicResult.builder()
                .schoolId(request.getSchoolId())
                .academicYear(request.getAcademicYear())
                .studentId(request.getStudentId())
                .grade(request.getGrade())
                .examName(request.getExamName())
                .marks(request.getMarks())
                .totalPercentage(request.getTotalPercentage())
                .overallGrade(request.getOverallGrade())
                .feedback(request.getFeedback())
                .build();
        AcademicResult updated = academicResultService.updateAcademicResult(id, resultDetails);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteAcademicResult(@PathVariable String id) {
        academicResultService.deleteAcademicResult(id);
        return ResponseEntity.ok(Map.of("message", "Academic result deleted successfully."));
    }
}
