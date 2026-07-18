package com.orbitastra.backend.controllers.academics;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.orbitastra.backend.dto.academics.CreateAttendanceRequest;
import com.orbitastra.backend.dto.academics.UpdateAttendanceRequest;
import com.orbitastra.backend.models.academics.Attendance;
import com.orbitastra.backend.services.academics.AttendanceService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/attendance")
@RequiredArgsConstructor
public class AttendanceController {

    private final AttendanceService attendanceService;

    @PostMapping
    public ResponseEntity<Attendance> createAttendance(@Valid @RequestBody CreateAttendanceRequest request) {
        Attendance attendance = Attendance.builder()
                .schoolId(request.getSchoolId())
                .academicYear(request.getAcademicYear())
                .studentId(request.getStudentId())
                .date(request.getDate())
                .status(request.getStatus())
                .presentBy(request.getPresentBy())
                .presentTime(request.getPresentTime())
                .build();
        Attendance created = attendanceService.createAttendance(attendance);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<Attendance>> getAllAttendance() {
        List<Attendance> attendanceList = attendanceService.getAllAttendance();
        return ResponseEntity.ok(attendanceList);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Attendance> getAttendanceById(@PathVariable String id) {
        Attendance attendance = attendanceService.getAttendanceById(id);
        return ResponseEntity.ok(attendance);
    }

    @GetMapping("/school/{schoolId}")
    public ResponseEntity<List<Attendance>> getAttendanceBySchool(@PathVariable String schoolId) {
        List<Attendance> attendanceList = attendanceService.getAttendanceBySchool(schoolId);
        return ResponseEntity.ok(attendanceList);
    }

    @GetMapping("/school/{schoolId}/academic-year/{academicYear}")
    public ResponseEntity<List<Attendance>> getAttendanceBySchoolAndAcademicYear(
            @PathVariable String schoolId,
            @PathVariable String academicYear) {
        return ResponseEntity.ok(attendanceService.getAttendanceBySchoolAndAcademicYear(schoolId, academicYear));
    }

    @GetMapping("/student/{studentId}")
    public ResponseEntity<List<Attendance>> getAttendanceByStudent(@PathVariable String studentId) {
        List<Attendance> attendanceList = attendanceService.getAttendanceByStudent(studentId);
        return ResponseEntity.ok(attendanceList);
    }

    @GetMapping("/school/{schoolId}/date/{date}")
    public ResponseEntity<List<Attendance>> getAttendanceBySchoolAndDate(
            @PathVariable String schoolId, 
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        List<Attendance> attendanceList = attendanceService.getAttendanceBySchoolAndDate(schoolId, date);
        return ResponseEntity.ok(attendanceList);
    }

    @GetMapping("/student/{studentId}/date/{date}")
    public ResponseEntity<List<Attendance>> getAttendanceByStudentAndDate(
            @PathVariable String studentId, 
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        List<Attendance> attendanceList = attendanceService.getAttendanceByStudentAndDate(studentId, date);
        return ResponseEntity.ok(attendanceList);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<Attendance> updateAttendance(
            @PathVariable String id,
            @Valid @RequestBody UpdateAttendanceRequest request) {
        Attendance attendanceDetails = Attendance.builder()
                .schoolId(request.getSchoolId())
                .academicYear(request.getAcademicYear())
                .studentId(request.getStudentId())
                .date(request.getDate())
                .status(request.getStatus())
                .presentBy(request.getPresentBy())
                .presentTime(request.getPresentTime())
                .build();
        Attendance updated = attendanceService.updateAttendance(id, attendanceDetails);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteAttendance(@PathVariable String id) {
        attendanceService.deleteAttendance(id);
        return ResponseEntity.ok(Map.of("message", "Attendance record deleted successfully."));
    }
}
