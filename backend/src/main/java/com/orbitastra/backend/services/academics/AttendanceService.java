package com.orbitastra.backend.services.academics;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;

import com.orbitastra.backend.exceptions.ResourceNotFoundException;
import com.orbitastra.backend.models.academics.Attendance;
import com.orbitastra.backend.models.student.Student;
import com.orbitastra.backend.repositories.academics.AttendanceRepository;
import com.orbitastra.backend.repositories.core.SchoolRepository;
import com.orbitastra.backend.repositories.student.StudentRepository;
import com.orbitastra.backend.repositories.staff.StaffRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AttendanceService {

    private final AttendanceRepository attendanceRepository;
    private final SchoolRepository schoolRepository;
    private final StudentRepository studentRepository;
    private final StaffRepository staffRepository;

    private void validatePresenter(String presentById, String schoolId) {
        if (presentById == null || presentById.isEmpty()) {
            throw new IllegalArgumentException("Presenter (teacher/staff) ID cannot be null or empty.");
        }
        com.orbitastra.backend.models.staff.Staff teacher = staffRepository.findById(presentById)
                .orElseThrow(() -> new ResourceNotFoundException("Staff/Teacher not found with id: " + presentById));
        if (!teacher.getSchoolId().equals(schoolId)) {
            throw new IllegalArgumentException("Staff/Teacher does not belong to the same school as the attendance record.");
        }
    }

    private void validateStudent(String studentId, String schoolId) {
        if (studentId == null || studentId.isEmpty()) {
            throw new IllegalArgumentException("Student ID cannot be null or empty.");
        }
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found with id: " + studentId));
        if (!student.getSchoolId().equals(schoolId)) {
            throw new IllegalArgumentException("Student does not belong to the same school as the attendance record.");
        }
    }

    public Attendance createAttendance(Attendance attendance) {
        if (attendance.getSchoolId() == null || !schoolRepository.existsById(attendance.getSchoolId())) {
            throw new ResourceNotFoundException("School not found with id: " + attendance.getSchoolId());
        }

        validateStudent(attendance.getStudentId(), attendance.getSchoolId());

        validatePresenter(attendance.getPresentBy(), attendance.getSchoolId());
        attendance.setPresentTime(java.time.LocalDateTime.now());

        return attendanceRepository.save(attendance);
    }

    public List<Attendance> getAllAttendance() {
        return attendanceRepository.findAll();
    }

    public Attendance getAttendanceById(String id) {
        return attendanceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Attendance record not found with id: " + id));
    }

    public List<Attendance> getAttendanceBySchool(String schoolId) {
        return attendanceRepository.findBySchoolId(schoolId);
    }

    public List<Attendance> getAttendanceByStudent(String studentId) {
        return attendanceRepository.findByStudentId(studentId);
    }

    public List<Attendance> getAttendanceBySchoolAndDate(String schoolId, LocalDate date) {
        return attendanceRepository.findBySchoolIdAndDate(schoolId, date);
    }

    public List<Attendance> getAttendanceByStudentAndDate(String studentId, LocalDate date) {
        return attendanceRepository.findByStudentIdAndDate(studentId, date);
    }

    public Attendance updateAttendance(String id, Attendance attendanceDetails) {
        Attendance attendance = getAttendanceById(id);

        if (attendanceDetails.getSchoolId() != null && !attendanceDetails.getSchoolId().equals(attendance.getSchoolId())) {
            if (!schoolRepository.existsById(attendanceDetails.getSchoolId())) {
                throw new ResourceNotFoundException("School not found with id: " + attendanceDetails.getSchoolId());
            }
            attendance.setSchoolId(attendanceDetails.getSchoolId());
        }

        if (attendanceDetails.getStudentId() != null && !attendanceDetails.getStudentId().equals(attendance.getStudentId())) {
            validateStudent(attendanceDetails.getStudentId(), attendance.getSchoolId());
            attendance.setStudentId(attendanceDetails.getStudentId());
        }

        boolean updatedInfo = false;
        if (attendanceDetails.getDate() != null) {
            attendance.setDate(attendanceDetails.getDate());
        }
        if (attendanceDetails.getStatus() != null && !attendanceDetails.getStatus().equals(attendance.getStatus())) {
            attendance.setStatus(attendanceDetails.getStatus());
            updatedInfo = true;
        }
        if (attendanceDetails.getPresentBy() != null && !attendanceDetails.getPresentBy().equals(attendance.getPresentBy())) {
            validatePresenter(attendanceDetails.getPresentBy(), attendance.getSchoolId());
            attendance.setPresentBy(attendanceDetails.getPresentBy());
            updatedInfo = true;
        }
        if (updatedInfo) {
            attendance.setPresentTime(java.time.LocalDateTime.now());
        }

        return attendanceRepository.save(attendance);
    }

    public void deleteAttendance(String id) {
        Attendance attendance = getAttendanceById(id);
        attendanceRepository.delete(attendance);
    }
}
