package com.orbitastra.backend.services.academics;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.orbitastra.backend.exceptions.ResourceNotFoundException;
import com.orbitastra.backend.models.academics.Homework;
import com.orbitastra.backend.models.academics.SchoolClass;
import com.orbitastra.backend.models.academics.enums.AssignmentScope;
import com.orbitastra.backend.models.academics.enums.HomeworkStatus;
import com.orbitastra.backend.models.staff.Staff;
import com.orbitastra.backend.models.student.Student;
import com.orbitastra.backend.models.student.StudentAcademicRecord;
import com.orbitastra.backend.repositories.academics.HomeworkRepository;
import com.orbitastra.backend.repositories.academics.SchoolClassRepository;
import com.orbitastra.backend.repositories.core.SchoolRepository;
import com.orbitastra.backend.repositories.staff.StaffRepository;
import com.orbitastra.backend.repositories.student.StudentAcademicRecordRepository;
import com.orbitastra.backend.services.utils.StudentValidator;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class HomeworkService {

    private final HomeworkRepository homeworkRepository;
    private final SchoolRepository schoolRepository;
    private final SchoolClassRepository schoolClassRepository;
    private final StudentValidator studentValidator;
    private final StudentAcademicRecordRepository studentAcademicRecordRepository;
    private final StaffRepository staffRepository;

    private static boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private void validateSchool(String schoolId) {
        if (!hasText(schoolId) || !schoolRepository.existsById(schoolId)) {
            throw new ResourceNotFoundException("School not found with id: " + schoolId);
        }
    }

    private SchoolClass validateClass(String classDocsId, String schoolId) {
        if (!hasText(classDocsId)) {
            throw new IllegalArgumentException("classDocsId cannot be null or empty.");
        }

        SchoolClass schoolClass = schoolClassRepository.findById(classDocsId)
                .orElseThrow(() -> new ResourceNotFoundException("Class not found with id: " + classDocsId));

        if (!Objects.equals(schoolClass.getSchoolId(), schoolId)) {
            throw new IllegalArgumentException("Class does not belong to the same school as the homework.");
        }
        if (!hasText(schoolClass.getAcademicYear())) {
            throw new IllegalArgumentException("Class " + classDocsId + " is not linked to an academic year.");
        }
        return schoolClass;
    }

    private void validateSection(SchoolClass schoolClass, String sectionNo) {
        if (!hasText(sectionNo)) {
            throw new IllegalArgumentException("sectionNo is required for homework.");
        }

        boolean belongsToClass = schoolClass.getSections() != null
                && schoolClass.getSections().stream()
                        .filter(Objects::nonNull)
                        .anyMatch(section -> section.trim().equalsIgnoreCase(sectionNo.trim()));
        if (!belongsToClass) {
            throw new IllegalArgumentException("Section '" + sectionNo
                    + "' does not belong to class " + schoolClass.getId() + ".");
        }
    }

    private Staff validateTeacherInSchool(String teacherDocsId, String schoolId) {
        if (!hasText(teacherDocsId)) {
            throw new IllegalArgumentException("teacherDocsId cannot be null or empty.");
        }

        Staff teacher = staffRepository.findById(teacherDocsId)
                .orElseThrow(() -> new ResourceNotFoundException("Teacher not found with id: " + teacherDocsId));
        if (!Objects.equals(teacher.getSchoolId(), schoolId)) {
            throw new IllegalArgumentException("Teacher does not belong to the same school as the homework.");
        }
        return teacher;
    }

    /**
     * A teacher may teach homework only for a class where they are the class
     * teacher or the teacher of one of the class subjects. Subject names are
     * also checked when the class has a configured subject list.
     */
    private void validateTeacherAndSubject(SchoolClass schoolClass, String teacherDocsId,
            String subject, String schoolId) {
        validateTeacherInSchool(teacherDocsId, schoolId);

        boolean classTeacher = Objects.equals(schoolClass.getClassTeacherDocsId(), teacherDocsId);
        boolean subjectTeacher = schoolClass.getSubjects() != null && schoolClass.getSubjects().stream()
                .filter(Objects::nonNull)
                .anyMatch(classSubject -> Objects.equals(classSubject.getTeacherDocsId(), teacherDocsId));
        if (!classTeacher && !subjectTeacher) {
            throw new IllegalArgumentException("Teacher " + teacherDocsId
                    + " is not assigned to class " + schoolClass.getId() + ".");
        }

        if (hasText(subject) && schoolClass.getSubjects() != null && !schoolClass.getSubjects().isEmpty()) {
            boolean subjectExists = schoolClass.getSubjects().stream()
                    .filter(Objects::nonNull)
                    .filter(classSubject -> hasText(classSubject.getName()))
                    .anyMatch(classSubject -> classSubject.getName().trim().equalsIgnoreCase(subject.trim()));
            if (!subjectExists) {
                throw new IllegalArgumentException("Subject '" + subject
                        + "' does not belong to class " + schoolClass.getId() + ".");
            }
        }
    }

    /**
     * Validates all fields that define the homework target and synchronises the
     * academic year from the class. The class is the source of truth for the
     * year; clients cannot assign homework to a different year by posting a
     * forged academicYear.
     */
    private SchoolClass validateHomeworkAcademicTarget(Homework homework) {
        validateSchool(homework.getSchoolId());
        SchoolClass schoolClass = validateClass(homework.getClassDocsId(), homework.getSchoolId());
        validateSection(schoolClass, homework.getSectionNo());
        homework.setSectionNo(homework.getSectionNo().trim());

        if (hasText(homework.getAcademicYear())
                && !Objects.equals(homework.getAcademicYear(), schoolClass.getAcademicYear())) {
            throw new IllegalArgumentException("Homework academicYear must match the class academic year.");
        }
        homework.setAcademicYear(schoolClass.getAcademicYear());
        return schoolClass;
    }

    private SchoolClass validateHomeworkTarget(Homework homework) {
        SchoolClass schoolClass = validateHomeworkAcademicTarget(homework);
        validateTeacherAndSubject(schoolClass, homework.getTeacherDocsId(), homework.getSubject(), homework.getSchoolId());
        return schoolClass;
    }

    private boolean sectionMatches(String expectedSection, String actualSection) {
        return hasText(expectedSection) && hasText(actualSection)
                && expectedSection.trim().equalsIgnoreCase(actualSection.trim());
    }

    private StudentAcademicRecord validateStudentAssignment(String schoolId, SchoolClass schoolClass,
            String sectionNo, String studentDocsId) {
        if (!hasText(studentDocsId)) {
            throw new IllegalArgumentException("studentDocsId is required for every homework assignment.");
        }

        Student student = studentValidator.validateStudent(studentDocsId, schoolId);
        StudentAcademicRecord academicRecord = studentAcademicRecordRepository
                .findByStudentDocsIdAndAcademicYear(studentDocsId, schoolClass.getAcademicYear())
                .orElseThrow(() -> new IllegalArgumentException("Student with ID " + studentDocsId
                        + " does not have an academic record for academic year " + schoolClass.getAcademicYear()));

        // When the denormalised pointer is present, it must point at the same
        // year-specific record. This prevents assigning work to a stale
        // enrolment after a student has moved to another class or year. Older
        // records without the pointer remain compatible and are still checked
        // against the resolved academic record below.
        if (student != null && hasText(student.getCurrentAcademicRecordDocsId())
                && !Objects.equals(student.getCurrentAcademicRecordDocsId(), academicRecord.getId())) {
            throw new IllegalArgumentException("Student with ID " + studentDocsId
                    + " does not have this class as the current academic record.");
        }

        if (!Objects.equals(academicRecord.getSchoolId(), schoolId)
                || !Objects.equals(schoolClass.getId(), academicRecord.getClassDocsId())
                || !sectionMatches(sectionNo, academicRecord.getSectionNo())) {
            throw new IllegalArgumentException("Student with ID " + studentDocsId
                    + " is not currently enrolled in class " + schoolClass.getId()
                    + ", section " + sectionNo + ", for academic year " + schoolClass.getAcademicYear());
        }
        return academicRecord;
    }

    private void validateStudentAssignments(String schoolId, SchoolClass schoolClass, String sectionNo,
            List<Homework.StudentAssignment> assignments) {
        if (assignments == null || assignments.isEmpty()) {
            throw new IllegalArgumentException("Student assignments list must not be empty for GROUP or INDIVIDUAL scope.");
        }

        Set<String> studentDocsIds = new HashSet<>();
        for (Homework.StudentAssignment assignment : assignments) {
            if (assignment == null) {
                throw new IllegalArgumentException("Student assignments cannot contain null entries.");
            }
            String studentDocsId = assignment.getStudentDocsId();
            if (!studentDocsIds.add(studentDocsId == null ? "" : studentDocsId.trim())) {
                throw new IllegalArgumentException("A student cannot be assigned to the same homework more than once: "
                        + studentDocsId);
            }
            validateStudentAssignment(schoolId, schoolClass, sectionNo, studentDocsId);
            if (assignment.getStatus() == null) {
                assignment.setStatus(HomeworkStatus.ASSIGNED);
            }
        }
    }

    private List<Homework.StudentAssignment> assignmentsForSection(String schoolId, SchoolClass schoolClass,
            String sectionNo) {
        List<StudentAcademicRecord> records = studentAcademicRecordRepository
                .findByClassDocsIdAndAcademicYear(schoolClass.getId(), schoolClass.getAcademicYear());
        List<Homework.StudentAssignment> assignments = new ArrayList<>();
        Set<String> assignedStudentDocsIds = new HashSet<>();

        for (StudentAcademicRecord record : records) {
            if (!Objects.equals(record.getSchoolId(), schoolId)
                    || !sectionMatches(sectionNo, record.getSectionNo())
                    || !hasText(record.getStudentDocsId())) {
                continue;
            }
            validateStudentAssignment(schoolId, schoolClass, sectionNo, record.getStudentDocsId());
            if (assignedStudentDocsIds.add(record.getStudentDocsId())) {
                assignments.add(Homework.StudentAssignment.builder()
                        .studentDocsId(record.getStudentDocsId())
                        .status(HomeworkStatus.ASSIGNED)
                        .build());
            }
        }
        return assignments;
    }

    public Homework createHomework(Homework homework) {
        SchoolClass schoolClass = validateHomeworkTarget(homework);
        if (homework.getAssignmentScope() == null) {
            homework.setAssignmentScope(AssignmentScope.CLASS);
        }

        if (homework.getAssignmentScope() == AssignmentScope.CLASS) {
            homework.setStudentAssignments(assignmentsForSection(homework.getSchoolId(), schoolClass,
                    homework.getSectionNo()));
        } else {
            validateStudentAssignments(homework.getSchoolId(), schoolClass, homework.getSectionNo(),
                    homework.getStudentAssignments());
        }

        homework.setSubmittedCount(0);
        return homeworkRepository.save(homework);
    }

    public Homework createHomeworkDefinition(Homework homework) {
        validateHomeworkTarget(homework);
        homework.setStudentAssignments(new ArrayList<>());
        homework.setSubmittedCount(0);
        return homeworkRepository.save(homework);
    }

    public Homework assignHomework(String id, AssignmentScope scope, List<Homework.StudentAssignment> providedAssignments) {
        if (scope == null) {
            throw new IllegalArgumentException("assignmentScope is required.");
        }

        Homework homework = getHomeworkById(id);
        SchoolClass schoolClass = validateHomeworkTarget(homework);
        homework.setAssignmentScope(scope);

        if (scope == AssignmentScope.CLASS) {
            homework.setStudentAssignments(assignmentsForSection(homework.getSchoolId(), schoolClass,
                    homework.getSectionNo()));
        } else {
            validateStudentAssignments(homework.getSchoolId(), schoolClass, homework.getSectionNo(), providedAssignments);
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

    public List<Homework> getHomeworkBySchoolAndAcademicYear(String schoolId, String academicYear) {
        return homeworkRepository.findBySchoolIdAndAcademicYear(schoolId, academicYear);
    }

    public List<Homework> getHomeworkByClass(String classDocsId) {
        return homeworkRepository.findByClassDocsId(classDocsId);
    }

    public List<Homework> getHomeworkByStudent(String studentDocsId) {
        return homeworkRepository.findByStudentAssignmentsStudentDocsId(studentDocsId);
    }

    public List<Homework> getHomeworkBySchoolAndStudent(String schoolId, String studentDocsId) {
        return homeworkRepository.findBySchoolIdAndStudentAssignmentsStudentDocsId(schoolId, studentDocsId);
    }

    public Homework submitHomework(String homeworkId, String studentDocsId, String text, String fileUrl) {
        Homework homework = getHomeworkById(homeworkId);
        SchoolClass schoolClass = validateHomeworkAcademicTarget(homework);
        validateStudentAssignment(homework.getSchoolId(), schoolClass, homework.getSectionNo(), studentDocsId);

        Homework.StudentAssignment targetAssignment = findStudentAssignment(homework, studentDocsId);
        targetAssignment.setSubmissionText(text);
        targetAssignment.setSubmissionFileUrl(fileUrl);
        targetAssignment.setSubmittedAt(LocalDateTime.now());
        targetAssignment.setStatus(HomeworkStatus.SUBMITTED);
        updateSubmittedCount(homework);
        return homeworkRepository.save(homework);
    }

    public Homework gradeHomework(String homeworkId, String studentDocsId, Integer obtainedMarks, String feedback) {
        Homework homework = getHomeworkById(homeworkId);
        SchoolClass schoolClass = validateHomeworkAcademicTarget(homework);
        validateStudentAssignment(homework.getSchoolId(), schoolClass, homework.getSectionNo(), studentDocsId);

        if (obtainedMarks != null && homework.getMaxMarks() != null && obtainedMarks > homework.getMaxMarks()) {
            throw new IllegalArgumentException("obtainedMarks cannot be greater than maxMarks.");
        }
        Homework.StudentAssignment targetAssignment = findStudentAssignment(homework, studentDocsId);
        targetAssignment.setObtainedMarks(obtainedMarks);
        targetAssignment.setFeedback(feedback);
        targetAssignment.setStatus(HomeworkStatus.EVALUATED);
        updateSubmittedCount(homework);
        return homeworkRepository.save(homework);
    }

    private Homework.StudentAssignment findStudentAssignment(Homework homework, String studentDocsId) {
        if (homework.getStudentAssignments() != null) {
            for (Homework.StudentAssignment assignment : homework.getStudentAssignments()) {
                if (assignment != null && Objects.equals(assignment.getStudentDocsId(), studentDocsId)) {
                    return assignment;
                }
            }
        }
        throw new ResourceNotFoundException("Student assignment not found for student: " + studentDocsId);
    }

    private void updateSubmittedCount(Homework homework) {
        long count = homework.getStudentAssignments() == null ? 0
                : homework.getStudentAssignments().stream()
                        .filter(Objects::nonNull)
                        .filter(a -> a.getStatus() == HomeworkStatus.SUBMITTED
                                || a.getStatus() == HomeworkStatus.EVALUATED)
                        .count();
        homework.setSubmittedCount((int) count);
    }

    public Homework updateHomework(String id, Homework homeworkDetails) {
        Homework homework = getHomeworkById(id);

        String targetSchoolId = hasText(homeworkDetails.getSchoolId())
                ? homeworkDetails.getSchoolId() : homework.getSchoolId();
        String targetClassDocsId = hasText(homeworkDetails.getClassDocsId())
                ? homeworkDetails.getClassDocsId() : homework.getClassDocsId();
        String targetSectionNo = hasText(homeworkDetails.getSectionNo())
                ? homeworkDetails.getSectionNo() : homework.getSectionNo();
        String targetTeacherDocsId = hasText(homeworkDetails.getTeacherDocsId())
                ? homeworkDetails.getTeacherDocsId() : homework.getTeacherDocsId();
        String targetSubject = homeworkDetails.getSubject() != null
                ? homeworkDetails.getSubject() : homework.getSubject();

        boolean targetChanged = !Objects.equals(targetClassDocsId, homework.getClassDocsId())
                || !sectionMatches(targetSectionNo, homework.getSectionNo());
        if (targetChanged && homework.getStudentAssignments() != null && !homework.getStudentAssignments().isEmpty()) {
            throw new IllegalArgumentException("Cannot change class or section after students have been assigned; create a new homework.");
        }

        Homework target = Homework.builder()
                .schoolId(targetSchoolId)
                .classDocsId(targetClassDocsId)
                .sectionNo(targetSectionNo)
                .teacherDocsId(targetTeacherDocsId)
                .subject(targetSubject)
                .academicYear(homework.getAcademicYear())
                .build();
        SchoolClass targetClass = validateHomeworkTarget(target);

        homework.setSchoolId(targetSchoolId);
        homework.setClassDocsId(targetClass.getId());
        homework.setSectionNo(targetSectionNo.trim());
        homework.setAcademicYear(targetClass.getAcademicYear());
        homework.setTeacherDocsId(targetTeacherDocsId);

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
            if (homeworkDetails.getMaxMarks() < 0) {
                throw new IllegalArgumentException("maxMarks cannot be negative.");
            }
            homework.setMaxMarks(homeworkDetails.getMaxMarks());
        }

        return homeworkRepository.save(homework);
    }

    public void deleteHomework(String id) {
        Homework homework = getHomeworkById(id);
        homeworkRepository.delete(homework);
    }
}
