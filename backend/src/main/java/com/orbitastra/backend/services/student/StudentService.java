package com.orbitastra.backend.services.student;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;

import com.orbitastra.backend.exceptions.ResourceNotFoundException;
import com.orbitastra.backend.models.core.School;
import com.orbitastra.backend.models.student.Parent;
import com.orbitastra.backend.models.student.Student;
import com.orbitastra.backend.models.student.StudentAcademicRecord;
import com.orbitastra.backend.repositories.student.StudentRepository;
import com.orbitastra.backend.services.student.utils.AcademicYearUtils;
import com.orbitastra.backend.repositories.student.ParentRepository;
import com.orbitastra.backend.repositories.core.SchoolRepository;
import com.orbitastra.backend.repositories.student.StudentAcademicRecordRepository;
import com.orbitastra.backend.models.academics.SchoolClass;
import com.orbitastra.backend.repositories.academics.SchoolClassRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class StudentService {

    private final StudentRepository studentRepository;
    private final ParentRepository parentRepository;
    private final SchoolRepository schoolRepository;
    private final StudentAcademicRecordRepository studentAcademicRecordRepository;
    private final SchoolClassRepository schoolClassRepository;

    private void populateAcademicFields(Student student) {
        if (student == null) return;
        List<StudentAcademicRecord> records = studentAcademicRecordRepository.findByStudentDocId(student.getId());
        if (!records.isEmpty()) {
            records.stream()
                .max(java.util.Comparator.comparing(StudentAcademicRecord::getAcademicYear, 
                    java.util.Comparator.nullsFirst(java.util.Comparator.naturalOrder())))
                .ifPresent(record -> {
                    student.setCurrentAcademicRecord(record);
                });
        }
    }

    private void populateAcademicFields(List<Student> students) {
        if (students == null) return;
        students.forEach(this::populateAcademicFields);
    }

    public Student createStudent(Student student) {
        if (student.getSchoolId() == null) {
            throw new IllegalArgumentException("School ID cannot be null.");
        }
        School school = schoolRepository.findById(student.getSchoolId())
                .orElseThrow(() -> new ResourceNotFoundException("School not found with id: " + student.getSchoolId()));

        // Check if student count exceeds package limit
        if (school.getMaxStudents() != null) {
            long currentStudentCount = studentRepository.countBySchoolId(student.getSchoolId());
            if (currentStudentCount >= school.getMaxStudents()) {
                throw new IllegalArgumentException("You have exceeded the package limit for maximum students.");
            }
        }

        if (student.getParentId() != null && !student.getParentId().isEmpty()) {
            Parent parent = parentRepository.findById(student.getParentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Parent not found with id: " + student.getParentId()));
            if (!student.getSchoolId().equals(parent.getSchoolId())) {
                throw new IllegalArgumentException("Parent does not belong to the same school as the student.");
            }
        }

        if (student.getAdmissionNo() != null && studentRepository.findByAdmissionNo(student.getAdmissionNo()).isPresent()) {
            throw new IllegalArgumentException("Admission number '" + student.getAdmissionNo() + "' is already taken.");
        }

        student.setCreatedAt(LocalDateTime.now());
        student.setUpdatedAt(LocalDateTime.now());
        Student saved = studentRepository.save(student);

        // Save academic record
        StudentAcademicRecord reqRecord = student.getCurrentAcademicRecord();
        StudentAcademicRecord savedRecord = null;
        if (reqRecord != null) {
            String acadYear = reqRecord.getAcademicYear() != null 
                    ? reqRecord.getAcademicYear() 
                    : AcademicYearUtils.getCurrentAcademicYear();

            if (reqRecord.getClassDocId() != null) {
                SchoolClass schoolClass = schoolClassRepository.findById(reqRecord.getClassDocId())
                        .orElseThrow(() -> new ResourceNotFoundException("Class not found with id: " + reqRecord.getClassDocId()));
                if (!schoolClass.getSchoolId().equals(saved.getSchoolId())) {
                    throw new IllegalArgumentException("Class does not belong to the same school as the student.");
                }
            }

            StudentAcademicRecord record = StudentAcademicRecord.builder()
                    .schoolId(saved.getSchoolId())
                    .studentDocId(saved.getId())
                    .academicYear(acadYear)
                    .studentId(reqRecord.getStudentId())
                    .rollNo(reqRecord.getRollNo())
                    .classDocId(reqRecord.getClassDocId())
                    .sectionId(reqRecord.getSectionId())
                    .hostelRoomId(reqRecord.getHostelRoomId())
                    .status(saved.getStatus())
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();
            savedRecord = studentAcademicRecordRepository.save(record);
        }

        // Update Parent's studentIds list
        if (saved.getParentId() != null && !saved.getParentId().isEmpty()) {
            parentRepository.findById(saved.getParentId()).ifPresent(parent -> {
                if (parent.getStudentIds() == null) {
                    parent.setStudentIds(new java.util.ArrayList<>());
                }
                if (!parent.getStudentIds().contains(saved.getId())) {
                    parent.getStudentIds().add(saved.getId());
                    parentRepository.save(parent);
                }
            });
        }

        // Set transient academic field on saved student object
        saved.setCurrentAcademicRecord(savedRecord);

        return saved;
    }

    public List<Student> getAllStudents() {
        List<Student> students = studentRepository.findAll();
        populateAcademicFields(students);
        return students;
    }

    public Student getStudentById(String id) {
        Student student = studentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found with id: " + id));
        populateAcademicFields(student);
        return student;
    }

    public Student getStudentByAdmissionNo(String admissionNo) {
        Student student = studentRepository.findByAdmissionNo(admissionNo)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found with admission number: " + admissionNo));
        populateAcademicFields(student);
        return student;
    }

    public List<Student> getStudentsBySchool(String schoolId) {
        List<Student> students = studentRepository.findBySchoolId(schoolId);
        populateAcademicFields(students);
        return students;
    }

    public List<Student> getStudentsBySchoolAndAcademicYear(String schoolId, String academicYear) {
        List<StudentAcademicRecord> records =
                studentAcademicRecordRepository.findBySchoolIdAndAcademicYear(schoolId, academicYear);
        List<String> studentIds = records.stream()
                .map(StudentAcademicRecord::getStudentDocId)
                .distinct()
                .toList();
        List<Student> students = studentRepository.findAllById(studentIds);

        // Attach the record for THIS academic year (not the latest, as populateAcademicFields would)
        java.util.Map<String, StudentAcademicRecord> recordByStudent = records.stream()
                .collect(java.util.stream.Collectors.toMap(
                        StudentAcademicRecord::getStudentDocId, r -> r, (a, b) -> a));
        students.forEach(s -> s.setCurrentAcademicRecord(recordByStudent.get(s.getId())));
        return students;
    }

    public List<Student> getStudentsByClass(String classId) {
        List<StudentAcademicRecord> records = studentAcademicRecordRepository.findByClassDocId(classId);
        List<String> studentIds = records.stream()
                .map(StudentAcademicRecord::getStudentDocId)
                .distinct()
                .toList();
        List<Student> students = studentRepository.findAllById(studentIds);
        populateAcademicFields(students);
        return students;
    }

    public List<Student> getStudentsByParent(String parentId) {
        List<Student> students = studentRepository.findByParentId(parentId);
        populateAcademicFields(students);
        return students;
    }

    public List<Student> getStudentsByHostelRoom(String hostelRoomId) {
        List<StudentAcademicRecord> records = studentAcademicRecordRepository.findByHostelRoomId(hostelRoomId);
        List<String> studentIds = records.stream()
                .map(StudentAcademicRecord::getStudentDocId)
                .distinct()
                .toList();
        List<Student> students = studentRepository.findAllById(studentIds);
        populateAcademicFields(students);
        return students;
    }

    public Student updateStudent(String id, Student studentDetails) {
        Student student = getStudentById(id);

        if (studentDetails.getSchoolId() != null && !studentDetails.getSchoolId().equals(student.getSchoolId())) {
            if (!schoolRepository.existsById(studentDetails.getSchoolId())) {
                throw new ResourceNotFoundException("School not found with id: " + studentDetails.getSchoolId());
            }
            student.setSchoolId(studentDetails.getSchoolId());
        }

        String oldParentId = student.getParentId();
        String newParentId = studentDetails.getParentId();

        if (newParentId != null && !newParentId.equals(oldParentId)) {
            if (!newParentId.isEmpty()) {
                Parent parent = parentRepository.findById(newParentId)
                        .orElseThrow(() -> new ResourceNotFoundException("Parent not found with id: " + newParentId));
                if (!student.getSchoolId().equals(parent.getSchoolId())) {
                    throw new IllegalArgumentException("Parent does not belong to the same school as the student.");
                }
            }
            student.setParentId(newParentId);
        }

        if (studentDetails.getAdmissionNo() != null && !studentDetails.getAdmissionNo().equals(student.getAdmissionNo())) {
            if (studentRepository.findByAdmissionNo(studentDetails.getAdmissionNo()).isPresent()) {
                throw new IllegalArgumentException("Admission number '" + studentDetails.getAdmissionNo() + "' is already taken.");
            }
            student.setAdmissionNo(studentDetails.getAdmissionNo());
        }

        if (studentDetails.getFirstName() != null) {
            student.setFirstName(studentDetails.getFirstName());
        }
        if (studentDetails.getLastName() != null) {
            student.setLastName(studentDetails.getLastName());
        }
        if (studentDetails.getDob() != null) {
            student.setDob(studentDetails.getDob());
        }
        if (studentDetails.getGender() != null) {
            student.setGender(studentDetails.getGender());
        }
        if (studentDetails.getBloodGroup() != null) {
            student.setBloodGroup(studentDetails.getBloodGroup());
        }
        if (studentDetails.getPhotoUrl() != null) {
            student.setPhotoUrl(studentDetails.getPhotoUrl());
        }
        if (studentDetails.getWalletId() != null) {
            student.setWalletId(studentDetails.getWalletId());
        }
        if (studentDetails.getMedicalRecordId() != null) {
            student.setMedicalRecordId(studentDetails.getMedicalRecordId());
        }
        if (studentDetails.getStatus() != null) {
            student.setStatus(studentDetails.getStatus());
        }
        if (studentDetails.getAdmissionDate() != null) {
            student.setAdmissionDate(studentDetails.getAdmissionDate());
        }

        student.setUpdatedAt(LocalDateTime.now());
        Student saved = studentRepository.save(student);

        // Update academic record
        StudentAcademicRecord detailsRecord = studentDetails.getCurrentAcademicRecord();
        String targetAcademicYear = (detailsRecord != null && detailsRecord.getAcademicYear() != null) 
                ? detailsRecord.getAcademicYear() 
                : (student.getCurrentAcademicRecord() != null ? student.getCurrentAcademicRecord().getAcademicYear() : null);
        if (targetAcademicYear == null) {
            targetAcademicYear = AcademicYearUtils.getCurrentAcademicYear();
        }

        final String finalAcadYear = targetAcademicYear;
        StudentAcademicRecord record = studentAcademicRecordRepository
                .findByStudentDocIdAndAcademicYear(student.getId(), finalAcadYear)
                .orElseGet(() -> StudentAcademicRecord.builder()
                        .studentDocId(student.getId())
                        .academicYear(finalAcadYear)
                        .createdAt(LocalDateTime.now())
                        .build());

        record.setSchoolId(saved.getSchoolId());
        boolean academicChanged = false;

        if (detailsRecord != null) {
            if (detailsRecord.getStudentId() != null) {
                record.setStudentId(detailsRecord.getStudentId());
                academicChanged = true;
            }
            if (detailsRecord.getRollNo() != null) {
                record.setRollNo(detailsRecord.getRollNo());
                academicChanged = true;
            }
            if (detailsRecord.getClassDocId() != null) {
                SchoolClass schoolClass = schoolClassRepository.findById(detailsRecord.getClassDocId())
                        .orElseThrow(() -> new ResourceNotFoundException("Class not found with id: " + detailsRecord.getClassDocId()));
                if (!schoolClass.getSchoolId().equals(saved.getSchoolId())) {
                    throw new IllegalArgumentException("Class does not belong to the same school as the student.");
                }
                record.setClassDocId(detailsRecord.getClassDocId());
                academicChanged = true;
            }
            if (detailsRecord.getSectionId() != null) {
                record.setSectionId(detailsRecord.getSectionId());
                academicChanged = true;
            }
            if (detailsRecord.getHostelRoomId() != null) {
                record.setHostelRoomId(detailsRecord.getHostelRoomId());
                academicChanged = true;
            }
        }
        if (studentDetails.getStatus() != null) {
            record.setStatus(studentDetails.getStatus());
            academicChanged = true;
        }

        if (academicChanged) {
            record.setUpdatedAt(LocalDateTime.now());
            record = studentAcademicRecordRepository.save(record);
        }
        saved.setCurrentAcademicRecord(record);

        // Adjust parent references
        if (newParentId != null && !newParentId.equals(oldParentId)) {
            // Remove from old parent
            if (oldParentId != null && !oldParentId.isEmpty()) {
                parentRepository.findById(oldParentId).ifPresent(oldParent -> {
                    if (oldParent.getStudentIds() != null && oldParent.getStudentIds().remove(saved.getId())) {
                        parentRepository.save(oldParent);
                    }
                });
            }
            // Add to new parent
            if (!newParentId.isEmpty()) {
                parentRepository.findById(newParentId).ifPresent(newParent -> {
                    if (newParent.getStudentIds() == null) {
                        newParent.setStudentIds(new java.util.ArrayList<>());
                    }
                    if (!newParent.getStudentIds().contains(saved.getId())) {
                        newParent.getStudentIds().add(saved.getId());
                        parentRepository.save(newParent);
                    }
                });
            }
        }

        // Repopulate transient fields to reflect latest state
        populateAcademicFields(saved);
        return saved;
    }

    public void deleteStudent(String id) {
        Student student = getStudentById(id);
        String parentId = student.getParentId();
        
        // Delete academic records first
        List<StudentAcademicRecord> records = studentAcademicRecordRepository.findByStudentDocId(id);
        studentAcademicRecordRepository.deleteAll(records);

        studentRepository.delete(student);

        if (parentId != null && !parentId.isEmpty()) {
            parentRepository.findById(parentId).ifPresent(parent -> {
                if (parent.getStudentIds() != null && parent.getStudentIds().remove(id)) {
                    parentRepository.save(parent);
                }
            });
        }
    }

    public StudentAcademicRecord createOrUpdateAcademicRecord(String studentId, StudentAcademicRecord recordDetails) {
        Student student = getStudentById(studentId); // Throws if not found
        
        StudentAcademicRecord record = studentAcademicRecordRepository
                .findByStudentDocIdAndAcademicYear(studentId, recordDetails.getAcademicYear())
                .orElseGet(() -> StudentAcademicRecord.builder()
                        .studentDocId(studentId)
                        .academicYear(recordDetails.getAcademicYear())
                        .createdAt(LocalDateTime.now())
                        .build());
                        
        record.setSchoolId(student.getSchoolId());
        if (recordDetails.getStudentId() != null) record.setStudentId(recordDetails.getStudentId());
        if (recordDetails.getRollNo() != null) record.setRollNo(recordDetails.getRollNo());
        if (recordDetails.getClassDocId() != null) {
            SchoolClass schoolClass = schoolClassRepository.findById(recordDetails.getClassDocId())
                    .orElseThrow(() -> new ResourceNotFoundException("Class not found with id: " + recordDetails.getClassDocId()));
            if (!schoolClass.getSchoolId().equals(student.getSchoolId())) {
                throw new IllegalArgumentException("Class does not belong to the same school as the student.");
            }
            record.setClassDocId(recordDetails.getClassDocId());
        }
        if (recordDetails.getSectionId() != null) record.setSectionId(recordDetails.getSectionId());
        if (recordDetails.getHostelRoomId() != null) record.setHostelRoomId(recordDetails.getHostelRoomId());
        if (recordDetails.getStatus() != null) {
            record.setStatus(recordDetails.getStatus());
        } else {
            record.setStatus(student.getStatus());
        }
        record.setUpdatedAt(LocalDateTime.now());
        
        return studentAcademicRecordRepository.save(record);
    }
    
    public StudentAcademicRecord promoteStudent(String studentId, StudentAcademicRecord promotionDetails) {
        if (promotionDetails.getAcademicYear() == null || promotionDetails.getAcademicYear().isEmpty()) {
            throw new IllegalArgumentException("Academic Year ID is required for promotion.");
        }
        return createOrUpdateAcademicRecord(studentId, promotionDetails);
    }
    
    public List<StudentAcademicRecord> getAcademicHistory(String studentId) {
        if (!studentRepository.existsById(studentId)) {
            throw new ResourceNotFoundException("Student not found with id: " + studentId);
        }
        return studentAcademicRecordRepository.findByStudentDocId(studentId);
    }

    public List<Student> getSiblings(String studentId) {
        Student student = getStudentById(studentId);
        
        if (student.getParentId() == null || student.getParentId().isEmpty()) {
            return new java.util.ArrayList<>();
        }
        
        List<Student> allChildren = studentRepository.findByParentId(student.getParentId());
        
        return allChildren.stream()
                .filter(child -> !child.getId().equals(studentId))
                .toList();
    }
}
