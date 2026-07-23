package com.orbitastra.backend.controllers.student;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.orbitastra.backend.dto.crm.ConvertAdmissionRequest;
import com.orbitastra.backend.dto.student.AcademicRecordRequest;
import com.orbitastra.backend.dto.student.CreateStudentRequest;
import com.orbitastra.backend.dto.student.GuardianLinkRequest;
import com.orbitastra.backend.dto.student.StudentResponse;
import com.orbitastra.backend.dto.student.UpdateStudentRequest;
import com.orbitastra.backend.models.student.Student;
import com.orbitastra.backend.models.student.StudentAcademicRecord;
import com.orbitastra.backend.services.crm.AdmissionService;
import com.orbitastra.backend.services.student.StudentService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/students")
@RequiredArgsConstructor
public class StudentController {

    private final StudentService studentService;
    private final AdmissionService admissionService;

    /** Normal student creation API — creates a student directly from a request payload. */
    @PostMapping
    public ResponseEntity<StudentResponse> createStudent(@Valid @RequestBody CreateStudentRequest request) {
        StudentResponse created = studentService.createStudent(request);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }


    /** Create student from admission ID API — converts an admission into an enrolled student via request body. */
    @PostMapping("/from-admission")
    public ResponseEntity<StudentResponse> createStudentFromAdmissionBody(
            @Valid @RequestBody ConvertAdmissionRequest request) {
        if (request == null || request.getAdmissionId() == null || request.getAdmissionId().isBlank()) {
            throw new IllegalArgumentException("admissionId is required to create student from admission.");
        }
        StudentResponse created = admissionService.convertToStudent(
                request.getAdmissionId(), request.toStudent(), request.toAcademicRecord());
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    // ----- DTO -> model mapping helpers -----

    private static StudentAcademicRecord toAcademicRecord(AcademicRecordRequest r) {
        return r == null ? null : r.toModel();
    }

    @GetMapping
    public ResponseEntity<List<StudentResponse>> getAllStudents() {
        return ResponseEntity.ok(studentService.getAllStudents());
    }

    @GetMapping("/{id}")
    public ResponseEntity<StudentResponse> getStudentById(@PathVariable String id) {
        return ResponseEntity.ok(studentService.getStudentById(id));
    }

    @GetMapping("/admission/{admissionNo}")
    public ResponseEntity<StudentResponse> getStudentByAdmissionNo(@PathVariable String admissionNo) {
        return ResponseEntity.ok(studentService.getStudentByAdmissionNo(admissionNo));
    }

    @GetMapping("/school/{schoolId}")
    public ResponseEntity<List<StudentResponse>> getStudentsBySchool(@PathVariable String schoolId) {
        return ResponseEntity.ok(studentService.getStudentsBySchool(schoolId));
    }

    @GetMapping("/school/{schoolId}/academic-year/{academicYear}")
    public ResponseEntity<List<StudentResponse>> getStudentsBySchoolAndAcademicYear(
            @PathVariable String schoolId,
            @PathVariable String academicYear) {
        return ResponseEntity.ok(studentService.getStudentsBySchoolAndAcademicYear(schoolId, academicYear));
    }

    @GetMapping("/class/{classId}")
    public ResponseEntity<List<StudentResponse>> getStudentsByClass(@PathVariable String classId) {
        return ResponseEntity.ok(studentService.getStudentsByClass(classId));
    }

    @GetMapping("/guardian/{guardianDocsId}")
    public ResponseEntity<List<StudentResponse>> getStudentsByGuardian(@PathVariable String guardianDocsId) {
        return ResponseEntity.ok(studentService.getStudentsByGuardian(guardianDocsId));
    }

    @PostMapping("/{id}/guardians")
    public ResponseEntity<StudentResponse> addGuardianLink(@PathVariable String id, @Valid @RequestBody GuardianLinkRequest request) {
        return ResponseEntity.ok(studentService.addGuardianLink(id, request.toModel()));
    }

    @DeleteMapping("/{id}/guardians/{guardianDocsId}")
    public ResponseEntity<StudentResponse> removeGuardianLink(@PathVariable String id, @PathVariable String guardianDocsId) {
        return ResponseEntity.ok(studentService.removeGuardianLink(id, guardianDocsId));
    }

    @GetMapping("/hostel/{hostelRoomNo}")
    public ResponseEntity<List<StudentResponse>> getStudentsByHostelRoom(@PathVariable String hostelRoomNo) {
        return ResponseEntity.ok(studentService.getStudentsByHostelRoom(hostelRoomNo));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<StudentResponse> updateStudent(@PathVariable String id, @Valid @RequestBody UpdateStudentRequest request) {
        Student studentDetails = Student.builder()
                .admissionNo(request.getAdmissionNo())
                .name(request.getName())
                .dob(request.getDob())
                .gender(request.getGender())
                .bloodGroup(request.getBloodGroup())
                .photoUrl(request.getPhotoUrl())
                .walletDocsId(request.getWalletDocsId())
                .medicalRecordDocsId(request.getMedicalRecordDocsId())
                .documents(request.getDocuments())
                .medicalRemark(request.getMedicalRemark())
                .status(request.getStatus())
                .admissionDate(request.getAdmissionDate())
                .build();
        StudentResponse updated = studentService.updateStudent(
                id, studentDetails, toAcademicRecord(request.getCurrentAcademicRecord()));
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteStudent(@PathVariable String id) {
        studentService.deleteStudent(id);
        return ResponseEntity.ok(Map.of("message", "Student deleted successfully."));
    }

    @GetMapping("/{id}/academic-history")
    public ResponseEntity<List<StudentAcademicRecord>> getStudentAcademicHistory(@PathVariable String id) {
        List<StudentAcademicRecord> history = studentService.getAcademicHistory(id);
        return ResponseEntity.ok(history);
    }

    @PostMapping("/{id}/academic-records")
    public ResponseEntity<StudentAcademicRecord> assignAcademicRecord(
            @PathVariable String id,
            @Valid @RequestBody AcademicRecordRequest request) {
        StudentAcademicRecord created = studentService.createOrUpdateAcademicRecord(id, toAcademicRecord(request));
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @PostMapping("/{id}/promote")
    public ResponseEntity<StudentAcademicRecord> promoteStudent(
            @PathVariable String id,
            @Valid @RequestBody AcademicRecordRequest request) {
        StudentAcademicRecord promoted = studentService.promoteStudent(id, toAcademicRecord(request));
        return ResponseEntity.ok(promoted);
    }

    @GetMapping("/{id}/siblings")
    public ResponseEntity<List<StudentResponse>> getStudentSiblings(@PathVariable String id) {
        return ResponseEntity.ok(studentService.getSiblings(id));
    }
}
