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
    public ResponseEntity<Student> createStudent(@Valid @RequestBody CreateStudentRequest request) {
        Student created = studentService.createStudent(request);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }


    /** Create student from admission ID API — converts an admission into an enrolled student via request body. */
    @PostMapping("/from-admission")
    public ResponseEntity<Student> createStudentFromAdmissionBody(
            @Valid @RequestBody ConvertAdmissionRequest request) {
        if (request == null || request.getAdmissionId() == null || request.getAdmissionId().isBlank()) {
            throw new IllegalArgumentException("admissionId is required to create student from admission.");
        }
        Student created = admissionService.convertToStudent(request.getAdmissionId(), request.toStudent());
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    // ----- DTO -> model mapping helpers -----

    private static StudentAcademicRecord toAcademicRecord(AcademicRecordRequest r) {
        return r == null ? null : r.toModel();
    }

    @GetMapping
    public ResponseEntity<List<Student>> getAllStudents() {
        List<Student> students = studentService.getAllStudents();
        return ResponseEntity.ok(students);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Student> getStudentById(@PathVariable String id) {
        Student student = studentService.getStudentById(id);
        return ResponseEntity.ok(student);
    }

    @GetMapping("/admission/{admissionNo}")
    public ResponseEntity<Student> getStudentByAdmissionNo(@PathVariable String admissionNo) {
        Student student = studentService.getStudentByAdmissionNo(admissionNo);
        return ResponseEntity.ok(student);
    }

    @GetMapping("/school/{schoolId}")
    public ResponseEntity<List<Student>> getStudentsBySchool(@PathVariable String schoolId) {
        List<Student> students = studentService.getStudentsBySchool(schoolId);
        return ResponseEntity.ok(students);
    }

    @GetMapping("/school/{schoolId}/academic-year/{academicYear}")
    public ResponseEntity<List<Student>> getStudentsBySchoolAndAcademicYear(
            @PathVariable String schoolId,
            @PathVariable String academicYear) {
        List<Student> students = studentService.getStudentsBySchoolAndAcademicYear(schoolId, academicYear);
        return ResponseEntity.ok(students);
    }

    @GetMapping("/class/{classId}")
    public ResponseEntity<List<Student>> getStudentsByClass(@PathVariable String classId) {
        List<Student> students = studentService.getStudentsByClass(classId);
        return ResponseEntity.ok(students);
    }

    @GetMapping("/guardian/{guardianId}")
    public ResponseEntity<List<Student>> getStudentsByGuardian(@PathVariable String guardianId) {
        return ResponseEntity.ok(studentService.getStudentsByGuardian(guardianId));
    }

    @PostMapping("/{id}/guardians")
    public ResponseEntity<Student> addGuardianLink(@PathVariable String id, @Valid @RequestBody GuardianLinkRequest request) {
        return ResponseEntity.ok(studentService.addGuardianLink(id, request.toModel()));
    }

    @DeleteMapping("/{id}/guardians/{guardianId}")
    public ResponseEntity<Student> removeGuardianLink(@PathVariable String id, @PathVariable String guardianId) {
        return ResponseEntity.ok(studentService.removeGuardianLink(id, guardianId));
    }

    @GetMapping("/hostel/{hostelRoomId}")
    public ResponseEntity<List<Student>> getStudentsByHostelRoom(@PathVariable String hostelRoomId) {
        List<Student> students = studentService.getStudentsByHostelRoom(hostelRoomId);
        return ResponseEntity.ok(students);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<Student> updateStudent(@PathVariable String id, @Valid @RequestBody UpdateStudentRequest request) {
        Student studentDetails = Student.builder()
                .admissionNo(request.getAdmissionNo())
                .name(request.getName())
                .dob(request.getDob())
                .gender(request.getGender())
                .bloodGroup(request.getBloodGroup())
                .photoUrl(request.getPhotoUrl())
                .walletId(request.getWalletId())
                .medicalRecordId(request.getMedicalRecordId())
                .status(request.getStatus())
                .admissionDate(request.getAdmissionDate())
                .currentAcademicRecord(toAcademicRecord(request.getCurrentAcademicRecord()))
                .build();
        Student updated = studentService.updateStudent(id, studentDetails);
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
    public ResponseEntity<List<Student>> getStudentSiblings(@PathVariable String id) {
        List<Student> siblings = studentService.getSiblings(id);
        return ResponseEntity.ok(siblings);
    }
}
