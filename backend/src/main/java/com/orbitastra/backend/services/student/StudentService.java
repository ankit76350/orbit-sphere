package com.orbitastra.backend.services.student;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.orbitastra.backend.dto.student.AcademicRecordRequest;
import com.orbitastra.backend.dto.student.CreateStudentRequest;
import com.orbitastra.backend.dto.student.StudentGuardianRequest;
import com.orbitastra.backend.dto.student.StudentResponse;
import com.orbitastra.backend.exceptions.ConflictException;
import com.orbitastra.backend.exceptions.ResourceNotFoundException;
import com.orbitastra.backend.models.academics.SchoolClass;
import com.orbitastra.backend.models.core.School;
import com.orbitastra.backend.models.student.Guardian;
import com.orbitastra.backend.models.student.GuardianLink;
import com.orbitastra.backend.models.student.Student;
import com.orbitastra.backend.models.student.StudentAcademicRecord;
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
        if (request.getName() == null || request.getName().isBlank()) {
            throw new IllegalArgumentException("Student name cannot be null or blank.");
        }
        log.info("[createStudent] Request looks good — school={}, name='{}', admissionNo={}",
                request.getSchoolId(), request.getName(), request.getAdmissionNo());

        // Step 2 — turn the guardians in the request into links (and avoid duplicates).
        log.info("[createStudent] Step 2: Preparing the student's guardians");
        List<GuardianService.GuardianDraft> drafts = toGuardianDrafts(request.getGuardians());
        List<GuardianLink> guardianLinks = guardianService.buildDedupedLinks(request.getSchoolId(), null, drafts);

        // Step 3 — build the first-year record if the request included class/year details.
        log.info("[createStudent] Step 3: Building the student's first academic-year record (if any was sent)");
        StudentAcademicRecord initialRecord = assembleInitialRecord(request);
        if (initialRecord != null) {
            log.info("[createStudent] Academic record to create — year={}, class={}, section={}, roll={}",
                    initialRecord.getAcademicYear(), initialRecord.getClassDocId(),
                    initialRecord.getSectionNo(), initialRecord.getRollNo());
        } else {
            log.info("[createStudent] No class/year details sent — the student will be created without a record for now");
        }

        // Step 4 — put together the student object we are going to save.
        log.info("[createStudent] Step 4: Building the student object");
        Student student = Student.builder()
                .schoolId(request.getSchoolId())
                .admissionNo(request.getAdmissionNo())
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
    public Student persistStudent(Student student, StudentAcademicRecord initialRecord) {
        // 5a — the school must exist.
        log.info("[persistStudent] 5a: Looking up the school {}", student.getSchoolId());
        if (student.getSchoolId() == null) {
            throw new IllegalArgumentException("schoolId cannot be null.");
        }
        School school = schoolRepository.findById(student.getSchoolId())
                .orElseThrow(() -> new ResourceNotFoundException("School not found with id: " + student.getSchoolId()));

        // 5b — the school may have a limit on how many students it can have.
        if (school.getMaxStudents() != null) {
            long current = studentRepository.countBySchoolId(student.getSchoolId());
            log.info("[persistStudent] 5b: This school has {} of {} allowed students", current, school.getMaxStudents());
            if (current >= school.getMaxStudents()) {
                throw new IllegalArgumentException("You have exceeded the package limit for maximum students.");
            }
        }

        // 5c — no two students can share the same admission number.
        if (student.getAdmissionNo() != null && !student.getAdmissionNo().isBlank()) {
            log.info("[persistStudent] 5c: Making sure admission number '{}' is not already used", student.getAdmissionNo());
            if (studentRepository.findByAdmissionNo(student.getAdmissionNo()).isPresent()) {
                throw new IllegalArgumentException(
                        "Admission number '" + student.getAdmissionNo() + "' is already taken.");
            }
        }

        // A single admission may produce at most one student. Direct student creation
        // leaves admissionDocsId null, so it does not participate in this check/index.
        if (student.getAdmissionDocsId() != null && !student.getAdmissionDocsId().isBlank()
                && studentRepository.findByAdmissionDocsId(student.getAdmissionDocsId()).isPresent()) {
            throw new ConflictException(
                    "Admission " + student.getAdmissionDocsId() + " has already been converted to a student.");
        }

        // 5d — save the student.
        student.setDocuments(copyStrings(student.getDocuments()));
        student.setMedicalRemark(copyStrings(student.getMedicalRemark()));
        student.setCreatedAt(LocalDateTime.now());
        student.setUpdatedAt(LocalDateTime.now());
        Student saved = studentRepository.save(student);
        log.info("[persistStudent] 5d: Saved the student (id={}) with {} guardian(s)",
                saved.getId(), saved.getGuardians() == null ? 0 : saved.getGuardians().size());

        // 5e — if class/year details were sent, save the academic-year record.
        if (initialRecord != null) {
            log.info("[persistStudent] 5e: Creating the student's academic-year record");

            // Work out which school year this belongs to (from the year sent, or the joining date).
            String acadYear = academicYearResolver
                    .resolve(saved.getSchoolId(), initialRecord.getAcademicYear(), saved.getAdmissionDate())
                    .getName();
            log.info("[persistStudent] Using academic year {}", acadYear);

            // If a class was given, make sure it exists and belongs to this school.
            if (initialRecord.getClassDocId() != null) {
                SchoolClass schoolClass = schoolClassRepository.findById(initialRecord.getClassDocId())
                        .orElseThrow(() -> new ResourceNotFoundException(
                                "Class not found with id: " + initialRecord.getClassDocId()));
                if (!schoolClass.getSchoolId().equals(saved.getSchoolId())) {
                    throw new IllegalArgumentException("Class does not belong to the same school as the student.");
                }
                log.info("[persistStudent] Class {} is valid and belongs to this school", initialRecord.getClassDocId());
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
            log.info("[persistStudent] Saved the academic-year record (id={})", savedRecord.getId());

            // 5f — remember this as the student's current-year record.
            saved.setCurrentAcademicRecordId(savedRecord.getId());
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
                        g.getGuardianId(), g.getName(), g.getPhone(), g.getEmail(),
                        g.getAddress(), g.getOccupation(), g.getRelation(),
                        g.getPrimary(), g.getEmergencyContact(),
                        g.getPickupApproved(), g.getPortalAccess()))
                .toList();
    }

    /**
     * Builds the first-year record from the request. The class/year details can come either as a
     * nested "currentAcademicRecord" object or as top-level fields — this reads whichever is there.
     */
    private StudentAcademicRecord assembleInitialRecord(CreateStudentRequest request) {
        AcademicRecordRequest dto = request.getCurrentAcademicRecord();
        StudentAcademicRecord record = dto != null ? dto.toModel() : null;

        // "classId" is an older name for "classDocId" — accept either.
        String classDocId = request.getClassDocId() != null ? request.getClassDocId() : request.getClassId();
        boolean hasTopLevel = request.getAcademicYear() != null || classDocId != null
                || request.getSectionNo() != null || request.getRollNo() != null;

        if (record == null && hasTopLevel) {
            // No nested object, but top-level fields were sent — build the record from those.
            record = StudentAcademicRecord.builder()
                    .academicYear(request.getAcademicYear())
                    .classDocId(classDocId)
                    .sectionNo(request.getSectionNo())
                    .rollNo(request.getRollNo())
                    .build();
        } else if (record != null) {
            // Nested object was sent — fill in anything it left out from the top-level fields.
            if (record.getAcademicYear() == null) record.setAcademicYear(request.getAcademicYear());
            if (record.getClassDocId() == null) record.setClassDocId(classDocId);
            if (record.getSectionNo() == null) record.setSectionNo(request.getSectionNo());
            if (record.getRollNo() == null) record.setRollNo(request.getRollNo());
        }
        return record;
    }

    // =======================================================================================
    // Building the reply we send back to the app
    // =======================================================================================

    /** Loads a student from the database, or throws if there is no student with that id. */
    private Student getStudentEntity(String id) {
        return studentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found with id: " + id));
    }

    /** Finds the student's record for the newest school year they have, or null if they have none. */
    private StudentAcademicRecord latestRecordOf(String studentDocId) {
        if (studentDocId == null) return null;
        return studentAcademicRecordRepository.findByStudentDocId(studentDocId).stream()
                .max(Comparator.comparing(StudentAcademicRecord::getAcademicYear,
                        Comparator.nullsFirst(Comparator.naturalOrder())))
                .orElse(null);
    }

    /** Builds the reply for one student, including their current-year record. */
    public StudentResponse buildResponse(Student student) {
        if (student == null) return null;
        return StudentResponse.of(student, latestRecordOf(student.getId()));
    }

    /**
     * Builds the reply for a list of students. It fetches everyone's records in a single database
     * call (instead of one call per student) so it stays fast even for long lists.
     */
    private List<StudentResponse> buildResponses(List<Student> students) {
        if (students == null || students.isEmpty()) return new ArrayList<>();
        List<String> ids = students.stream().map(Student::getId).filter(Objects::nonNull).toList();
        // For each student, keep only their newest-year record.
        Map<String, StudentAcademicRecord> latest = studentAcademicRecordRepository.findByStudentDocIdIn(ids).stream()
                .collect(Collectors.toMap(
                        StudentAcademicRecord::getStudentDocId,
                        r -> r,
                        (a, b) -> Comparator.comparing(StudentAcademicRecord::getAcademicYear,
                                Comparator.nullsFirst(Comparator.naturalOrder())).compare(a, b) >= 0 ? a : b));
        return students.stream().map(s -> StudentResponse.of(s, latest.get(s.getId()))).toList();
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
        List<String> ids = records.stream().map(StudentAcademicRecord::getStudentDocId).distinct().toList();
        List<Student> students = studentRepository.findAllById(ids);
        // Show the record for the year that was asked for (not the newest one).
        Map<String, StudentAcademicRecord> byStudent = records.stream()
                .collect(Collectors.toMap(StudentAcademicRecord::getStudentDocId, r -> r, (a, b) -> a));
        return students.stream().map(s -> StudentResponse.of(s, byStudent.get(s.getId()))).toList();
    }

    public List<StudentResponse> getStudentsByClass(String classId) {
        List<String> ids = studentAcademicRecordRepository.findByClassDocId(classId).stream()
                .map(StudentAcademicRecord::getStudentDocId).distinct().toList();
        return buildResponses(studentRepository.findAllById(ids));
    }

    public List<StudentResponse> getStudentsByHostelRoom(String hostelRoomNo) {
        List<String> ids = studentAcademicRecordRepository.findByHostelRoomNo(hostelRoomNo).stream()
                .map(StudentAcademicRecord::getStudentDocId).distinct().toList();
        return buildResponses(studentRepository.findAllById(ids));
    }

    public List<StudentResponse> getStudentsByGuardian(String guardianId) {
        return buildResponses(studentRepository.findByGuardiansGuardianId(guardianId));
    }

    // =======================================================================================
    // Updating a student
    // =======================================================================================

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
        StudentAcademicRecord currentRecord = latestRecordOf(student.getId());
        String targetYear = (detailsRecord != null && detailsRecord.getAcademicYear() != null)
                ? detailsRecord.getAcademicYear()
                : (currentRecord != null ? currentRecord.getAcademicYear() : null);
        if (targetYear == null) {
            targetYear = academicYearResolver.resolve(saved.getSchoolId(), null, saved.getAdmissionDate()).getName();
        }

        // Find that year's record, or start a new one if it does not exist yet.
        final String year = targetYear;
        StudentAcademicRecord record = studentAcademicRecordRepository
                .findByStudentDocIdAndAcademicYear(student.getId(), year)
                .orElseGet(() -> StudentAcademicRecord.builder()
                        .studentDocId(student.getId())
                        .academicYear(year)
                        .createdAt(LocalDateTime.now())
                        .build());
        record.setSchoolId(saved.getSchoolId());
        boolean changed = false;

        // Update only the record fields that were sent.
        if (detailsRecord != null) {
            if (detailsRecord.getStudentNo() != null) { record.setStudentNo(detailsRecord.getStudentNo()); changed = true; }
            if (detailsRecord.getRollNo() != null) { record.setRollNo(detailsRecord.getRollNo()); changed = true; }
            if (detailsRecord.getClassDocId() != null) {
                SchoolClass schoolClass = schoolClassRepository.findById(detailsRecord.getClassDocId())
                        .orElseThrow(() -> new ResourceNotFoundException(
                                "Class not found with id: " + detailsRecord.getClassDocId()));
                if (!schoolClass.getSchoolId().equals(saved.getSchoolId())) {
                    throw new IllegalArgumentException("Class does not belong to the same school as the student.");
                }
                record.setClassDocId(detailsRecord.getClassDocId());
                changed = true;
            }
            if (detailsRecord.getSectionNo() != null) { record.setSectionNo(detailsRecord.getSectionNo()); changed = true; }
            if (detailsRecord.getHostelRoomNo() != null) { record.setHostelRoomNo(detailsRecord.getHostelRoomNo()); changed = true; }
        }
        if (details.getStatus() != null) { record.setStatus(details.getStatus()); changed = true; }

        // Only save the record if something in it actually changed.
        if (changed) {
            record.setUpdatedAt(LocalDateTime.now());
            studentAcademicRecordRepository.save(record);
            syncCurrentAcademicRecordPointer(saved);
        }
        return buildResponse(saved);
    }

    private List<String> copyStrings(List<String> values) {
        return values == null ? new ArrayList<>() : new ArrayList<>(values);
    }

    // =======================================================================================
    // Guardians attached to a student
    // =======================================================================================

    /**
     * Attaches a guardian to a student. If that guardian is already attached, the old link is
     * replaced (handy for changing their role or flags). The guardian must exist and be in the
     * same school as the student.
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
        if (student.getGuardians() == null) student.setGuardians(new ArrayList<>());
        // Remove any existing link to this guardian first, then add the new one.
        student.getGuardians().removeIf(g -> link.getGuardianId().equals(g.getGuardianId()));
        student.getGuardians().add(link);
        student.setUpdatedAt(LocalDateTime.now());
        return buildResponse(studentRepository.save(student));
    }

    public StudentResponse removeGuardianLink(String studentId, String guardianId) {
        Student student = getStudentEntity(studentId);
        // Only save if a link was actually removed.
        if (student.getGuardians() != null
                && student.getGuardians().removeIf(g -> guardianId.equals(g.getGuardianId()))) {
            student.setUpdatedAt(LocalDateTime.now());
            student = studentRepository.save(student);
        }
        return buildResponse(student);
    }

    // =======================================================================================
    // Deleting a student
    // =======================================================================================

    public void deleteStudent(String id) {
        Student student = getStudentEntity(id);
        // Delete the student's year records first, then the student.
        studentAcademicRecordRepository.deleteAll(studentAcademicRecordRepository.findByStudentDocId(id));
        studentRepository.delete(student);
        log.info("[deleteStudent] Deleted student {} and all of their academic-year records", id);
    }

    // =======================================================================================
    // Academic-year records (class / section / roll number for a school year)
    // =======================================================================================

    /**
     * Adds a new academic-year record for a student, or updates it if one already exists for that
     * year. This is how a student is placed in a class/section for a given year.
     */
    public StudentAcademicRecord createOrUpdateAcademicRecord(String studentId, StudentAcademicRecord details) {
        Student student = getStudentEntity(studentId);

        // Find this year's record, or start a fresh one.
        StudentAcademicRecord record = studentAcademicRecordRepository
                .findByStudentDocIdAndAcademicYear(studentId, details.getAcademicYear())
                .orElseGet(() -> StudentAcademicRecord.builder()
                        .studentDocId(studentId)
                        .academicYear(details.getAcademicYear())
                        .createdAt(LocalDateTime.now())
                        .build());

        record.setSchoolId(student.getSchoolId());
        if (details.getStudentNo() != null) record.setStudentNo(details.getStudentNo());
        if (details.getRollNo() != null) record.setRollNo(details.getRollNo());
        if (details.getClassDocId() != null) {
            // Make sure the class exists and is in the same school.
            SchoolClass schoolClass = schoolClassRepository.findById(details.getClassDocId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Class not found with id: " + details.getClassDocId()));
            if (!schoolClass.getSchoolId().equals(student.getSchoolId())) {
                throw new IllegalArgumentException("Class does not belong to the same school as the student.");
            }
            record.setClassDocId(details.getClassDocId());
        }
        if (details.getSectionNo() != null) record.setSectionNo(details.getSectionNo());
        if (details.getHostelRoomNo() != null) record.setHostelRoomNo(details.getHostelRoomNo());
        // Use the status sent, or fall back to the student's status.
        record.setStatus(details.getStatus() != null ? details.getStatus() : student.getStatus());
        record.setUpdatedAt(LocalDateTime.now());

        StudentAcademicRecord saved = studentAcademicRecordRepository.save(record);
        // Keep the "current year" pointer on the student up to date.
        syncCurrentAcademicRecordPointer(student);
        return saved;
    }

    /** Moves a student up to a new school year. It's the same as adding that year's record. */
    public StudentAcademicRecord promoteStudent(String studentId, StudentAcademicRecord promotion) {
        if (promotion.getAcademicYear() == null || promotion.getAcademicYear().isBlank()) {
            throw new IllegalArgumentException("Academic year is required for promotion.");
        }
        return createOrUpdateAcademicRecord(studentId, promotion);
    }

    public List<StudentAcademicRecord> getAcademicHistory(String studentId) {
        if (!studentRepository.existsById(studentId)) {
            throw new ResourceNotFoundException("Student not found with id: " + studentId);
        }
        return studentAcademicRecordRepository.findByStudentDocId(studentId);
    }

    /**
     * Points the student at their newest-year record, and saves that only if it changed. Called
     * after adding or changing a year record so the "current year" always stays correct.
     */
    private void syncCurrentAcademicRecordPointer(Student student) {
        String latestId = studentAcademicRecordRepository.findByStudentDocId(student.getId()).stream()
                .max(Comparator.comparing(StudentAcademicRecord::getAcademicYear,
                        Comparator.nullsFirst(Comparator.naturalOrder())))
                .map(StudentAcademicRecord::getId)
                .orElse(null);
        if (!Objects.equals(student.getCurrentAcademicRecordId(), latestId)) {
            student.setCurrentAcademicRecordId(latestId);
            student.setUpdatedAt(LocalDateTime.now());
            studentRepository.save(student);
            log.info("[syncCurrentAcademicRecordPointer] Student {} now points to current-year record {}", student.getId(), latestId);
        }
    }

    // =======================================================================================
    // Siblings
    // =======================================================================================

    /** Finds this student's siblings — other students who share at least one guardian with them. */
    public List<StudentResponse> getSiblings(String studentId) {
        Student student = getStudentEntity(studentId);
        if (student.getGuardians() == null || student.getGuardians().isEmpty()) {
            return new ArrayList<>();
        }
        // For each guardian, collect the other students they belong to.
        Map<String, Student> siblings = new java.util.LinkedHashMap<>();
        student.getGuardians().stream()
                .map(GuardianLink::getGuardianId)
                .filter(Objects::nonNull)
                .distinct()
                .forEach(gid -> studentRepository.findByGuardiansGuardianId(gid).forEach(s -> {
                    if (!s.getId().equals(studentId)) siblings.put(s.getId(), s);
                }));
        return buildResponses(new ArrayList<>(siblings.values()));
    }
}
