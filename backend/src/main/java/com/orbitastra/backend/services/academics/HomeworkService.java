package com.orbitastra.backend.services.academics;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.orbitastra.backend.exceptions.ResourceNotFoundException;
import com.orbitastra.backend.models.academics.Homework;
import com.orbitastra.backend.models.academics.SchoolClass;
import com.orbitastra.backend.models.academics.enums.AssignmentScope;
import com.orbitastra.backend.models.academics.enums.HomeworkStatus;
import com.orbitastra.backend.models.student.Student;
import com.orbitastra.backend.models.student.StudentAcademicRecord;
import com.orbitastra.backend.repositories.academics.HomeworkRepository;
import com.orbitastra.backend.repositories.academics.SchoolClassRepository;
import com.orbitastra.backend.repositories.core.SchoolRepository;
import com.orbitastra.backend.repositories.student.StudentRepository;
import com.orbitastra.backend.repositories.student.StudentAcademicRecordRepository;
import com.orbitastra.backend.repositories.staff.StaffRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class HomeworkService {

    private final HomeworkRepository homeworkRepository;
    private final SchoolRepository schoolRepository;
    private final SchoolClassRepository schoolClassRepository;
    private final StudentRepository studentRepository;
    private final StudentAcademicRecordRepository studentAcademicRecordRepository;
    private final StaffRepository staffRepository;

    private void validateTeacher(String teacherId, String schoolId) {
        if (teacherId == null || teacherId.isEmpty()) {
            throw new IllegalArgumentException("Teacher ID cannot be null or empty.");
        }
        com.orbitastra.backend.models.staff.Staff teacher = staffRepository.findById(teacherId)
                .orElseThrow(() -> new ResourceNotFoundException("Teacher not found with id: " + teacherId));
        if (!teacher.getSchoolId().equals(schoolId)) {
            throw new IllegalArgumentException("Teacher does not belong to the same school as the homework.");
        }
    }

    private SchoolClass validateClass(String classId, String schoolId) {
        if (classId == null || classId.isEmpty()) {
            throw new IllegalArgumentException("Class ID cannot be null or empty.");
        }
        SchoolClass schoolClass = schoolClassRepository.findById(classId)
                .orElseThrow(() -> new ResourceNotFoundException("Class not found with id: " + classId));
        if (!schoolClass.getSchoolId().equals(schoolId)) {
            throw new IllegalArgumentException("Class does not belong to the same school as the homework.");
        }
        return schoolClass;
    }

    private void validateStudentAssignments(String schoolId, String classId, List<Homework.StudentAssignment> assignments) {
        if (assignments == null || assignments.isEmpty()) {
            return;
        }

        SchoolClass schoolClass = validateClass(classId, schoolId);
        String targetAcademicYear = schoolClass.getAcademicYear();

        for (Homework.StudentAssignment assignment : assignments) {
            String studentId = assignment.getStudentId();
            Student student = studentRepository.findById(studentId)
                    .orElseThrow(() -> new ResourceNotFoundException("Student not found with id: " + studentId));
            
            if (!student.getSchoolId().equals(schoolId)) {
                throw new IllegalArgumentException("Student with ID " + studentId + " does not belong to the same school as the homework.");
            }

            StudentAcademicRecord academicRecord = studentAcademicRecordRepository
                    .findByStudentDocIdAndAcademicYear(studentId, targetAcademicYear)
                    .orElseThrow(() -> new IllegalArgumentException("Student with ID " + studentId + " does not have an academic record for academic year " + targetAcademicYear));

            if (!classId.equals(academicRecord.getClassDocId())) {
                throw new IllegalArgumentException("Student with ID " + studentId + " is not currently enrolled in class " + classId + " for the academic year " + targetAcademicYear);
            }
        }
    }

    public Homework createHomework(Homework homework) {
        if (homework.getSchoolId() == null || !schoolRepository.existsById(homework.getSchoolId())) {
            throw new ResourceNotFoundException("School not found with id: " + homework.getSchoolId());
        }

        validateTeacher(homework.getTeacherId(), homework.getSchoolId());

        validateClass(homework.getClassId(), homework.getSchoolId());

        if (homework.getAssignmentScope() == null) {
            homework.setAssignmentScope(AssignmentScope.CLASS);
        }

        if (homework.getAssignmentScope() == AssignmentScope.CLASS) {
            List<StudentAcademicRecord> records = studentAcademicRecordRepository.findByClassDocId(homework.getClassId());
            List<Homework.StudentAssignment> assignments = new ArrayList<>();
            for (StudentAcademicRecord record : records) {
                Homework.StudentAssignment assignment = Homework.StudentAssignment.builder()
                        .studentId(record.getStudentDocId())
                        .status(HomeworkStatus.ASSIGNED)
                        .build();
                assignments.add(assignment);
            }
            homework.setStudentAssignments(assignments);
        } else {
            if (homework.getStudentAssignments() != null) {
                validateStudentAssignments(homework.getSchoolId(), homework.getClassId(), homework.getStudentAssignments());
                for (Homework.StudentAssignment assignment : homework.getStudentAssignments()) {
                    if (assignment.getStatus() == null) {
                        assignment.setStatus(HomeworkStatus.ASSIGNED);
                    }
                }
            }
        }

        homework.setSubmittedCount(0);
        return homeworkRepository.save(homework);
    }

    public Homework createHomeworkDefinition(Homework homework) {
        if (homework.getSchoolId() == null || !schoolRepository.existsById(homework.getSchoolId())) {
            throw new ResourceNotFoundException("School not found with id: " + homework.getSchoolId());
        }

        validateTeacher(homework.getTeacherId(), homework.getSchoolId());

        validateClass(homework.getClassId(), homework.getSchoolId());

        homework.setStudentAssignments(new ArrayList<>());
        homework.setSubmittedCount(0);
        return homeworkRepository.save(homework);
    }

    public Homework assignHomework(String id, AssignmentScope scope, List<Homework.StudentAssignment> providedAssignments) {
        Homework homework = getHomeworkById(id);
        homework.setAssignmentScope(scope);

        if (scope == AssignmentScope.CLASS) {
            List<StudentAcademicRecord> records = studentAcademicRecordRepository.findByClassDocId(homework.getClassId());
            List<Homework.StudentAssignment> assignments = new ArrayList<>();
            for (StudentAcademicRecord record : records) {
                Homework.StudentAssignment assignment = Homework.StudentAssignment.builder()
                        .studentId(record.getStudentDocId())
                        .status(HomeworkStatus.ASSIGNED)
                        .build();
                assignments.add(assignment);
            }
            homework.setStudentAssignments(assignments);
        } else {
            if (providedAssignments == null || providedAssignments.isEmpty()) {
                throw new IllegalArgumentException("Student assignments list must not be empty for GROUP or INDIVIDUAL scope.");
            }
            validateStudentAssignments(homework.getSchoolId(), homework.getClassId(), providedAssignments);
            for (Homework.StudentAssignment assignment : providedAssignments) {
                if (assignment.getStatus() == null) {
                    assignment.setStatus(HomeworkStatus.ASSIGNED);
                }
            }
            homework.setStudentAssignments(providedAssignments);
        }

        homework.setSubmittedCount(0);
        return homeworkRepository.save(homework);
    }

    public List<Homework> getAllHomework() {
        return homeworkRepository.findAll();
    }

    public Homework getHomeworkById(String id) {
        return homeworkRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Homework not found with id: " + id));
    }

    public List<Homework> getHomeworkBySchool(String schoolId) {
        return homeworkRepository.findBySchoolId(schoolId);
    }

    public List<Homework> getHomeworkByClass(String classId) {
        return homeworkRepository.findByClassId(classId);
    }

    public List<Homework> getHomeworkByStudent(String studentId) {
        return homeworkRepository.findByStudentAssignmentsStudentId(studentId);
    }

    public List<Homework> getHomeworkBySchoolAndStudent(String schoolId, String studentId) {
        return homeworkRepository.findBySchoolIdAndStudentAssignmentsStudentId(schoolId, studentId);
    }

    public Homework submitHomework(String homeworkId, String studentId, String text, String fileUrl) {
        Homework homework = getHomeworkById(homeworkId);

        Homework.StudentAssignment targetAssignment = null;
        for (Homework.StudentAssignment assignment : homework.getStudentAssignments()) {
            if (assignment.getStudentId().equals(studentId)) {
                targetAssignment = assignment;
                break;
            }
        }

        if (targetAssignment == null) {
            throw new ResourceNotFoundException("Student assignment not found for student: " + studentId);
        }

        targetAssignment.setSubmissionText(text);
        targetAssignment.setSubmissionFileUrl(fileUrl);
        targetAssignment.setSubmittedAt(LocalDateTime.now());
        targetAssignment.setStatus(HomeworkStatus.SUBMITTED);

        long count = homework.getStudentAssignments().stream()
                .filter(a -> a.getStatus() == HomeworkStatus.SUBMITTED || a.getStatus() == HomeworkStatus.EVALUATED)
                .count();
        homework.setSubmittedCount((int) count);

        return homeworkRepository.save(homework);
    }

    public Homework gradeHomework(String homeworkId, String studentId, Integer obtainedMarks, String feedback) {
        Homework homework = getHomeworkById(homeworkId);

        Homework.StudentAssignment targetAssignment = null;
        for (Homework.StudentAssignment assignment : homework.getStudentAssignments()) {
            if (assignment.getStudentId().equals(studentId)) {
                targetAssignment = assignment;
                break;
            }
        }

        if (targetAssignment == null) {
            throw new ResourceNotFoundException("Student assignment not found for student: " + studentId);
        }

        targetAssignment.setObtainedMarks(obtainedMarks);
        targetAssignment.setFeedback(feedback);
        targetAssignment.setStatus(HomeworkStatus.EVALUATED);

        return homeworkRepository.save(homework);
    }

    public Homework updateHomework(String id, Homework homeworkDetails) {
        Homework homework = getHomeworkById(id);

        if (homeworkDetails.getSchoolId() != null && !homeworkDetails.getSchoolId().equals(homework.getSchoolId())) {
            if (!schoolRepository.existsById(homeworkDetails.getSchoolId())) {
                throw new ResourceNotFoundException("School not found with id: " + homeworkDetails.getSchoolId());
            }
            homework.setSchoolId(homeworkDetails.getSchoolId());
        }

        if (homeworkDetails.getClassId() != null && !homeworkDetails.getClassId().equals(homework.getClassId())) {
            validateClass(homeworkDetails.getClassId(), homework.getSchoolId());
            homework.setClassId(homeworkDetails.getClassId());
        }

        if (homeworkDetails.getSubject() != null) {
            homework.setSubject(homeworkDetails.getSubject());
        }
        if (homeworkDetails.getTitle() != null) {
            homework.setTitle(homeworkDetails.getTitle());
        }
        if (homeworkDetails.getInstructions() != null) {
            homework.setInstructions(homeworkDetails.getInstructions());
        }
        if (homeworkDetails.getDueDate() != null) {
            homework.setDueDate(homeworkDetails.getDueDate());
        }
        if (homeworkDetails.getAssignmentScope() != null) {
            homework.setAssignmentScope(homeworkDetails.getAssignmentScope());
        }
        if (homeworkDetails.getMaxMarks() != null) {
            homework.setMaxMarks(homeworkDetails.getMaxMarks());
        }
        if (homeworkDetails.getTeacherId() != null && !homeworkDetails.getTeacherId().equals(homework.getTeacherId())) {
            validateTeacher(homeworkDetails.getTeacherId(), homework.getSchoolId());
            homework.setTeacherId(homeworkDetails.getTeacherId());
        }

        return homeworkRepository.save(homework);
    }

    public void deleteHomework(String id) {
        Homework homework = getHomeworkById(id);
        homeworkRepository.delete(homework);
    }
}
