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

import com.orbitastra.backend.dto.crm.ConvertAdmissionRequest;
import com.orbitastra.backend.dto.crm.CreateAdmissionRequest;
import com.orbitastra.backend.dto.crm.InquiryGuardianRequest;
import com.orbitastra.backend.dto.crm.UpdateAdmissionRequest;
import com.orbitastra.backend.models.crm.Admission;
import com.orbitastra.backend.models.crm.enums.AdmissionStatus;
import com.orbitastra.backend.dto.student.StudentResponse;
import com.orbitastra.backend.services.crm.AdmissionService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/admissions")
@RequiredArgsConstructor
public class AdmissionController {

    private final AdmissionService admissionService;

    @PostMapping
    public ResponseEntity<Admission> createAdmission(@Valid @RequestBody CreateAdmissionRequest request) {
        Admission admission = Admission.builder()
                .schoolId(request.getSchoolId())
                .inquiryId(request.getInquiryId())
                .studentName(request.getStudentName())
                .dob(request.getDob())
                .gender(request.getGender())
                .guardians(InquiryGuardianRequest.toModels(request.getGuardians()))
                .status(request.getStatus())
                .documents(request.getDocuments())
                .admissionDate(request.getAdmissionDate())
                .build();
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
    public ResponseEntity<Admission> updateAdmission(@PathVariable String id, @Valid @RequestBody UpdateAdmissionRequest request) {
        Admission details = Admission.builder()
                .status(request.getStatus())
                .documents(request.getDocuments())
                .admissionDate(request.getAdmissionDate())
                .inquiryId(request.getInquiryId())
                .studentName(request.getStudentName())
                .dob(request.getDob())
                .gender(request.getGender())
                .guardians(InquiryGuardianRequest.toModels(request.getGuardians()))
                .build();
        return ResponseEntity.ok(admissionService.updateAdmission(id, details));
    }

    @PostMapping("/{id}/convert")
    public ResponseEntity<StudentResponse> convertToStudent(@PathVariable String id, @Valid @RequestBody ConvertAdmissionRequest request) {
        StudentResponse created = admissionService.convertToStudent(
                id, request.toStudent(), request.toAcademicRecord());
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteAdmission(@PathVariable String id) {
        admissionService.deleteAdmission(id);
        return ResponseEntity.ok(Map.of("message", "Admission deleted successfully."));
    }
}
