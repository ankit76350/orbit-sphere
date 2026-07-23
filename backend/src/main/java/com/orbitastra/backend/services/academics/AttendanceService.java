package com.orbitastra.backend.services.academics;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Service;

import com.orbitastra.backend.exceptions.ResourceNotFoundException;
import com.orbitastra.backend.models.academics.Attendance;
import com.orbitastra.backend.models.core.AcademicYear;
import com.orbitastra.backend.models.staff.Staff;
import com.orbitastra.backend.models.student.Student;
import com.orbitastra.backend.models.student.StudentAcademicRecord;
import com.orbitastra.backend.repositories.academics.AttendanceRepository;
import com.orbitastra.backend.repositories.core.SchoolRepository;
import com.orbitastra.backend.repositories.staff.StaffRepository;
import com.orbitastra.backend.repositories.student.StudentAcademicRecordRepository;
import com.orbitastra.backend.services.utils.AcademicYearResolver;
import com.orbitastra.backend.services.utils.StudentValidator;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AttendanceService {

    private final AttendanceRepository attendanceRepository;
    private final SchoolRepository schoolRepository;
    private final StudentValidator studentValidator;
    private final StaffRepository staffRepository;
    private final StudentAcademicRecordRepository studentAcademicRecordRepository;
    private final AcademicYearResolver academicYearResolver;

    private static boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private void validateSchool(String schoolId) {
        if (!hasText(schoolId) || !schoolRepository.existsById(schoolId)) {
            throw new ResourceNotFoundException("School not found with id: " + schoolId);
        }
    }

    private Staff validatePresenter(String presentById, String schoolId) {
        if (!hasText(presentById)) {
            throw new IllegalArgumentException("Presenter (teacher/staff) ID cannot be null or empty.");
        }
        Staff presenter = staffRepository.findById(presentById)
                .orElseThrow(() -> new ResourceNotFoundException("Staff/Teacher not found with id: " + presentById));
        if (!Objects.equals(presenter.getSchoolId(), schoolId)) {
            throw new IllegalArgumentException("Staff/Teacher does not belong to the same school as the attendance record.");
        }
        return presenter;
    }

    private record CurrentAcademicRecord(Student student, StudentAcademicRecord record) {
    }

    /**
     * Resolves the student's denormalised current-record pointer and verifies
     * that the pointed document still belongs to the same student and school.
     * Attendance never trusts a client-supplied academic-year name to choose a
     * record.
     */
    private CurrentAcademicRecord resolveCurrentAcademicRecord(String studentId, String schoolId) {
        Student student = studentValidator.validateStudent(studentId, schoolId);
        if (student == null || !hasText(student.getCurrentAcademicRecordDocsId())) {
            throw new IllegalArgumentException("Student " + studentId
                    + " does not have a current academic record.");
        }

        String recordId = student.getCurrentAcademicRecordDocsId();
        StudentAcademicRecord record = studentAcademicRecordRepository.findById(recordId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Current academic record not found with id: " + recordId));

        if (!Objects.equals(record.getStudentDocId(), studentId)
                || !Objects.equals(record.getSchoolId(), schoolId)
                || !hasText(record.getAcademicYear())) {
            throw new IllegalArgumentException("Current academic record " + recordId
                    + " is not a valid record for student " + studentId + " in this school.");
        }
        return new CurrentAcademicRecord(student, record);
    }

    private AcademicYear validateDateInAcademicYear(String schoolId, String academicYear, LocalDate date) {
        if (date == null) {
            throw new IllegalArgumentException("Attendance date is required.");
        }
        if (!hasText(academicYear)) {
            throw new IllegalArgumentException("The current academic record has no academic year.");
        }

        AcademicYear year = academicYearResolver.byName(schoolId, academicYear);
        if (year.getStartDate() == null || year.getEndDate() == null
                || year.getStartDate().isAfter(year.getEndDate())) {
            throw new IllegalArgumentException("Academic year '" + academicYear
                    + "' does not have a valid date range.");
        }
        if (date.isBefore(year.getStartDate()) || date.isAfter(year.getEndDate())) {
            throw new IllegalArgumentException("Attendance date " + date
                    + " is outside academic year '" + year.getName() + "', which runs "
                    + year.getStartDate() + " to " + year.getEndDate() + ".");
        }
        return year;
    }

    private CurrentAcademicRecord validateTarget(String schoolId, String studentId,
            String suppliedAcademicYear, LocalDate date) {
        CurrentAcademicRecord current = resolveCurrentAcademicRecord(studentId, schoolId);
        String currentAcademicYear = current.record().getAcademicYear();

        if (hasText(suppliedAcademicYear)
                && !Objects.equals(suppliedAcademicYear.trim(), currentAcademicYear)) {
            throw new IllegalArgumentException("academicYear must match the student's current academic year '"
                    + currentAcademicYear + "'.");
        }
        validateDateInAcademicYear(schoolId, currentAcademicYear, date);
        return current;
    }

    private void rejectDuplicate(String studentId, LocalDate date, String attendanceId) {
        boolean exists = attendanceId == null
                ? attendanceRepository.existsByStudentIdAndDate(studentId, date)
                : attendanceRepository.existsByStudentIdAndDateAndIdNot(studentId, date, attendanceId);
        if (exists) {
            throw new IllegalArgumentException("Attendance already exists for student " + studentId
                    + " on " + date + ". Update the existing record instead.");
        }
    }

    public Attendance createAttendance(Attendance attendance) {
        if (attendance == null) {
            throw new IllegalArgumentException("Attendance details are required.");
        }
        validateSchool(attendance.getSchoolId());
        if (attendance.getStatus() == null) {
            throw new IllegalArgumentException("Attendance status is required.");
        }

        CurrentAcademicRecord current = validateTarget(attendance.getSchoolId(), attendance.getStudentId(),
                attendance.getAcademicYear(), attendance.getDate());
        validatePresenter(attendance.getPresentBy(), attendance.getSchoolId());
        rejectDuplicate(attendance.getStudentId(), attendance.getDate(), null);

        attendance.setAcademicYear(current.record().getAcademicYear());
        attendance.setCurrentAcademicRecordDocsId(current.student().getCurrentAcademicRecordDocsId());
        attendance.setPresentTime(LocalDateTime.now());
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

    public List<Attendance> getAttendanceBySchoolAndAcademicYear(String schoolId, String academicYear) {
        return attendanceRepository.findBySchoolIdAndAcademicYear(schoolId, academicYear);
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
        if (attendanceDetails == null) {
            throw new IllegalArgumentException("Attendance details are required.");
        }
        Attendance attendance = getAttendanceById(id);

        String targetSchoolId = hasText(attendanceDetails.getSchoolId())
                ? attendanceDetails.getSchoolId() : attendance.getSchoolId();
        String targetStudentId = hasText(attendanceDetails.getStudentId())
                ? attendanceDetails.getStudentId() : attendance.getStudentId();
        LocalDate targetDate = attendanceDetails.getDate() != null
                ? attendanceDetails.getDate() : attendance.getDate();

        if (hasText(attendanceDetails.getAcademicYear())) {
            academicYearResolver.assertImmutable(attendance.getAcademicYear(), attendanceDetails.getAcademicYear());
        }

        CurrentAcademicRecord current = validateTarget(targetSchoolId, targetStudentId,
                attendanceDetails.getAcademicYear(), targetDate);
        if (hasText(attendance.getAcademicYear())
                && !Objects.equals(attendance.getAcademicYear(), current.record().getAcademicYear())) {
            throw new IllegalArgumentException("The student's current academic year does not match this attendance record.");
        }

        if (attendanceDetails.getStatus() == null && attendance.getStatus() == null) {
            throw new IllegalArgumentException("Attendance status is required.");
        }
        String targetPresenterId = attendanceDetails.getPresentBy() != null
                ? attendanceDetails.getPresentBy() : attendance.getPresentBy();
        validatePresenter(targetPresenterId, targetSchoolId);
        rejectDuplicate(targetStudentId, targetDate, attendance.getId());

        boolean changed = !Objects.equals(attendance.getSchoolId(), targetSchoolId)
                || !Objects.equals(attendance.getStudentId(), targetStudentId)
                || !Objects.equals(attendance.getDate(), targetDate)
                || (attendanceDetails.getStatus() != null
                        && !Objects.equals(attendance.getStatus(), attendanceDetails.getStatus()))
                || (attendanceDetails.getPresentBy() != null
                        && !Objects.equals(attendance.getPresentBy(), attendanceDetails.getPresentBy()));

        attendance.setSchoolId(targetSchoolId);
        attendance.setStudentId(targetStudentId);
        attendance.setDate(targetDate);
        attendance.setAcademicYear(current.record().getAcademicYear());
        attendance.setCurrentAcademicRecordDocsId(current.student().getCurrentAcademicRecordDocsId());
        if (attendanceDetails.getStatus() != null) {
            attendance.setStatus(attendanceDetails.getStatus());
        }
        attendance.setPresentBy(targetPresenterId);
        if (changed) {
            attendance.setPresentTime(LocalDateTime.now());
        }
        return attendanceRepository.save(attendance);
    }

    public void deleteAttendance(String id) {
        Attendance attendance = getAttendanceById(id);
        attendanceRepository.delete(attendance);
    }
}
