package com.orbitastra.backend.services.student;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.orbitastra.backend.dto.student.AcademicRecordRequest;
import com.orbitastra.backend.dto.student.CreateStudentRequest;
import com.orbitastra.backend.dto.student.StudentGuardianRequest;
import com.orbitastra.backend.dto.student.StudentResponse;
import com.orbitastra.backend.dto.student.UpdateAcademicRecordRequest;
import com.orbitastra.backend.exceptions.ConflictException;
import com.orbitastra.backend.exceptions.ResourceNotFoundException;
import com.orbitastra.backend.models.academics.SchoolClass;
import com.orbitastra.backend.models.core.AcademicYear;
import com.orbitastra.backend.models.core.School;
import com.orbitastra.backend.models.student.Guardian;
import com.orbitastra.backend.models.student.Student;
import com.orbitastra.backend.models.student.StudentAcademicRecord;
import com.orbitastra.backend.models.student.embedded.GuardianLink;
import com.orbitastra.backend.models.student.enums.StudentStatus;
import com.orbitastra.backend.repositories.academics.SchoolClassRepository;
import com.orbitastra.backend.repositories.core.SchoolRepository;
import com.orbitastra.backend.repositories.student.GuardianRepository;
import com.orbitastra.backend.repositories.student.StudentAcademicRecordRepository;
import com.orbitastra.backend.repositories.student.StudentRepository;
import com.orbitastra.backend.services.utils.AcademicYearResolver;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Handles students and their year-by-year school records (class, section, roll number, etc.).
 *
 * The student row itself keeps only the student's own details plus the id of the record for the
 * current year. When we send a student back to the app we build a {@link StudentResponse}, which
 * also includes the full current-year record.
 *
 * The "create a student" steps are logged one by one, so you can follow along in the logs and
 * see exactly what is happening at each point.
 */
