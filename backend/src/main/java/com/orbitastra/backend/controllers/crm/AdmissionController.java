package com.orbitastra.backend.controllers.crm;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.orbitastra.backend.models.crm.Admission;
import com.orbitastra.backend.models.crm.enums.AdmissionStatus;
import com.orbitastra.backend.models.student.Student;
import com.orbitastra.backend.services.crm.AdmissionService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/admissions")
@RequiredArgsConstructor
public class AdmissionController {

    private final AdmissionService admissionService;

    @PostMapping
    public ResponseEntity<Admission> createAdmission(@RequestBody Admission admission) {
        return new ResponseEntity<>(admissionService.createAdmission(admission), HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Admission> getAdmissionById(@PathVariable String id) {
        return ResponseEntity.ok(admissionService.getAdmissionById(id));
    }

    @GetMapping("/school/{schoolId}")
    public ResponseEntity<List<Admission>> getAdmissionsBySchool(@PathVariable String schoolId) {
        return ResponseEntity.ok(admissionService.getAdmissionsBySchool(schoolId));
    }

    @GetMapping("/school/{schoolId}/academic-year/{academicYear}")
    public ResponseEntity<List<Admission>> getAdmissionsBySchoolAndAcademicYear(
            @PathVariable String schoolId,
            @PathVariable String academicYear) {
        return ResponseEntity.ok(admissionService.getAdmissionsBySchoolAndAcademicYear(schoolId, academicYear));
    }

    @GetMapping("/school/{schoolId}/status/{status}")
    public ResponseEntity<List<Admission>> getAdmissionsBySchoolAndStatus(
            @PathVariable String schoolId,
            @PathVariable AdmissionStatus status) {
        return ResponseEntity.ok(admissionService.getAdmissionsBySchoolAndStatus(schoolId, status));
    }

    @GetMapping("/inquiry/{inquiryId}")
    public ResponseEntity<List<Admission>> getAdmissionsByInquiry(@PathVariable String inquiryId) {
        return ResponseEntity.ok(admissionService.getAdmissionsByInquiry(inquiryId));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<Admission> updateAdmission(@PathVariable String id, @RequestBody Admission details) {
        return ResponseEntity.ok(admissionService.updateAdmission(id, details));
    }

    @PostMapping("/{id}/convert")
    public ResponseEntity<Student> convertToStudent(@PathVariable String id, @RequestBody Student studentPayload) {
        return new ResponseEntity<>(admissionService.convertToStudent(id, studentPayload), HttpStatus.CREATED);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteAdmission(@PathVariable String id) {
        admissionService.deleteAdmission(id);
        return ResponseEntity.ok(Map.of("message", "Admission deleted successfully."));
    }
}
