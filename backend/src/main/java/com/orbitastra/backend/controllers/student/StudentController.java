package com.orbitastra.backend.controllers.student;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.orbitastra.backend.models.student.GuardianLink;
import com.orbitastra.backend.models.student.Student;
import com.orbitastra.backend.models.student.StudentAcademicRecord;
import com.orbitastra.backend.services.student.StudentService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/students")
@RequiredArgsConstructor
public class StudentController {

    private final StudentService studentService;

    @PostMapping
    public ResponseEntity<Student> createStudent(@RequestBody Student student) {
        Student created = studentService.createStudent(student);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
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
    public ResponseEntity<Student> addGuardianLink(@PathVariable String id, @RequestBody GuardianLink link) {
        return ResponseEntity.ok(studentService.addGuardianLink(id, link));
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
    public ResponseEntity<Student> updateStudent(@PathVariable String id, @RequestBody Student studentDetails) {
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
            @RequestBody StudentAcademicRecord recordDetails) {
        StudentAcademicRecord created = studentService.createOrUpdateAcademicRecord(id, recordDetails);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @PostMapping("/{id}/promote")
    public ResponseEntity<StudentAcademicRecord> promoteStudent(
            @PathVariable String id, 
            @RequestBody StudentAcademicRecord promotionDetails) {
        StudentAcademicRecord promoted = studentService.promoteStudent(id, promotionDetails);
        return ResponseEntity.ok(promoted);
    }

    @GetMapping("/{id}/siblings")
    public ResponseEntity<List<Student>> getStudentSiblings(@PathVariable String id) {
        List<Student> siblings = studentService.getSiblings(id);
        return ResponseEntity.ok(siblings);
    }
}
