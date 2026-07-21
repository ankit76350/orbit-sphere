package com.orbitastra.backend.services.student;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;

import com.orbitastra.backend.exceptions.ResourceNotFoundException;
import com.orbitastra.backend.models.core.School;
import com.orbitastra.backend.models.student.Guardian;
import com.orbitastra.backend.models.student.GuardianLink;
import com.orbitastra.backend.models.student.Student;
import com.orbitastra.backend.models.student.StudentAcademicRecord;
import com.orbitastra.backend.repositories.student.GuardianRepository;
import com.orbitastra.backend.repositories.student.StudentRepository;
import com.orbitastra.backend.services.utils.AcademicYearResolver;
import com.orbitastra.backend.repositories.core.SchoolRepository;
import com.orbitastra.backend.repositories.student.StudentAcademicRecordRepository;
import com.orbitastra.backend.models.academics.SchoolClass;
import com.orbitastra.backend.repositories.academics.SchoolClassRepository;
import com.orbitastra.backend.dto.student.CreateStudentRequest;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class StudentService {

    private final StudentRepository studentRepository;
    private final SchoolRepository schoolRepository;
    private final StudentAcademicRecordRepository studentAcademicRecordRepository;
    private final SchoolClassRepository schoolClassRepository;
    private final AcademicYearResolver academicYearResolver;
    private final GuardianRepository guardianRepository;
    private final GuardianService guardianService;

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

    /**
     * Batch variant: resolves the latest academic record for every student in
     * ONE query (findByStudentDocIdIn) instead of one query per student, then
     * attaches the most recent record to each. Avoids the N+1 on list endpoints.
     */
    private void populateAcademicFields(List<Student> students) {
        if (students == null || students.isEmpty()) return;
        List<String> ids = students.stream()
                .map(Student::getId)
                .filter(java.util.Objects::nonNull)
                .toList();
        java.util.Map<String, StudentAcademicRecord> latestByStudent = studentAcademicRecordRepository
                .findByStudentDocIdIn(ids).stream()
                .collect(java.util.stream.Collectors.toMap(
                        StudentAcademicRecord::getStudentDocId,
                        r -> r,
                        (a, b) -> java.util.Comparator
                                .comparing(StudentAcademicRecord::getAcademicYear,
                                        java.util.Comparator.nullsFirst(java.util.Comparator.naturalOrder()))
                                .compare(a, b) >= 0 ? a : b));
        students.forEach(s -> {
            StudentAcademicRecord rec = latestByStudent.get(s.getId());
            if (rec != null) s.setCurrentAcademicRecord(rec);
        });
    }

    @Transactional
    public Student createStudent(CreateStudentRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Request body is required.");
        }
        if (request.getSchoolId() == null || request.getSchoolId().isBlank()) {
            throw new IllegalArgumentException("School ID cannot be null or blank.");
        }
        if (request.getName() == null || request.getName().isBlank()) {
            throw new IllegalArgumentException("Student name cannot be null or blank.");
        }

        // Resolve guardians into de-duplicated links (shared with admission conversion).
        List<GuardianService.GuardianDraft> drafts = request.getGuardians() == null
                ? null
                : request.getGuardians().stream()
                        .map(g -> new GuardianService.GuardianDraft(
                                g.getGuardianId(), g.getName(), g.getPhone(), g.getEmail(),
                                g.getAddress(), g.getOccupation(), g.getRelation(),
                                g.getPrimary(), g.getEmergencyContact(),
                                g.getPickupApproved(), g.getPortalAccess()))
                        .toList();
                        
        List<GuardianLink> links = guardianService.buildDedupedLinks(request.getSchoolId(), null, drafts);

        // Build StudentAcademicRecord from request.getCurrentAcademicRecord() or top-level academic fields
        com.orbitastra.backend.dto.student.AcademicRecordRequest reqRecordDto = request.getCurrentAcademicRecord();
        StudentAcademicRecord reqRecord = reqRecordDto != null ? reqRecordDto.toModel() : null;

        String classDocId = request.getClassDocId() != null ? request.getClassDocId() : request.getClassId();
        if (reqRecord == null && (request.getAcademicYear() != null || classDocId != null || request.getSectionId() != null || request.getRollNo() != null)) {
            reqRecord = StudentAcademicRecord.builder()
                    .academicYear(request.getAcademicYear())
                    .classDocId(classDocId)
                    .sectionId(request.getSectionId())
                    .rollNo(request.getRollNo())
                    .build();
        } else if (reqRecord != null) {
            if (reqRecord.getAcademicYear() == null && request.getAcademicYear() != null) {
                reqRecord.setAcademicYear(request.getAcademicYear());
            }
            if (reqRecord.getClassDocId() == null && classDocId != null) {
                reqRecord.setClassDocId(classDocId);
            }
            if (reqRecord.getSectionId() == null && request.getSectionId() != null) {
                reqRecord.setSectionId(request.getSectionId());
            }
            if (reqRecord.getRollNo() == null && request.getRollNo() != null) {
                reqRecord.setRollNo(request.getRollNo());
            }
        }

        Student student = Student.builder()
                .schoolId(request.getSchoolId())
                .admissionNo(request.getAdmissionNo())
                .name(request.getName())
                .dob(request.getDob())
                .gender(request.getGender())
                .bloodGroup(request.getBloodGroup())
                .photoUrl(request.getPhotoUrl())
                .walletId(request.getWalletId())
                .medicalRecordId(request.getMedicalRecordId())
                .status(request.getStatus() != null ? request.getStatus() : com.orbitastra.backend.models.student.enums.StudentStatus.ACTIVE)
                .admissionDate(request.getAdmissionDate())
                .guardians(links)
                .currentAcademicRecord(reqRecord)
                .build();

        return createStudent(student);
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
            String acadYear = academicYearResolver
                    .resolve(saved.getSchoolId(), reqRecord.getAcademicYear(), saved.getAdmissionDate())
                    .getName();

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

        if (studentDetails.getAdmissionNo() != null && !studentDetails.getAdmissionNo().equals(student.getAdmissionNo())) {
            if (studentRepository.findByAdmissionNo(studentDetails.getAdmissionNo()).isPresent()) {
                throw new IllegalArgumentException("Admission number '" + studentDetails.getAdmissionNo() + "' is already taken.");
            }
            student.setAdmissionNo(studentDetails.getAdmissionNo());
        }

        if (studentDetails.getName() != null) {
            student.setName(studentDetails.getName());
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
            targetAcademicYear = academicYearResolver
                    .resolve(saved.getSchoolId(), null, saved.getAdmissionDate())
                    .getName();
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

        // Repopulate transient fields to reflect latest state
        populateAcademicFields(saved);
        return saved;
    }

    // ----- Guardian links (many-to-many family) -----

    /**
     * Links a guardian to a student with a role + flags. Idempotent per guardian:
     * re-linking the same guardian replaces the existing link (e.g. to change
     * flags). The guardian must exist and belong to the student's school.
     */
    public Student addGuardianLink(String studentId, GuardianLink link) {
        Student student = getStudentById(studentId);
        if (link == null || link.getGuardianId() == null || link.getGuardianId().isBlank()) {
            throw new IllegalArgumentException("guardianId is required to link a guardian.");
        }
        Guardian guardian = guardianRepository.findById(link.getGuardianId())
                .orElseThrow(() -> new ResourceNotFoundException("Guardian not found with id: " + link.getGuardianId()));
        if (!guardian.getSchoolId().equals(student.getSchoolId())) {
            throw new IllegalArgumentException("Guardian does not belong to the same school as the student.");
        }
        if (student.getGuardians() == null) {
            student.setGuardians(new java.util.ArrayList<>());
        }
        student.getGuardians().removeIf(g -> link.getGuardianId().equals(g.getGuardianId()));
        student.getGuardians().add(link);
        student.setUpdatedAt(LocalDateTime.now());
        Student saved = studentRepository.save(student);
        populateAcademicFields(saved);
        return saved;
    }

    public Student removeGuardianLink(String studentId, String guardianId) {
        Student student = getStudentById(studentId);
        if (student.getGuardians() != null) {
            student.getGuardians().removeIf(g -> guardianId.equals(g.getGuardianId()));
            student.setUpdatedAt(LocalDateTime.now());
            studentRepository.save(student);
        }
        populateAcademicFields(student);
        return student;
    }

    public List<Student> getStudentsByGuardian(String guardianId) {
        List<Student> students = studentRepository.findByGuardiansGuardianId(guardianId);
        populateAcademicFields(students);
        return students;
    }

    public void deleteStudent(String id) {
        Student student = getStudentById(id);

        // Delete academic records first
        List<StudentAcademicRecord> records = studentAcademicRecordRepository.findByStudentDocId(id);
        studentAcademicRecordRepository.deleteAll(records);

        studentRepository.delete(student);
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

    /**
     * Siblings = other students who share at least one guardian with this student.
     */
    public List<Student> getSiblings(String studentId) {
        Student student = getStudentById(studentId);

        if (student.getGuardians() == null || student.getGuardians().isEmpty()) {
            return new java.util.ArrayList<>();
        }

        java.util.Map<String, Student> siblings = new java.util.LinkedHashMap<>();
        student.getGuardians().stream()
                .map(GuardianLink::getGuardianId)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .forEach(gid -> studentRepository.findByGuardiansGuardianId(gid).forEach(s -> {
                    if (!s.getId().equals(studentId)) siblings.put(s.getId(), s);
                }));

        List<Student> result = new java.util.ArrayList<>(siblings.values());
        populateAcademicFields(result);
        return result;
    }
}
