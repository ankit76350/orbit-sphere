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
import com.orbitastra.backend.dto.student.AcademicRecordRequest;
import com.orbitastra.backend.dto.student.CreateStudentRequest;
import com.orbitastra.backend.dto.student.StudentResponse;
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

    // ----- Entity + response helpers -----

    /** Loads a student entity or throws. For internal use where the mutable entity is needed. */
    private Student getStudentEntity(String id) {
        return studentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found with id: " + id));
    }

    /** The student's academic record for the current (most recent) academic year, or null. */
    private StudentAcademicRecord latestRecordOf(String studentDocId) {
        if (studentDocId == null) return null;
        return studentAcademicRecordRepository.findByStudentDocId(studentDocId).stream()
                .max(java.util.Comparator.comparing(StudentAcademicRecord::getAcademicYear,
                        java.util.Comparator.nullsFirst(java.util.Comparator.naturalOrder())))
                .orElse(null);
    }

    /** Builds the API view of a student, resolving its current academic record. */
    public StudentResponse buildResponse(Student student) {
        if (student == null) return null;
        return StudentResponse.of(student, latestRecordOf(student.getId()));
    }

    /**
     * Batch view builder: resolves the latest academic record for every student in ONE query
     * (findByStudentDocIdIn) instead of one per student, avoiding the N+1 on list endpoints.
     */
    private List<StudentResponse> buildResponses(List<Student> students) {
        if (students == null || students.isEmpty()) return new java.util.ArrayList<>();
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
        return students.stream()
                .map(s -> StudentResponse.of(s, latestByStudent.get(s.getId())))
                .toList();
    }

    // ----- Create -----

    @Transactional
    public StudentResponse createStudent(CreateStudentRequest request) {
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
                        
        //? buliding or link the Guardian               
        List<GuardianLink> links = guardianService.buildDedupedLinks(request.getSchoolId(), null, drafts);

        // Build the initial academic record from currentAcademicRecord or the top-level fields.
        AcademicRecordRequest reqRecordDto = request.getCurrentAcademicRecord();
        StudentAcademicRecord initialRecord = reqRecordDto != null ? reqRecordDto.toModel() : null;

        String classDocId = request.getClassDocId() != null ? request.getClassDocId() : request.getClassId();
        if (initialRecord == null && (request.getAcademicYear() != null || classDocId != null
                || request.getSectionNo() != null || request.getRollNo() != null)) {
            initialRecord = StudentAcademicRecord.builder()
                    .academicYear(request.getAcademicYear())
                    .classDocId(classDocId)
                    .sectionNo(request.getSectionNo())
                    .rollNo(request.getRollNo())
                    .build();
        } else if (initialRecord != null) {
            if (initialRecord.getAcademicYear() == null && request.getAcademicYear() != null) {
                initialRecord.setAcademicYear(request.getAcademicYear());
            }
            if (initialRecord.getClassDocId() == null && classDocId != null) {
                initialRecord.setClassDocId(classDocId);
            }
            if (initialRecord.getSectionNo() == null && request.getSectionNo() != null) {
                initialRecord.setSectionNo(request.getSectionNo());
            }
            if (initialRecord.getRollNo() == null && request.getRollNo() != null) {
                initialRecord.setRollNo(request.getRollNo());
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
                .build();

        return buildResponse(persistStudent(student, initialRecord));
    }

    /**
     * Persists a student entity and, when supplied, its initial academic record; keeps the
     * student's current-record pointer in sync. Returns the saved entity (not the API view) so
     * callers such as admission conversion can act on the created id.
     */
    public Student persistStudent(Student student, StudentAcademicRecord initialRecord) {
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
        if (initialRecord != null) {
            String acadYear = academicYearResolver
                    .resolve(saved.getSchoolId(), initialRecord.getAcademicYear(), saved.getAdmissionDate())
                    .getName();

            if (initialRecord.getClassDocId() != null) {
                SchoolClass schoolClass = schoolClassRepository.findById(initialRecord.getClassDocId())
                        .orElseThrow(() -> new ResourceNotFoundException("Class not found with id: " + initialRecord.getClassDocId()));
                if (!schoolClass.getSchoolId().equals(saved.getSchoolId())) {
                    throw new IllegalArgumentException("Class does not belong to the same school as the student.");
                }
            }

            StudentAcademicRecord record = StudentAcademicRecord.builder()
                    .schoolId(saved.getSchoolId())
                    .studentDocId(saved.getId())
                    .academicYear(acadYear)
                    .studentNo(initialRecord.getStudentNo())
                    .rollNo(initialRecord.getRollNo())
                    .classDocId(initialRecord.getClassDocId())
                    .sectionNo(initialRecord.getSectionNo())
                    .hostelRoomNo(initialRecord.getHostelRoomNo())
                    .status(saved.getStatus())
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();
            StudentAcademicRecord savedRecord = studentAcademicRecordRepository.save(record);

            // Persist the pointer to the current academic record.
            saved.setCurrentAcademicRecordId(savedRecord.getId());
            saved.setUpdatedAt(LocalDateTime.now());
            saved = studentRepository.save(saved);
        }

        return saved;
    }

    // ----- Reads -----

    public List<StudentResponse> getAllStudents() {
        return buildResponses(studentRepository.findAll());
    }

    public StudentResponse getStudentById(String id) {
        return buildResponse(getStudentEntity(id));
    }

    public StudentResponse getStudentByAdmissionNo(String admissionNo) {
        Student student = studentRepository.findByAdmissionNo(admissionNo)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found with admission number: " + admissionNo));
        return buildResponse(student);
    }

    public List<StudentResponse> getStudentsBySchool(String schoolId) {
        return buildResponses(studentRepository.findBySchoolId(schoolId));
    }

    public List<StudentResponse> getStudentsBySchoolAndAcademicYear(String schoolId, String academicYear) {
        List<StudentAcademicRecord> records =
                studentAcademicRecordRepository.findBySchoolIdAndAcademicYear(schoolId, academicYear);
        List<String> studentIds = records.stream()
                .map(StudentAcademicRecord::getStudentDocId)
                .distinct()
                .toList();
        List<Student> students = studentRepository.findAllById(studentIds);

        // Attach the record for THIS academic year (not the latest, as buildResponses would).
        java.util.Map<String, StudentAcademicRecord> recordByStudent = records.stream()
                .collect(java.util.stream.Collectors.toMap(
                        StudentAcademicRecord::getStudentDocId, r -> r, (a, b) -> a));
        return students.stream()
                .map(s -> StudentResponse.of(s, recordByStudent.get(s.getId())))
                .toList();
    }

    public List<StudentResponse> getStudentsByClass(String classId) {
        List<StudentAcademicRecord> records = studentAcademicRecordRepository.findByClassDocId(classId);
        List<String> studentIds = records.stream()
                .map(StudentAcademicRecord::getStudentDocId)
                .distinct()
                .toList();
        return buildResponses(studentRepository.findAllById(studentIds));
    }

    public List<StudentResponse> getStudentsByHostelRoom(String hostelRoomNo) {
        List<StudentAcademicRecord> records = studentAcademicRecordRepository.findByHostelRoomNo(hostelRoomNo);
        List<String> studentIds = records.stream()
                .map(StudentAcademicRecord::getStudentDocId)
                .distinct()
                .toList();
        return buildResponses(studentRepository.findAllById(studentIds));
    }

    public List<StudentResponse> getStudentsByGuardian(String guardianId) {
        return buildResponses(studentRepository.findByGuardiansGuardianId(guardianId));
    }

    // ----- Update -----

    public StudentResponse updateStudent(String id, Student studentDetails, StudentAcademicRecord detailsRecord) {
        Student student = getStudentEntity(id);

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
        StudentAcademicRecord currentRecord = latestRecordOf(student.getId());
        String targetAcademicYear = (detailsRecord != null && detailsRecord.getAcademicYear() != null)
                ? detailsRecord.getAcademicYear()
                : (currentRecord != null ? currentRecord.getAcademicYear() : null);
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
            if (detailsRecord.getStudentNo() != null) {
                record.setStudentNo(detailsRecord.getStudentNo());
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
            if (detailsRecord.getSectionNo() != null) {
                record.setSectionNo(detailsRecord.getSectionNo());
                academicChanged = true;
            }
            if (detailsRecord.getHostelRoomNo() != null) {
                record.setHostelRoomNo(detailsRecord.getHostelRoomNo());
                academicChanged = true;
            }
        }
        if (studentDetails.getStatus() != null) {
            record.setStatus(studentDetails.getStatus());
            academicChanged = true;
        }

        if (academicChanged) {
            record.setUpdatedAt(LocalDateTime.now());
            studentAcademicRecordRepository.save(record);
            syncCurrentAcademicRecordPointer(saved);
        }

        return buildResponse(saved);
    }

    // ----- Guardian links (many-to-many family) -----

    /**
     * Links a guardian to a student with a role + flags. Idempotent per guardian:
     * re-linking the same guardian replaces the existing link (e.g. to change
     * flags). The guardian must exist and belong to the student's school.
     */
    public StudentResponse addGuardianLink(String studentId, GuardianLink link) {
        Student student = getStudentEntity(studentId);
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
        return buildResponse(saved);
    }

    public StudentResponse removeGuardianLink(String studentId, String guardianId) {
        Student student = getStudentEntity(studentId);
        if (student.getGuardians() != null) {
            student.getGuardians().removeIf(g -> guardianId.equals(g.getGuardianId()));
            student.setUpdatedAt(LocalDateTime.now());
            student = studentRepository.save(student);
        }
        return buildResponse(student);
    }

    public void deleteStudent(String id) {
        Student student = getStudentEntity(id);

        // Delete academic records first
        List<StudentAcademicRecord> records = studentAcademicRecordRepository.findByStudentDocId(id);
        studentAcademicRecordRepository.deleteAll(records);

        studentRepository.delete(student);
    }

    // ----- Academic records -----

    public StudentAcademicRecord createOrUpdateAcademicRecord(String studentId, StudentAcademicRecord recordDetails) {
        Student student = getStudentEntity(studentId); // Throws if not found

        StudentAcademicRecord record = studentAcademicRecordRepository
                .findByStudentDocIdAndAcademicYear(studentId, recordDetails.getAcademicYear())
                .orElseGet(() -> StudentAcademicRecord.builder()
                        .studentDocId(studentId)
                        .academicYear(recordDetails.getAcademicYear())
                        .createdAt(LocalDateTime.now())
                        .build());

        record.setSchoolId(student.getSchoolId());
        if (recordDetails.getStudentNo() != null) record.setStudentNo(recordDetails.getStudentNo());
        if (recordDetails.getRollNo() != null) record.setRollNo(recordDetails.getRollNo());
        if (recordDetails.getClassDocId() != null) {
            SchoolClass schoolClass = schoolClassRepository.findById(recordDetails.getClassDocId())
                    .orElseThrow(() -> new ResourceNotFoundException("Class not found with id: " + recordDetails.getClassDocId()));
            if (!schoolClass.getSchoolId().equals(student.getSchoolId())) {
                throw new IllegalArgumentException("Class does not belong to the same school as the student.");
            }
            record.setClassDocId(recordDetails.getClassDocId());
        }
        if (recordDetails.getSectionNo() != null) record.setSectionNo(recordDetails.getSectionNo());
        if (recordDetails.getHostelRoomNo() != null) record.setHostelRoomNo(recordDetails.getHostelRoomNo());
        if (recordDetails.getStatus() != null) {
            record.setStatus(recordDetails.getStatus());
        } else {
            record.setStatus(student.getStatus());
        }
        record.setUpdatedAt(LocalDateTime.now());
        StudentAcademicRecord saved = studentAcademicRecordRepository.save(record);

        // Keep the student's pointer to the current academic year in sync.
        syncCurrentAcademicRecordPointer(student);
        return saved;
    }

    /**
     * Recomputes and persists {@link Student#getCurrentAcademicRecordId()} so it points at the
     * student's record for the most recent academic year. No-op when already correct. Call after
     * any create / assign / promote of a {@link StudentAcademicRecord}.
     */
    private void syncCurrentAcademicRecordPointer(Student student) {
        String latestId = studentAcademicRecordRepository.findByStudentDocId(student.getId()).stream()
                .max(java.util.Comparator.comparing(StudentAcademicRecord::getAcademicYear,
                        java.util.Comparator.nullsFirst(java.util.Comparator.naturalOrder())))
                .map(StudentAcademicRecord::getId)
                .orElse(null);
        if (!java.util.Objects.equals(student.getCurrentAcademicRecordId(), latestId)) {
            student.setCurrentAcademicRecordId(latestId);
            student.setUpdatedAt(LocalDateTime.now());
            studentRepository.save(student);
        }
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
    public List<StudentResponse> getSiblings(String studentId) {
        Student student = getStudentEntity(studentId);

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

        return buildResponses(new java.util.ArrayList<>(siblings.values()));
    }
}