@Slf4j
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

    // =======================================================================================
    // CREATE a student  — POST /api/students
    // =======================================================================================

    /**
     * Creates a student, plus their guardians and their first-year record if those were sent.
     * Everything runs together: if any step fails, nothing is saved.
     */
    @Transactional
    public StudentResponse createStudent(CreateStudentRequest request) {
        log.info("========== [createStudent] Starting to create a new student ==========");

        // Step 1 — make sure the request has the details we need.
        log.info("[createStudent] Step 1: Checking the request has the required details");
        if (request == null) {
            throw new IllegalArgumentException("Request body is required.");
        }
        if (request.getSchoolId() == null || request.getSchoolId().isBlank()) {
            throw new IllegalArgumentException("schoolId cannot be null or blank.");
        }
        if (request.getAdmissionNo() == null || request.getAdmissionNo().isBlank()) {
            throw new IllegalArgumentException("admissionNo cannot be null or blank.");
        }
        if (request.getName() == null || request.getName().isBlank()) {
            throw new IllegalArgumentException("Student name cannot be null or blank.");
        }
        log.info("[createStudent] Request looks good — school={}, name='{}', admissionNo={}",
                request.getSchoolId(), request.getName(), request.getAdmissionNo());

        // Step 2 — validate and build the first-year record before any related
        // guardian records can be written.
        log.info("[createStudent] Step 2: Building the student's first academic-year record (if any was sent)");
        StudentAcademicRecord initialRecord = assembleInitialRecord(request);
        if (initialRecord != null) {
            log.info("[createStudent] Academic record to create — year={}, class={}, section={}, roll={}",
                    initialRecord.getAcademicYear(), initialRecord.getClassDocsId(),
                    initialRecord.getSectionNo(), initialRecord.getRollNo());
        } else {
            log.info("[createStudent] No class/year details sent — the student will be created without a record for now");
        }

        // Step 3 — turn the guardians in the request into links (and avoid duplicates).
        log.info("[createStudent] Step 3: Preparing the student's guardians");
        List<GuardianService.GuardianDraft> drafts = toGuardianDrafts(request.getGuardians());
        List<GuardianLink> guardianLinks = guardianService.buildDedupedLinks(request.getSchoolId(), null, drafts);

        // Step 4 — put together the student object we are going to save.
        log.info("[createStudent] Step 4: Building the student object");
        Student student = Student.builder()
                .schoolId(request.getSchoolId())
                .admissionNo(request.getAdmissionNo())
                .admissionDocsId(null)
                .name(request.getName())
                .dob(request.getDob())
                .gender(request.getGender())
                .bloodGroup(request.getBloodGroup())
                .photoUrl(request.getPhotoUrl())
                .walletDocsId(request.getWalletDocsId())
                .medicalRecordDocsId(request.getMedicalRecordDocsId())
                .documents(copyStrings(request.getDocuments()))
                .medicalRemark(copyStrings(request.getMedicalRemark()))
                .status(request.getStatus() != null ? request.getStatus() : StudentStatus.ACTIVE)
                .admissionDate(request.getAdmissionDate())
                .guardians(guardianLinks)
                .build();

        // Step 5 — run the checks and save everything to the database.
        log.info("[createStudent] Step 5: Saving the student");
        Student saved = persistStudent(student, initialRecord);

        // Step 6 — build the reply to send back to the app.
        log.info("[createStudent] Step 6: Building the response to send back");
        StudentResponse response = buildResponse(saved);

        log.info("========== [createStudent] Done — new student id={} ==========", saved.getId());
        return response;
    }

    /**
     * Does the actual saving: runs the checks, saves the student, saves the first-year record if
     * given, and remembers which record is the current one. Returns the saved student.
     * Used both when creating a student directly and when turning an admission into a student.
     */
    @Transactional
    public Student persistStudent(Student student, StudentAcademicRecord initialRecord) {
        if (student == null) {
            throw new IllegalArgumentException("Student details are required.");
        }
        String schoolId = normalizeRequired(student.getSchoolId(), "schoolId");
        String admissionNo = normalizeRequired(student.getAdmissionNo(), "admissionNo");
        String studentName = normalizeRequired(student.getName(), "Student name");
        student.setSchoolId(schoolId);
        student.setAdmissionNo(admissionNo);
        student.setName(studentName);
        if (student.getAdmissionDocsId() != null) {
            String admissionDocsId = student.getAdmissionDocsId().trim();
            student.setAdmissionDocsId(admissionDocsId.isEmpty() ? null : admissionDocsId);
        }
        String requestedInitialAcademicYear = null;
        if (initialRecord != null) {
            requestedInitialAcademicYear = normalizeOptional(initialRecord.getAcademicYear());
            if (requestedInitialAcademicYear == null) {
                throw new IllegalArgumentException(
                        "currentAcademicRecord.academicYear is required when currentAcademicRecord contains academic details.");
            }
        }

        // 5a — the school must exist.
        log.info("[persistStudent] 5a: Looking up the school {}", schoolId);
        School school = schoolRepository.findById(schoolId)
                .orElseThrow(() -> new ResourceNotFoundException("School not found with id: " + schoolId));

        // 5b — the school may have a limit on how many students it can have.
        if (school.getMaxStudents() != null) {
            long current = studentRepository.countBySchoolId(student.getSchoolId());
            log.info("[persistStudent] 5b: This school has {} of {} allowed students", current, school.getMaxStudents());
            if (current >= school.getMaxStudents()) {
                throw new IllegalArgumentException("You have exceeded the package limit for maximum students.");
            }
        }

        // 5c — no two students can share the same admission number.
        log.info("[persistStudent] 5c: Making sure admission number '{}' is not already used", admissionNo);
        if (studentRepository.findByAdmissionNo(admissionNo).isPresent()) {
            throw duplicateStudentAdmissionNo(admissionNo, null);
        }

        // A single admission may produce at most one student. Direct student creation
        // leaves admissionDocsId null, so it does not participate in this check/index.
        if (student.getAdmissionDocsId() != null && !student.getAdmissionDocsId().isBlank()
                && studentRepository.findByAdmissionDocsId(student.getAdmissionDocsId()).isPresent()) {
            throw new ConflictException(
                    "Admission " + student.getAdmissionDocsId() + " has already been converted to a student.");
        }

        // Validate and normalize the optional initial academic placement before
        // writing the student, so an invalid class/section or duplicate
        // identityNo/rollNo rejects the whole creation without a partial record.
        StudentAcademicRecord preparedInitialRecord = null;
        if (initialRecord != null) {
            String academicYear = academicYearResolver
                    .resolve(student.getSchoolId(), requestedInitialAcademicYear, student.getAdmissionDate())
                    .getName();
            preparedInitialRecord = StudentAcademicRecord.builder()
                    .schoolId(student.getSchoolId())
                    .studentDocsId(student.getId())
                    .academicYear(academicYear)
                    .identityNo(initialRecord.getIdentityNo())
                    .rollNo(initialRecord.getRollNo())
                    .classDocsId(initialRecord.getClassDocsId())
                    .sectionNo(initialRecord.getSectionNo())
                    .hostelRoomNo(initialRecord.getHostelRoomNo())
                    .status(initialRecord.getStatus() != null
                            ? initialRecord.getStatus()
                            : (student.getStatus() != null ? student.getStatus() : StudentStatus.ACTIVE))
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();
            normalizeAndValidateAcademicRecord(student, preparedInitialRecord);
            rejectAcademicIdentifierConflicts(student, preparedInitialRecord);
        }

        // 5d — save the student.
        student.setDocuments(copyStrings(student.getDocuments()));
        student.setMedicalRemark(copyStrings(student.getMedicalRemark()));
        student.setCreatedAt(LocalDateTime.now());
        student.setUpdatedAt(LocalDateTime.now());
        Student saved;
        try {
            saved = studentRepository.save(student);
        } catch (DuplicateKeyException ex) {
            if (duplicateKeyReferences(ex, "admissionDocsId")
                    && student.getAdmissionDocsId() != null) {
                throw duplicateAdmissionConversion(student.getAdmissionDocsId(), ex);
            }
            if (duplicateKeyReferences(ex, "admissionNo")) {
                throw duplicateStudentAdmissionNo(admissionNo, ex);
            }
            throw new ConflictException("A student with the same unique reference already exists.", ex);
        }
        log.info("[persistStudent] 5d: Saved the student (id={}) with {} guardian(s)",
                saved.getId(), saved.getGuardians() == null ? 0 : saved.getGuardians().size());

        // 5e — if class/year details were sent, save the academic-year record.
        if (preparedInitialRecord != null) {
            log.info("[persistStudent] 5e: Creating the student's academic-year record");
            log.info("[persistStudent] Using academic year {}", preparedInitialRecord.getAcademicYear());
            preparedInitialRecord.setStudentDocsId(saved.getId());

            StudentAcademicRecord savedRecord;
            try {
                savedRecord = studentAcademicRecordRepository.save(preparedInitialRecord);
            } catch (DuplicateKeyException ex) {
                throw duplicateAcademicRecordConflict(saved, preparedInitialRecord, ex);
            }
            log.info("[persistStudent] Saved the academic-year record (id={})", savedRecord.getId());

            // 5f — remember this as the student's current-year record.
            saved.setCurrentAcademicRecordDocsId(savedRecord.getId());
            saved.setUpdatedAt(LocalDateTime.now());
            saved = studentRepository.save(saved);
            log.info("[persistStudent] 5f: Set the student's current record to {}", savedRecord.getId());
        }

        return saved;
    }

    /** Copies the guardians from the request into the simple "draft" shape the guardian code uses. */
    private List<GuardianService.GuardianDraft> toGuardianDrafts(List<StudentGuardianRequest> guardians) {
        if (guardians == null) return null;
        return guardians.stream()
                .map(g -> new GuardianService.GuardianDraft(
                        g.getGuardianDocsId(), g.getName(), g.getPhone(), g.getEmail(),
                        g.getAddress(), g.getOccupation(), g.getRelation(),
                        g.getPrimary(), g.getEmergencyContact(),
                        g.getPickupApproved(), g.getPortalAccess()))
                .toList();
    }

    /** Builds the first-year record from the nested currentAcademicRecord payload, if present. */
    private StudentAcademicRecord assembleInitialRecord(CreateStudentRequest request) {
        AcademicRecordRequest dto = request.getCurrentAcademicRecord();
        return dto == null ? null : dto.toModel();
    }

    // =======================================================================================
    // Building the reply we send back to the app
    // =======================================================================================

    /** Loads a student from the database, or throws if there is no student with that id. */
    private Student getStudentEntity(String id) {
        return studentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found with id: " + id));
    }

    /** Finds the current pointer first, then falls back to the newest active placement. */
    private StudentAcademicRecord currentRecordOf(Student student) {
        if (student == null || student.getId() == null) return null;
        if (student.getCurrentAcademicRecordDocsId() != null
                && !student.getCurrentAcademicRecordDocsId().isBlank()) {
            var pointed = studentAcademicRecordRepository.findById(student.getCurrentAcademicRecordDocsId());
            if (pointed.isPresent()
                    && pointed.get().getStatus() == StudentStatus.ACTIVE) {
                return pointed.get();
            }
        }
        return studentAcademicRecordRepository.findByStudentDocsId(student.getId()).stream()
                .filter(record -> record.getStatus() == StudentStatus.ACTIVE)
                .max(academicRecordRecencyComparator())
                .orElse(null);
    }

    /** Builds the reply for one student, including their current-year record. */
    public StudentResponse buildResponse(Student student) {
        if (student == null) return null;
        return StudentResponse.of(student, currentRecordOf(student));
    }

    /**
     * Builds the reply for a list of students. It fetches everyone's records in a single database
     * call (instead of one call per student) so it stays fast even for long lists.
     */
    private List<StudentResponse> buildResponses(List<Student> students) {
        if (students == null || students.isEmpty()) return new ArrayList<>();
        List<String> ids = students.stream().map(Student::getId).filter(Objects::nonNull).toList();
        List<StudentAcademicRecord> records = studentAcademicRecordRepository.findByStudentDocsIdIn(ids);
        Map<String, StudentAcademicRecord> activeRecordsById = records.stream()
                .filter(record -> record.getStatus() == StudentStatus.ACTIVE)
                .filter(record -> record.getId() != null)
                .collect(Collectors.toMap(StudentAcademicRecord::getId, record -> record, (a, b) -> a));
        Map<String, StudentAcademicRecord> latestActive = records.stream()
                .filter(record -> record.getStatus() == StudentStatus.ACTIVE)
                .collect(Collectors.toMap(
                        StudentAcademicRecord::getStudentDocsId,
                        record -> record,
                        (a, b) -> academicRecordRecencyComparator().compare(a, b) >= 0 ? a : b));
        return students.stream().map(student -> {
            StudentAcademicRecord current = student.getCurrentAcademicRecordDocsId() == null
                    ? null
                    : activeRecordsById.get(student.getCurrentAcademicRecordDocsId());
            return StudentResponse.of(
                    student,
                    current != null ? current : latestActive.get(student.getId()));
        }).toList();
    }

    // =======================================================================================
    // Reading students
    // =======================================================================================

    public List<StudentResponse> getAllStudents() {
        return buildResponses(studentRepository.findAll());
    }

    public StudentResponse getStudentById(String id) {
        return buildResponse(getStudentEntity(id));
    }

    public StudentResponse getStudentByAdmissionNo(String admissionNo) {
        Student student = studentRepository.findByAdmissionNo(admissionNo)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Student not found with admission number: " + admissionNo));
        return buildResponse(student);
    }

    public List<StudentResponse> getStudentsBySchool(String schoolId) {
        return buildResponses(studentRepository.findBySchoolId(schoolId));
    }

    public List<StudentResponse> getStudentsBySchoolAndAcademicYear(String schoolId, String academicYear) {
        List<StudentAcademicRecord> records =
                studentAcademicRecordRepository.findBySchoolIdAndAcademicYear(schoolId, academicYear);
        List<String> ids = records.stream().map(StudentAcademicRecord::getStudentDocsId).distinct().toList();
        List<Student> students = studentRepository.findAllById(ids);
        // Show the record for the year that was asked for (not the newest one).
        Map<String, StudentAcademicRecord> byStudent = records.stream()
                .collect(Collectors.toMap(
                        StudentAcademicRecord::getStudentDocsId,
                        record -> record,
                        (a, b) -> academicRecordRecencyComparator().compare(a, b) >= 0 ? a : b));
        return students.stream().map(s -> StudentResponse.of(s, byStudent.get(s.getId()))).toList();
    }

    public List<StudentResponse> getStudentsByClass(String classDocsId) {
        List<String> ids = studentAcademicRecordRepository
                .findByClassDocsIdAndStatus(classDocsId, StudentStatus.ACTIVE).stream()
                .map(StudentAcademicRecord::getStudentDocsId).distinct().toList();
        return buildResponses(studentRepository.findAllById(ids));
    }

    public List<StudentResponse> getStudentsByHostelRoom(String hostelRoomNo) {
        List<String> ids = studentAcademicRecordRepository
                .findByHostelRoomNoAndStatus(hostelRoomNo, StudentStatus.ACTIVE).stream()
                .map(StudentAcademicRecord::getStudentDocsId).distinct().toList();
        return buildResponses(studentRepository.findAllById(ids));
    }

    public List<StudentResponse> getStudentsByGuardian(String guardianDocsId) {
        return buildResponses(studentRepository.findByGuardiansGuardianDocsId(guardianDocsId));
    }

    // =======================================================================================
    // Updating a student
    // =======================================================================================

    @Transactional
    public StudentResponse updateStudent(String id, Student details, StudentAcademicRecord detailsRecord) {
        Student student = getStudentEntity(id);

        // If the school is being changed, make sure the new school exists.
        if (details.getSchoolId() != null && !details.getSchoolId().equals(student.getSchoolId())) {
            if (!schoolRepository.existsById(details.getSchoolId())) {
                throw new ResourceNotFoundException("School not found with id: " + details.getSchoolId());
            }
            student.setSchoolId(details.getSchoolId());
        }
        // If the admission number is being changed, make sure it is not already used.
        if (details.getAdmissionNo() != null && !details.getAdmissionNo().equals(student.getAdmissionNo())) {
            if (studentRepository.findByAdmissionNo(details.getAdmissionNo()).isPresent()) {
                throw new IllegalArgumentException(
                        "Admission number '" + details.getAdmissionNo() + "' is already taken.");
            }
            student.setAdmissionNo(details.getAdmissionNo());
        }
        // Only change the fields the caller actually sent (non-null ones).
        if (details.getName() != null) student.setName(details.getName());
        if (details.getDob() != null) student.setDob(details.getDob());
        if (details.getGender() != null) student.setGender(details.getGender());
        if (details.getBloodGroup() != null) student.setBloodGroup(details.getBloodGroup());
        if (details.getPhotoUrl() != null) student.setPhotoUrl(details.getPhotoUrl());
        if (details.getWalletDocsId() != null) student.setWalletDocsId(details.getWalletDocsId());
        if (details.getMedicalRecordDocsId() != null) student.setMedicalRecordDocsId(details.getMedicalRecordDocsId());
        if (details.getDocuments() != null) student.setDocuments(copyStrings(details.getDocuments()));
        if (details.getMedicalRemark() != null) student.setMedicalRemark(copyStrings(details.getMedicalRemark()));
        if (details.getStatus() != null) student.setStatus(details.getStatus());
        if (details.getAdmissionDate() != null) student.setAdmissionDate(details.getAdmissionDate());

        student.setUpdatedAt(LocalDateTime.now());
        Student saved = studentRepository.save(student);

        // Work out which school year's record to update: the one sent, else the current one.
        StudentAcademicRecord currentRecord = currentRecordOf(student);
        String targetYear = (detailsRecord != null && detailsRecord.getAcademicYear() != null)
                ? detailsRecord.getAcademicYear()
                : (currentRecord != null ? currentRecord.getAcademicYear() : null);
        if (targetYear == null) {
            targetYear = academicYearResolver.resolve(saved.getSchoolId(), null, saved.getAdmissionDate()).getName();
        }

        // Find that year's record, or start a new one if it does not exist yet.
        final String year = academicYearResolver
                .resolve(saved.getSchoolId(), targetYear, saved.getAdmissionDate())
                .getName();
        StudentAcademicRecord record = studentAcademicRecordRepository
                .findFirstByStudentDocsIdAndAcademicYearAndStatusOrderByCreatedAtDesc(
                        student.getId(), year, StudentStatus.ACTIVE)
                .orElseGet(() -> StudentAcademicRecord.builder()
                        .studentDocsId(student.getId())
                        .academicYear(year)
                        .createdAt(LocalDateTime.now())
                        .build());
        record.setSchoolId(saved.getSchoolId());
        boolean changed = false;

        // Update only the record fields that were sent.
        if (detailsRecord != null) {
            if (detailsRecord.getIdentityNo() != null) {
                record.setIdentityNo(normalizeOptional(detailsRecord.getIdentityNo()));
                changed = true;
            }
            if (detailsRecord.getRollNo() != null) {
                record.setRollNo(normalizeOptional(detailsRecord.getRollNo()));
                changed = true;
            }
            if (detailsRecord.getClassDocsId() != null) {
                String requestedClassDocsId = normalizeOptional(detailsRecord.getClassDocsId());
                boolean classChanged =
                        !Objects.equals(normalizeOptional(record.getClassDocsId()), requestedClassDocsId);
                record.setClassDocsId(requestedClassDocsId);
                if (classChanged && detailsRecord.getSectionNo() == null) {
                    record.setSectionNo(null);
                }
                changed = true;
            }
            if (detailsRecord.getSectionNo() != null) {
                record.setSectionNo(normalizeOptional(detailsRecord.getSectionNo()));
                changed = true;
            }
            if (detailsRecord.getHostelRoomNo() != null) {
                record.setHostelRoomNo(normalizeOptional(detailsRecord.getHostelRoomNo()));
                changed = true;
            }
        }
        if (details.getStatus() != null) { record.setStatus(details.getStatus()); changed = true; }

        // Only save the record if something in it actually changed.
        if (changed) {
            record.setUpdatedAt(LocalDateTime.now());
            normalizeAndValidateAcademicRecord(saved, record);
            rejectAcademicIdentifierConflicts(saved, record);
            final StudentAcademicRecord savedRecord;
            try {
                savedRecord = studentAcademicRecordRepository.save(record);
            } catch (DuplicateKeyException ex) {
                throw duplicateAcademicRecordConflict(saved, record, ex);
            }
            setCurrentAcademicRecordPointer(saved, savedRecord);
        }
        return buildResponse(saved);
    }

    private List<String> copyStrings(List<String> values) {
        return values == null ? new ArrayList<>() : new ArrayList<>(values);
    }

    private String normalizeRequired(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " cannot be null or blank.");
        }
        return value.trim();
    }

    private ConflictException duplicateStudentAdmissionNo(String admissionNo, Throwable cause) {
        String message = "A student already exists with admissionNo: " + admissionNo;
        return cause == null ? new ConflictException(message) : new ConflictException(message, cause);
    }

    private ConflictException duplicateAdmissionConversion(String admissionDocsId, Throwable cause) {
        String message = "Admission " + admissionDocsId + " has already been converted to a student.";
        return cause == null ? new ConflictException(message) : new ConflictException(message, cause);
    }

    private boolean duplicateKeyReferences(DuplicateKeyException ex, String fieldName) {
        return ex.getMessage() != null && ex.getMessage().contains(fieldName);
    }

    // =======================================================================================
    // Guardians attached to a student
    // =======================================================================================

    /**
     * Attaches a guardian to a student. If that guardian is already attached, the old link is
     * replaced (handy for changing their role or flags). The guardian must exist and be in the
     * same school as the student.
     */
    public StudentResponse addGuardianLink(String studentDocsId, GuardianLink link) {
        Student student = getStudentEntity(studentDocsId);
        if (link == null || link.getGuardianDocsId() == null || link.getGuardianDocsId().isBlank()) {
            throw new IllegalArgumentException("guardianDocsId is required to link a guardian.");
        }
        Guardian guardian = guardianRepository.findById(link.getGuardianDocsId())
                .orElseThrow(() -> new ResourceNotFoundException("Guardian not found with id: " + link.getGuardianDocsId()));
        if (!guardian.getSchoolId().equals(student.getSchoolId())) {
            throw new IllegalArgumentException("Guardian does not belong to the same school as the student.");
        }
        if (student.getGuardians() == null) student.setGuardians(new ArrayList<>());
        // Remove any existing link to this guardian first, then add the new one.
        student.getGuardians().removeIf(g -> link.getGuardianDocsId().equals(g.getGuardianDocsId()));
        student.getGuardians().add(link);
        student.setUpdatedAt(LocalDateTime.now());
        return buildResponse(studentRepository.save(student));
    }

    public StudentResponse removeGuardianLink(String studentDocsId, String guardianDocsId) {
        Student student = getStudentEntity(studentDocsId);
        // Only save if a link was actually removed.
        if (student.getGuardians() != null
                && student.getGuardians().removeIf(g -> guardianDocsId.equals(g.getGuardianDocsId()))) {
            student.setUpdatedAt(LocalDateTime.now());
            student = studentRepository.save(student);
        }
        return buildResponse(student);
    }

    // =======================================================================================
    // Academic-year records (class / section / roll number for a school year)
    // =======================================================================================

    /**
     * Creates a new academic placement-history record and makes it the student's
     * current record. Every existing academic record for the student is retained
     * as history after being marked INACTIVE.
     */
    @Transactional
    public StudentAcademicRecord createAcademicRecord(String studentDocsId, StudentAcademicRecord details) {
        String normalizedStudentDocsId = normalizeRequired(studentDocsId, "studentDocsId");
        if (details == null) {
            throw new IllegalArgumentException("Academic record details are required.");
        }

        String requestedYear = normalizeRequired(details.getAcademicYear(), "academicYear");
        Student student = getStudentEntity(normalizedStudentDocsId);
        String studentSchoolId = normalizeRequired(student.getSchoolId(), "Student schoolId");

        // A record may only reference an academic year owned by the student's school.
        AcademicYear academicYear = academicYearResolver.resolve(studentSchoolId, requestedYear, null);
        if (academicYear == null || academicYear.getName() == null || academicYear.getName().isBlank()) {
            throw new IllegalArgumentException("Academic year could not be resolved for this school.");
        }
        String yearName = academicYear.getName();

        StudentAcademicRecord previousActiveForYear = studentAcademicRecordRepository
                .findFirstByStudentDocsIdAndAcademicYearAndStatusOrderByCreatedAtDesc(
                        normalizedStudentDocsId, yearName, StudentStatus.ACTIVE)
                .orElse(null);
        List<StudentAcademicRecord> existingRecords =
                studentAcademicRecordRepository.findByStudentDocsId(normalizedStudentDocsId);

        if (details.getStatus() != null && details.getStatus() != StudentStatus.ACTIVE) {
            throw new IllegalArgumentException(
                    "A newly assigned current academic record must have status ACTIVE.");
        }

        StudentAcademicRecord record = StudentAcademicRecord.builder()
                .schoolId(studentSchoolId)
                .studentDocsId(normalizedStudentDocsId)
                .academicYear(yearName)
                .identityNo(normalizeOptional(details.getIdentityNo()) != null
                        ? normalizeOptional(details.getIdentityNo())
                        : previousActiveForYear != null
                                ? normalizeOptional(previousActiveForYear.getIdentityNo())
                                : null)
                .rollNo(details.getRollNo())
                .classDocsId(details.getClassDocsId())
                .sectionNo(details.getSectionNo())
                .hostelRoomNo(details.getHostelRoomNo())
                .status(StudentStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        normalizeAndValidateAcademicRecord(student, record);
        rejectAcademicIdentifierConflicts(student, record);

        for (StudentAcademicRecord existingRecord : existingRecords) {
            deactivateAcademicRecord(existingRecord);
        }
        if (previousActiveForYear != null
                && existingRecords.stream()
                        .noneMatch(existing -> sameAcademicRecord(existing, previousActiveForYear))) {
            // Protect against an inconsistent query result while preserving the
            // one-active-record-per-student/year database constraint.
            deactivateAcademicRecord(previousActiveForYear);
        }

        final StudentAcademicRecord saved;
        try {
            saved = studentAcademicRecordRepository.save(record);
        } catch (DuplicateKeyException ex) {
            throw duplicateAcademicRecordConflict(student, record, ex);
        }

        // Keep the current-record pointer and the history write in one transaction.
        setCurrentAcademicRecordPointer(student, saved);
        return saved;
    }

    /**
     * Partially updates only the client-editable fields of an existing academic
     * record. Ownership, academic year, ids, and audit timestamps remain
     * server-controlled.
     */
    @Transactional
    public StudentAcademicRecord updateAcademicRecord(
            String studentDocsId,
            String academicRecordDocsId,
            UpdateAcademicRecordRequest request) {
        String normalizedStudentDocsId = normalizeRequired(studentDocsId, "studentDocsId");
        String normalizedRecordDocsId =
                normalizeRequired(academicRecordDocsId, "academicRecordDocsId");
        if (request == null) {
            throw new IllegalArgumentException("Academic record update body is required.");
        }
        if (!request.hasUpdates()) {
            throw new IllegalArgumentException(
                    "At least one editable academic-record field is required.");
        }

        Student student = getStudentEntity(normalizedStudentDocsId);
        StudentAcademicRecord record = studentAcademicRecordRepository
                .findById(normalizedRecordDocsId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Academic record not found with id: " + normalizedRecordDocsId));
        if (!normalizedStudentDocsId.equals(record.getStudentDocsId())) {
            throw new IllegalArgumentException(
                    "Academic record does not belong to student: " + normalizedStudentDocsId);
        }

        String studentSchoolId = normalizeRequired(student.getSchoolId(), "Student schoolId");
        if (record.getSchoolId() != null
                && !studentSchoolId.equals(record.getSchoolId())) {
            throw new IllegalArgumentException(
                    "Academic record does not belong to the same school as the student.");
        }
        if (record.getStatus() != StudentStatus.ACTIVE) {
            throw new ConflictException(
                    "Academic record '" + normalizedRecordDocsId + "' is "
                            + (record.getStatus() == null ? "not ACTIVE" : record.getStatus())
                            + " and cannot be edited. Only ACTIVE academic records are editable.");
        }
        String academicYearName =
                normalizeRequired(record.getAcademicYear(), "Academic record academicYear");
        AcademicYear academicYear =
                academicYearResolver.resolve(studentSchoolId, academicYearName, null);
        if (academicYear == null
                || academicYear.getName() == null
                || academicYear.getName().isBlank()) {
            throw new IllegalArgumentException(
                    "Academic year could not be resolved for this school.");
        }

        record.setSchoolId(studentSchoolId);
        record.setStudentDocsId(normalizedStudentDocsId);
        record.setAcademicYear(academicYear.getName());

        if (request.isProvided("identityNo")) {
            record.setIdentityNo(request.getIdentityNo());
        }
        if (request.isProvided("rollNo")) {
            record.setRollNo(request.getRollNo());
        }
        if (request.isProvided("hostelRoomNo")) {
            record.setHostelRoomNo(request.getHostelRoomNo());
        }

        if (request.isProvided("classDocsId")) {
            String previousClassDocsId = normalizeOptional(record.getClassDocsId());
            String requestedClassDocsId = normalizeOptional(request.getClassDocsId());
            boolean classChanged = !Objects.equals(previousClassDocsId, requestedClassDocsId);
            record.setClassDocsId(requestedClassDocsId);

            if (requestedClassDocsId == null) {
                if (!request.isProvided("sectionNo")) {
                    record.setSectionNo(null);
                }
                if (!request.isProvided("rollNo")) {
                    record.setRollNo(null);
                }
            } else if (classChanged && !request.isProvided("sectionNo")) {
                // Never carry a section from the old class into the new class.
                record.setSectionNo(null);
            }
        }
        if (request.isProvided("sectionNo")) {
            record.setSectionNo(request.getSectionNo());
        }

        if (request.isProvided("status")) {
            if (request.getStatus() == null) {
                throw new IllegalArgumentException("status cannot be null.");
            }
            record.setStatus(request.getStatus());
        }

        normalizeAndValidateAcademicRecord(student, record);
        rejectAcademicIdentifierConflicts(student, record);

        if (record.getStatus() == StudentStatus.ACTIVE) {
            for (StudentAcademicRecord existing :
                    studentAcademicRecordRepository.findByStudentDocsId(normalizedStudentDocsId)) {
                if (!sameAcademicRecord(existing, record)) {
                    deactivateAcademicRecord(existing);
                }
            }
        }

        record.setUpdatedAt(LocalDateTime.now());
        final StudentAcademicRecord saved;
        try {
            saved = studentAcademicRecordRepository.save(record);
        } catch (DuplicateKeyException ex) {
            throw duplicateAcademicRecordConflict(student, record, ex);
        }

        if (saved.getStatus() == StudentStatus.ACTIVE) {
            setCurrentAcademicRecordPointer(student, saved);
        } else if (Objects.equals(
                student.getCurrentAcademicRecordDocsId(), saved.getId())) {
            syncCurrentAcademicRecordPointer(student);
        }
        return saved;
    }

    private void deactivateAcademicRecord(StudentAcademicRecord record) {
        if (record == null || record.getStatus() == StudentStatus.INACTIVE) {
            return;
        }
        record.setStatus(StudentStatus.INACTIVE);
        record.setUpdatedAt(LocalDateTime.now());
        studentAcademicRecordRepository.save(record);
    }

    /**
     * Applies one validation policy to student creation, academic assignment,
     * profile updates, and new placement-history records.
     */
    private void normalizeAndValidateAcademicRecord(Student student, StudentAcademicRecord record) {
        String schoolId = normalizeRequired(student.getSchoolId(), "Student schoolId");
        String academicYear = normalizeRequired(record.getAcademicYear(), "academicYear");
        String classDocsId = normalizeOptional(record.getClassDocsId());
        String sectionNo = normalizeOptional(record.getSectionNo());
        String rollNo = normalizeOptional(record.getRollNo());

        record.setSchoolId(schoolId);
        record.setAcademicYear(academicYear);
        record.setIdentityNo(normalizeOptional(record.getIdentityNo()));
        record.setRollNo(rollNo);
        record.setClassDocsId(classDocsId);
        record.setSectionNo(sectionNo);
        record.setHostelRoomNo(normalizeOptional(record.getHostelRoomNo()));
        if (record.getStatus() == null) {
            record.setStatus(student.getStatus() != null ? student.getStatus() : StudentStatus.ACTIVE);
        }

        if (sectionNo != null && classDocsId == null) {
            throw new IllegalArgumentException("sectionNo requires a classDocsId in the academic record.");
        }
        if (rollNo != null && classDocsId == null) {
            throw new IllegalArgumentException("rollNo requires a classDocsId in the academic record.");
        }
        if (classDocsId == null) {
            return;
        }

        SchoolClass schoolClass = schoolClassRepository.findById(classDocsId)
                .orElseThrow(() -> new ResourceNotFoundException("Class not found with id: " + classDocsId));
        if (schoolClass.getSchoolId() == null || !schoolId.equals(schoolClass.getSchoolId())) {
            throw new IllegalArgumentException("Class does not belong to the same school as the student.");
        }
        if (schoolClass.getAcademicYear() == null
                || !academicYear.equals(schoolClass.getAcademicYear())) {
            throw new IllegalArgumentException(
                    "Class does not belong to academic year '" + academicYear + "'.");
        }

        // A class can be assigned without a section. When a section is supplied,
        // use the class's canonical spelling and reject unknown values.
        if (sectionNo != null) {
            String canonicalSectionNo = schoolClass.getSections() == null ? null
                    : schoolClass.getSections().stream()
                            .filter(Objects::nonNull)
                            .map(String::trim)
                            .filter(section -> section.equalsIgnoreCase(sectionNo))
                            .findFirst()
                            .orElse(null);
            if (canonicalSectionNo == null) {
                throw new IllegalArgumentException(
                        "Section '" + sectionNo + "' does not exist in class '" + classDocsId + "'.");
            }
            record.setSectionNo(canonicalSectionNo);
        }
    }

    private void rejectAcademicIdentifierConflicts(Student student, StudentAcademicRecord record) {
        if (record.getStatus() == StudentStatus.ACTIVE && record.getIdentityNo() != null) {
            studentAcademicRecordRepository
                    .findFirstBySchoolIdAndAcademicYearAndIdentityNoAndStatus(
                            record.getSchoolId(), record.getAcademicYear(),
                            record.getIdentityNo(), StudentStatus.ACTIVE)
                    .filter(existing -> !sameAcademicRecord(existing, record))
                    .filter(existing -> !Objects.equals(existing.getStudentDocsId(), student.getId()))
                    .ifPresent(existing -> {
                        throw new ConflictException(
                                "identityNo '" + record.getIdentityNo()
                                        + "' is already used by another student in school '"
                                        + record.getSchoolId() + "' for academic year '"
                                        + record.getAcademicYear() + "'.");
                    });
        }

        if (record.getRollNo() != null) {
            studentAcademicRecordRepository
                    .findFirstByClassDocsIdAndAcademicYearAndRollNo(
                            record.getClassDocsId(), record.getAcademicYear(), record.getRollNo())
                    .filter(existing -> !sameAcademicRecord(existing, record))
                    .ifPresent(existing -> {
                        throw new ConflictException(
                                "rollNo '" + record.getRollNo() + "' is already used in class '"
                                        + record.getClassDocsId()
                                        + "' in academic year '" + record.getAcademicYear() + "'.");
                    });
        }
    }

    private boolean sameAcademicRecord(StudentAcademicRecord left, StudentAcademicRecord right) {
        return left.getId() != null && right.getId() != null && left.getId().equals(right.getId());
    }

    private String normalizeOptional(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private ConflictException duplicateAcademicRecordConflict(
            Student student, StudentAcademicRecord record, Throwable cause) {
        String detail = record.getIdentityNo() != null
                ? "identityNo '" + record.getIdentityNo() + "'"
                : record.getRollNo() != null ? "rollNo '" + record.getRollNo() + "'" : "class/section/roll assignment";
        return new ConflictException("Academic record conflicts with an existing " + detail
                + " for school '" + student.getSchoolId() + "' and year '" + record.getAcademicYear() + "'.", cause);
    }

    public List<StudentAcademicRecord> getAcademicHistory(String studentDocsId) {
        if (!studentRepository.existsById(studentDocsId)) {
            throw new ResourceNotFoundException("Student not found with id: " + studentDocsId);
        }
        return studentAcademicRecordRepository.findByStudentDocsId(studentDocsId);
    }

    private Comparator<StudentAcademicRecord> academicRecordRecencyComparator() {
        return Comparator
                .comparing(
                        StudentAcademicRecord::getAcademicYear,
                        Comparator.nullsFirst(Comparator.naturalOrder()))
                .thenComparing(record -> record.getStatus() == StudentStatus.ACTIVE ? 1 : 0)
                .thenComparing(
                        StudentAcademicRecord::getUpdatedAt,
                        Comparator.nullsFirst(Comparator.naturalOrder()))
                .thenComparing(
                        StudentAcademicRecord::getCreatedAt,
                        Comparator.nullsFirst(Comparator.naturalOrder()));
    }

    /**
     * Makes a successfully saved placement the student's explicit current record.
     */
    private void setCurrentAcademicRecordPointer(Student student, StudentAcademicRecord record) {
        if (record == null || record.getId() == null) {
            syncCurrentAcademicRecordPointer(student);
            return;
        }
        if (!Objects.equals(student.getCurrentAcademicRecordDocsId(), record.getId())) {
            student.setCurrentAcademicRecordDocsId(record.getId());
            student.setUpdatedAt(LocalDateTime.now());
            studentRepository.save(student);
            log.info("[setCurrentAcademicRecordPointer] Student {} now points to record {}",
                    student.getId(), record.getId());
        }
    }

    /**
     * Repairs the pointer by selecting the newest active placement. When no
     * active academic record exists, the pointer is cleared.
     */
    private void syncCurrentAcademicRecordPointer(Student student) {
        String latestId = studentAcademicRecordRepository.findByStudentDocsId(student.getId()).stream()
                .filter(record -> record.getStatus() == StudentStatus.ACTIVE)
                .max(academicRecordRecencyComparator())
                .map(StudentAcademicRecord::getId)
                .orElse(null);
        if (!Objects.equals(student.getCurrentAcademicRecordDocsId(), latestId)) {
            student.setCurrentAcademicRecordDocsId(latestId);
            student.setUpdatedAt(LocalDateTime.now());
            studentRepository.save(student);
            log.info("[syncCurrentAcademicRecordPointer] Student {} now points to current-year record {}", student.getId(), latestId);
        }
    }

    // =======================================================================================
    // Siblings
    // =======================================================================================

    /** Finds this student's siblings — other students who share at least one guardian with them. */
    public List<StudentResponse> getSiblings(String studentDocsId) {
        Student student = getStudentEntity(studentDocsId);
        if (student.getGuardians() == null || student.getGuardians().isEmpty()) {
            return new ArrayList<>();
        }
        // For each guardian, collect the other students they belong to.
        Map<String, Student> siblings = new java.util.LinkedHashMap<>();
        student.getGuardians().stream()
                .map(GuardianLink::getGuardianDocsId)
                .filter(Objects::nonNull)
                .distinct()
                .forEach(gid -> studentRepository.findByGuardiansGuardianDocsId(gid).forEach(s -> {
                    if (!s.getId().equals(studentDocsId)) siblings.put(s.getId(), s);
                }));
        return buildResponses(new ArrayList<>(siblings.values()));
    }
}
