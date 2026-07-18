package com.orbitastra.backend.controllers.academics;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.orbitastra.backend.dto.academics.CreateMedicalRecordRequest;
import com.orbitastra.backend.dto.academics.UpdateMedicalRecordRequest;
import com.orbitastra.backend.models.academics.MedicalRecord;
import com.orbitastra.backend.services.academics.MedicalRecordService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/medical-records")
@RequiredArgsConstructor
public class MedicalRecordController {

    private final MedicalRecordService medicalRecordService;

    @PostMapping
    public ResponseEntity<MedicalRecord> createMedicalRecord(@Valid @RequestBody CreateMedicalRecordRequest request) {
        MedicalRecord record = MedicalRecord.builder()
                .schoolId(request.getSchoolId())
                .studentId(request.getStudentId())
                .visitDate(request.getVisitDate())
                .diagnosis(request.getDiagnosis())
                .medicines(request.getMedicines())
                .doctorName(request.getDoctorName())
                .build();
        MedicalRecord created = medicalRecordService.createMedicalRecord(record);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<MedicalRecord>> getAllMedicalRecords() {
        List<MedicalRecord> records = medicalRecordService.getAllMedicalRecords();
        return ResponseEntity.ok(records);
    }

    @GetMapping("/{id}")
    public ResponseEntity<MedicalRecord> getMedicalRecordById(@PathVariable String id) {
        MedicalRecord record = medicalRecordService.getMedicalRecordById(id);
        return ResponseEntity.ok(record);
    }

    @GetMapping("/school/{schoolId}")
    public ResponseEntity<List<MedicalRecord>> getMedicalRecordsBySchool(@PathVariable String schoolId) {
        List<MedicalRecord> records = medicalRecordService.getMedicalRecordsBySchool(schoolId);
        return ResponseEntity.ok(records);
    }

    @GetMapping("/student/{studentId}")
    public ResponseEntity<List<MedicalRecord>> getMedicalRecordsByStudent(@PathVariable String studentId) {
        List<MedicalRecord> records = medicalRecordService.getMedicalRecordsByStudent(studentId);
        return ResponseEntity.ok(records);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<MedicalRecord> updateMedicalRecord(
            @PathVariable String id,
            @Valid @RequestBody UpdateMedicalRecordRequest request) {
        MedicalRecord recordDetails = MedicalRecord.builder()
                .schoolId(request.getSchoolId())
                .studentId(request.getStudentId())
                .visitDate(request.getVisitDate())
                .diagnosis(request.getDiagnosis())
                .medicines(request.getMedicines())
                .doctorName(request.getDoctorName())
                .build();
        MedicalRecord updated = medicalRecordService.updateMedicalRecord(id, recordDetails);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteMedicalRecord(@PathVariable String id) {
        medicalRecordService.deleteMedicalRecord(id);
        return ResponseEntity.ok(Map.of("message", "Medical record deleted successfully."));
    }
}
