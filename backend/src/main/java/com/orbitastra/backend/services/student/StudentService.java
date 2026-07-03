package com.orbitastra.backend.services.student;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;

import com.orbitastra.backend.exceptions.ResourceNotFoundException;
import com.orbitastra.backend.models.core.School;
import com.orbitastra.backend.models.student.Student;
import com.orbitastra.backend.models.student.StudentAcademicRecord;
import com.orbitastra.backend.repositories.student.StudentRepository;
import com.orbitastra.backend.services.student.utils.AcademicYearUtils;
import com.orbitastra.backend.repositories.student.ParentRepository;
import com.orbitastra.backend.repositories.core.SchoolRepository;
import com.orbitastra.backend.repositories.student.StudentAcademicRecordRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class StudentService {

    private final StudentRepository studentRepository;
    private final ParentRepository parentRepository;
    private final SchoolRepository schoolRepository;
    private final StudentAcademicRecordRepository studentAcademicRecordRepository;

    private void populateAcademicFields(Student student) {
        if (student == null) return;
        List<StudentAcademicRecord> records = studentAcademicRecordRepository.findByStudentDocId(student.getId());
        if (!records.isEmpty()) {
            records.stream()
                .max(java.util.Comparator.comparing(StudentAcademicRecord::getAcademicYearId, 
                    java.util.Comparator.nullsFirst(java.util.Comparator.naturalOrder())))
                .ifPresent(record -> {
                    student.setAcademicYearId(record.getAcademicYearId());
                    student.setStudentId(record.getStudentId());
                    student.setRollNo(record.getRollNo());
                    student.setClassId(record.getClassId());
                    student.setSectionId(record.getSectionId());
                    student.setHostelRoomId(record.getHostelRoomId());
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
            if (!parentRepository.existsById(student.getParentId())) {
                throw new ResourceNotFoundException("Parent not found with id: " + student.getParentId());
            }
        }

        if (student.getAdmissionNo() != null && studentRepository.findByAdmissionNo(student.getAdmissionNo()).isPresent()) {
            throw new IllegalArgumentException("Admission number '" + student.getAdmissionNo() + "' is already taken.");
        }

        student.setCreatedAt(LocalDateTime.now());
        student.setUpdatedAt(LocalDateTime.now());
        Student saved = studentRepository.save(student);

        // Save academic record
        String acadYear = student.getAcademicYearId() != null ? student.getAcademicYearId() : AcademicYearUtils.getCurrentAcademicYear();
        StudentAcademicRecord record = StudentAcademicRecord.builder()
                .schoolId(saved.getSchoolId())
                .studentDocId(saved.getId())
                .academicYearId(acadYear)
                .studentId(student.getStudentId())
                .rollNo(student.getRollNo())
                .classId(student.getClassId())
                .sectionId(student.getSectionId())
                .hostelRoomId(student.getHostelRoomId())
                .status(saved.getStatus())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        studentAcademicRecordRepository.save(record);

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

        // Set transient academic fields on saved student object
        saved.setAcademicYearId(acadYear);
        saved.setStudentId(student.getStudentId());
        saved.setRollNo(student.getRollNo());
        saved.setClassId(student.getClassId());
        saved.setSectionId(student.getSectionId());
        saved.setHostelRoomId(student.getHostelRoomId());

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

    public List<Student> getStudentsByClass(String classId) {
        List<StudentAcademicRecord> records = studentAcademicRecordRepository.findByClassId(classId);
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
            if (!newParentId.isEmpty() && !parentRepository.existsById(newParentId)) {
                throw new ResourceNotFoundException("Parent not found with id: " + newParentId);
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
        String targetAcademicYear = studentDetails.getAcademicYearId() != null ? studentDetails.getAcademicYearId() : student.getAcademicYearId();
        if (targetAcademicYear == null) {
            targetAcademicYear = AcademicYearUtils.getCurrentAcademicYear();
        }

        final String finalAcadYear = targetAcademicYear;
        StudentAcademicRecord record = studentAcademicRecordRepository
                .findByStudentDocIdAndAcademicYearId(student.getId(), finalAcadYear)
                .orElseGet(() -> StudentAcademicRecord.builder()
                        .studentDocId(student.getId())
                        .academicYearId(finalAcadYear)
                        .createdAt(LocalDateTime.now())
                        .build());

        record.setSchoolId(saved.getSchoolId());
        boolean academicChanged = false;

        if (studentDetails.getStudentId() != null) {
            record.setStudentId(studentDetails.getStudentId());
            academicChanged = true;
        }
        if (studentDetails.getRollNo() != null) {
            record.setRollNo(studentDetails.getRollNo());
            academicChanged = true;
        }
        if (studentDetails.getClassId() != null) {
            record.setClassId(studentDetails.getClassId());
            academicChanged = true;
        }
        if (studentDetails.getSectionId() != null) {
            record.setSectionId(studentDetails.getSectionId());
            academicChanged = true;
        }
        if (studentDetails.getHostelRoomId() != null) {
            record.setHostelRoomId(studentDetails.getHostelRoomId());
            academicChanged = true;
        }
        if (studentDetails.getStatus() != null) {
            record.setStatus(studentDetails.getStatus());
            academicChanged = true;
        }

        if (academicChanged) {
            record.setUpdatedAt(LocalDateTime.now());
            studentAcademicRecordRepository.save(record);
        }

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
                .findByStudentDocIdAndAcademicYearId(studentId, recordDetails.getAcademicYearId())
                .orElseGet(() -> StudentAcademicRecord.builder()
                        .studentDocId(studentId)
                        .academicYearId(recordDetails.getAcademicYearId())
                        .createdAt(LocalDateTime.now())
                        .build());
                        
        record.setSchoolId(student.getSchoolId());
        if (recordDetails.getStudentId() != null) record.setStudentId(recordDetails.getStudentId());
        if (recordDetails.getRollNo() != null) record.setRollNo(recordDetails.getRollNo());
        if (recordDetails.getClassId() != null) record.setClassId(recordDetails.getClassId());
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
        if (promotionDetails.getAcademicYearId() == null || promotionDetails.getAcademicYearId().isEmpty()) {
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
