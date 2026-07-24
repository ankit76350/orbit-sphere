package com.orbitastra.backend.services.student;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Optional;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;

import com.orbitastra.backend.exceptions.ConflictException;
import com.orbitastra.backend.exceptions.ResourceNotFoundException;
import com.orbitastra.backend.models.core.AcademicYear;
import com.orbitastra.backend.models.core.School;
import com.orbitastra.backend.models.student.GuardianLink;
import com.orbitastra.backend.models.student.Student;
import com.orbitastra.backend.models.student.StudentAcademicRecord;
import com.orbitastra.backend.models.student.enums.StudentStatus;
import com.orbitastra.backend.repositories.student.GuardianRepository;
import com.orbitastra.backend.repositories.student.StudentRepository;
import com.orbitastra.backend.repositories.core.SchoolRepository;
import com.orbitastra.backend.repositories.student.StudentAcademicRecordRepository;
import com.orbitastra.backend.models.academics.SchoolClass;
import com.orbitastra.backend.repositories.academics.SchoolClassRepository;
import com.orbitastra.backend.dto.student.CreateStudentRequest;
import com.orbitastra.backend.dto.student.AcademicRecordRequest;
import com.orbitastra.backend.dto.student.StudentGuardianRequest;
import com.orbitastra.backend.dto.student.StudentResponse;
import com.orbitastra.backend.services.utils.AcademicYearResolver;
import com.orbitastra.backend.models.student.enums.GuardianRelation;

@ExtendWith(MockitoExtension.class)
public class StudentServiceTest {

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private SchoolRepository schoolRepository;

    @Mock
    private StudentAcademicRecordRepository studentAcademicRecordRepository;

    @Mock
    private SchoolClassRepository schoolClassRepository;

    @Mock
    private AcademicYearResolver academicYearResolver;

    @Mock
    private GuardianRepository guardianRepository;

    @Mock
    private GuardianService guardianService;

    @InjectMocks
    private StudentService studentService;

    private Student student;
    private School school;
    private AcademicYear academicYear;

    @BeforeEach
    void setUp() {
        student = new Student();
        student.setId("student-id-123");
        student.setSchoolId("school-id-123");
        student.setAdmissionNo("ADM-001");
        student.setName("John Doe");
        student.setDob(LocalDate.of(2012, 5, 10));

        school = new School();
        school.setId("school-id-123");
        school.setMaxStudents(100);

        academicYear = AcademicYear.builder()
                .name("2026-2027")
                .build();
    }

    @Test
    void persistStudent_Success() {
        when(schoolRepository.findById("school-id-123")).thenReturn(Optional.of(school));
        when(studentRepository.countBySchoolId("school-id-123")).thenReturn(10L);
        when(studentRepository.findByAdmissionNo("ADM-001")).thenReturn(Optional.empty());
        when(studentRepository.save(student)).thenReturn(student);
        when(academicYearResolver.resolve(anyString(), any(), any())).thenReturn(academicYear);
        when(studentAcademicRecordRepository.save(any(StudentAcademicRecord.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Student created = studentService.persistStudent(student, StudentAcademicRecord.builder().build());

        assertNotNull(created);
        assertEquals("ADM-001", created.getAdmissionNo());
        verify(schoolRepository, times(1)).findById("school-id-123");
        verify(studentRepository, times(1)).findByAdmissionNo("ADM-001");
        // Saved twice: once to obtain the id + create the record, once to persist the
        // currentAcademicRecordDocsId pointer.
        verify(studentRepository, times(2)).save(student);
        verify(studentAcademicRecordRepository, times(1)).save(any(StudentAcademicRecord.class));
    }

    @Test
    void createStudent_withRequest_delegatesGuardianDedupToGuardianService() {
        CreateStudentRequest req = new CreateStudentRequest();
        req.setSchoolId("school-id-123");
        req.setName("Lucas Johnson");
        req.setAdmissionNo("ADM-2026-0003");
        AcademicRecordRequest academicRecord = new AcademicRecordRequest();
        academicRecord.setAcademicYear("2026-2027");
        req.setCurrentAcademicRecord(academicRecord);
        req.setWalletDocsId("wallet-doc-1");
        req.setMedicalRecordDocsId("medical-doc-1");
        req.setDocuments(List.of("birth-certificate.pdf"));
        req.setMedicalRemark(List.of("Penicillin allergy"));

        StudentGuardianRequest gReq = StudentGuardianRequest.builder()
                .name("Priya Sharma")
                .relation(GuardianRelation.MOTHER)
                .phone("+61-400-555-666")
                .email("priya@example.com")
                .address("9 Oak Ave")
                .occupation("Teacher")
                .build();
        req.setGuardians(List.of(gReq, gReq)); // duplicate in request list — dedup is GuardianService's job

        // GuardianService owns the dedup + link building (covered by GuardianServiceTest);
        // here we only assert StudentService forwards the drafts and uses the result.
        GuardianLink link = GuardianLink.builder().guardianDocsId("guardian-priya").primary(true).build();
        when(guardianService.buildDedupedLinks(eq("school-id-123"), isNull(), anyList()))
                .thenReturn(new ArrayList<>(List.of(link)));

        when(schoolRepository.findById("school-id-123")).thenReturn(Optional.of(school));
        when(studentRepository.countBySchoolId("school-id-123")).thenReturn(0L);
        when(studentRepository.findByAdmissionNo("ADM-2026-0003")).thenReturn(Optional.empty());
        when(studentRepository.save(any(Student.class))).thenAnswer(i -> i.getArgument(0));
        when(academicYearResolver.resolve(anyString(), any(), any())).thenReturn(academicYear);
        // A nested currentAcademicRecord is present, so a record is created; give it an id so
        // the currentAcademicRecordDocsId pointer can be set.
        when(studentAcademicRecordRepository.save(any(StudentAcademicRecord.class)))
                .thenAnswer(i -> { StudentAcademicRecord r = i.getArgument(0); r.setId("acad-rec-1"); return r; });

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<GuardianService.GuardianDraft>> draftsCaptor = ArgumentCaptor.forClass(List.class);

        StudentResponse created = studentService.createStudent(req);

        assertNotNull(created);
        assertEquals("Lucas Johnson", created.getName());
        assertEquals("wallet-doc-1", created.getWalletDocsId());
        assertEquals("medical-doc-1", created.getMedicalRecordDocsId());
        assertEquals(List.of("birth-certificate.pdf"), created.getDocuments());
        assertEquals(List.of("Penicillin allergy"), created.getMedicalRemark());
        assertNull(created.getAdmissionDocsId());
        assertEquals("acad-rec-1", created.getCurrentAcademicRecordDocsId());
        assertEquals(1, created.getGuardians().size());
        assertEquals("guardian-priya", created.getGuardians().get(0).getGuardianDocsId());
        // Both request entries are forwarded as drafts; GuardianService collapses them.
        verify(guardianService).buildDedupedLinks(eq("school-id-123"), isNull(), draftsCaptor.capture());
        assertEquals(2, draftsCaptor.getValue().size());
        assertEquals("Priya Sharma", draftsCaptor.getValue().get(0).name());
    }

    @Test
    void createStudent_withoutAdmissionNo_isRejectedBeforeGuardianWrites() {
        CreateStudentRequest request = new CreateStudentRequest();
        request.setSchoolId("school-id-123");
        request.setName("Lucas Johnson");

        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> studentService.createStudent(request));

        assertEquals("admissionNo cannot be null or blank.", error.getMessage());
        verifyNoInteractions(guardianService);
        verify(studentRepository, never()).save(any());
    }

    @Test
    void persistStudent_SchoolNotFound_ThrowsException() {
        when(schoolRepository.findById("school-id-123")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> {
            studentService.persistStudent(student, null);
        });

        verify(schoolRepository, times(1)).findById("school-id-123");
        verify(studentRepository, never()).save(any());
    }

    @Test
    void persistStudent_withoutName_isRejectedBeforeDatabaseAccess() {
        student.setName(" ");

        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> studentService.persistStudent(student, null));

        assertEquals("Student name cannot be null or blank.", error.getMessage());
        verifyNoInteractions(schoolRepository);
        verify(studentRepository, never()).save(any());
    }

    @Test
    void persistStudent_AdmissionNoDuplicate_ThrowsException() {
        when(schoolRepository.findById("school-id-123")).thenReturn(Optional.of(school));
        when(studentRepository.countBySchoolId("school-id-123")).thenReturn(10L);
        when(studentRepository.findByAdmissionNo("ADM-001")).thenReturn(Optional.of(new Student()));

        ConflictException error = assertThrows(
                ConflictException.class,
                () -> studentService.persistStudent(student, null));

        assertEquals("A student already exists with admissionNo: ADM-001", error.getMessage());
        verify(studentRepository, times(1)).findByAdmissionNo("ADM-001");
        verify(studentRepository, never()).save(any());
    }

    @Test
    void persistStudent_concurrentDuplicateAdmissionNo_returnsConflict() {
        when(schoolRepository.findById("school-id-123")).thenReturn(Optional.of(school));
        when(studentRepository.countBySchoolId("school-id-123")).thenReturn(10L);
        when(studentRepository.findByAdmissionNo("ADM-001")).thenReturn(Optional.empty());
        when(studentRepository.save(student))
                .thenThrow(new DuplicateKeyException("E11000 index: admissionNo_1 dup key"));

        ConflictException error = assertThrows(
                ConflictException.class,
                () -> studentService.persistStudent(student, null));

        assertEquals("A student already exists with admissionNo: ADM-001", error.getMessage());
    }

    @Test
    void persistStudent_concurrentDuplicateAdmissionReference_returnsConflict() {
        student.setAdmissionDocsId("admission-789");
        when(schoolRepository.findById("school-id-123")).thenReturn(Optional.of(school));
        when(studentRepository.countBySchoolId("school-id-123")).thenReturn(10L);
        when(studentRepository.findByAdmissionNo("ADM-001")).thenReturn(Optional.empty());
        when(studentRepository.findByAdmissionDocsId("admission-789")).thenReturn(Optional.empty());
        when(studentRepository.save(student))
                .thenThrow(new DuplicateKeyException("E11000 index: admissionDocsId_1 dup key"));

        ConflictException error = assertThrows(
                ConflictException.class,
                () -> studentService.persistStudent(student, null));

        assertEquals(
                "Admission admission-789 has already been converted to a student.",
                error.getMessage());
    }

    @Test
    void persistStudent_AdmissionAlreadyConverted_ThrowsConflict() {
        student.setAdmissionDocsId("admission-789");
        when(schoolRepository.findById("school-id-123")).thenReturn(Optional.of(school));
        when(studentRepository.countBySchoolId("school-id-123")).thenReturn(10L);
        when(studentRepository.findByAdmissionNo("ADM-001")).thenReturn(Optional.empty());
        when(studentRepository.findByAdmissionDocsId("admission-789"))
                .thenReturn(Optional.of(new Student()));

        assertThrows(ConflictException.class, () -> studentService.persistStudent(student, null));

        verify(studentRepository).findByAdmissionDocsId("admission-789");
        verify(studentRepository, never()).save(any());
    }

    @Test
    void persistStudent_LimitExceeded_ThrowsException() {
        school.setMaxStudents(5);
        when(schoolRepository.findById("school-id-123")).thenReturn(Optional.of(school));
        when(studentRepository.countBySchoolId("school-id-123")).thenReturn(5L);

        assertThrows(IllegalArgumentException.class, () -> {
            studentService.persistStudent(student, null);
        });

        verify(studentRepository, never()).save(any());
    }

    @Test
    void getStudentById_Success() {
        student.setAdmissionDocsId("admission-789");
        StudentAcademicRecord record = StudentAcademicRecord.builder()
                .studentDocId("student-id-123")
                .academicYear("2026-2027")
                .studentNo("STD-001")
                .rollNo("12")
                .build();

        when(studentRepository.findById("student-id-123")).thenReturn(Optional.of(student));
        when(studentAcademicRecordRepository.findByStudentDocId("student-id-123")).thenReturn(List.of(record));

        StudentResponse found = studentService.getStudentById("student-id-123");

        assertNotNull(found);
        assertEquals("student-id-123", found.getId());
        assertEquals("admission-789", found.getAdmissionDocsId());
        assertNotNull(found.getCurrentAcademicRecord());
        assertEquals("STD-001", found.getCurrentAcademicRecord().getStudentNo());
        assertEquals("12", found.getCurrentAcademicRecord().getRollNo());
        assertEquals("2026-2027", found.getCurrentAcademicRecord().getAcademicYear());
    }

    @Test
    void getStudentById_NotFound_ThrowsException() {
        when(studentRepository.findById("student-id-123")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> {
            studentService.getStudentById("student-id-123");
        });
    }

    @Test
    void updateStudent_Success() {
        Student details = new Student();
        details.setName("Jane Doe");

        when(studentRepository.findById("student-id-123")).thenReturn(Optional.of(student));
        when(studentAcademicRecordRepository.findByStudentDocId("student-id-123")).thenReturn(new ArrayList<>());
        when(studentAcademicRecordRepository.findByStudentDocIdAndAcademicYear(anyString(), anyString())).thenReturn(Optional.empty());
        when(studentRepository.save(any(Student.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(academicYearResolver.resolve(anyString(), any(), any())).thenReturn(academicYear);

        StudentResponse updated = studentService.updateStudent("student-id-123", details, null);

        assertNotNull(updated);
        assertEquals("Jane Doe", updated.getName());
        assertEquals("ADM-001", updated.getAdmissionNo()); // unchanged
    }

    @Test
    void deleteStudent_Success() {
        when(studentRepository.findById("student-id-123")).thenReturn(Optional.of(student));
        when(studentAcademicRecordRepository.findByStudentDocId("student-id-123")).thenReturn(new ArrayList<>());

        studentService.deleteStudent("student-id-123");

        verify(studentRepository, times(1)).delete(student);
        verify(studentAcademicRecordRepository, times(1)).deleteAll(anyList());
    }

    @Test
    void createOrUpdateAcademicRecord_Success() {
        StudentAcademicRecord input = StudentAcademicRecord.builder()
                .academicYear("2026-2027")
                .classDocId("class-new")
                .build();

        when(studentRepository.findById("student-id-123")).thenReturn(Optional.of(student));
        when(schoolClassRepository.findById("class-new")).thenReturn(Optional.of(
                SchoolClass.builder().id("class-new").schoolId("school-id-123")
                        .academicYear("2026-2027").build()
        ));
        when(academicYearResolver.resolve("school-id-123", "2026-2027", null)).thenReturn(academicYear);
        when(studentAcademicRecordRepository.findByStudentDocId("student-id-123")).thenReturn(new ArrayList<>());
        when(studentAcademicRecordRepository.findByStudentDocIdAndAcademicYear("student-id-123", "2026-2027"))
                .thenReturn(Optional.empty());
        when(studentAcademicRecordRepository.save(any(StudentAcademicRecord.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        StudentAcademicRecord record = studentService.createOrUpdateAcademicRecord("student-id-123", input);

        assertNotNull(record);
        assertEquals("class-new", record.getClassDocId());
        assertEquals("2026-2027", record.getAcademicYear());
        verify(studentAcademicRecordRepository, times(1)).save(any(StudentAcademicRecord.class));
    }

    @Test
    void createOrUpdateAcademicRecord_updatesStudentCurrentAcademicRecordPointer() {
        StudentAcademicRecord input = StudentAcademicRecord.builder()
                .academicYear("2026-2027")
                .classDocId("class-new")
                .build();

        when(studentRepository.findById("student-id-123")).thenReturn(Optional.of(student));
        when(schoolClassRepository.findById("class-new")).thenReturn(Optional.of(
                SchoolClass.builder().id("class-new").schoolId("school-id-123")
                        .academicYear("2026-2027").build()
        ));
        when(academicYearResolver.resolve("school-id-123", "2026-2027", null)).thenReturn(academicYear);
        when(studentAcademicRecordRepository.findByStudentDocIdAndAcademicYear("student-id-123", "2026-2027"))
                .thenReturn(Optional.empty());
        when(studentAcademicRecordRepository.save(any(StudentAcademicRecord.class)))
                .thenAnswer(i -> { StudentAcademicRecord r = i.getArgument(0); r.setId("rec-2026"); return r; });
        // After the save the record exists and becomes the current one, so the pointer is persisted.
        when(studentAcademicRecordRepository.findByStudentDocId("student-id-123"))
                .thenReturn(List.of(StudentAcademicRecord.builder()
                        .id("rec-2026").academicYear("2026-2027").build()));
        when(studentRepository.save(any(Student.class))).thenAnswer(i -> i.getArgument(0));

        studentService.createOrUpdateAcademicRecord("student-id-123", input);

        assertEquals("rec-2026", student.getCurrentAcademicRecordDocsId());
        verify(studentRepository, times(1)).save(student); // pointer persisted once
    }

    @Test
    void getAcademicHistory_Success() {
        StudentAcademicRecord r1 = StudentAcademicRecord.builder().academicYear("2025-2026").build();
        StudentAcademicRecord r2 = StudentAcademicRecord.builder().academicYear("2026-2027").build();

        when(studentRepository.existsById("student-id-123")).thenReturn(true);
        when(studentAcademicRecordRepository.findByStudentDocId("student-id-123")).thenReturn(List.of(r1, r2));

        List<StudentAcademicRecord> history = studentService.getAcademicHistory("student-id-123");

        assertEquals(2, history.size());
        verify(studentAcademicRecordRepository, times(1)).findByStudentDocId("student-id-123");
    }

    @Test
    void promoteStudent_Success() {
        StudentAcademicRecord input = StudentAcademicRecord.builder()
                .academicYear("2027-2028")
                .classDocId("class-new")
                .build();

        when(studentRepository.findById("student-id-123")).thenReturn(Optional.of(student));
        when(schoolClassRepository.findById("class-new")).thenReturn(Optional.of(
                SchoolClass.builder().id("class-new").schoolId("school-id-123")
                        .academicYear("2027-2028").build()
        ));
        when(academicYearResolver.resolve("school-id-123", "2027-2028", null))
                .thenReturn(AcademicYear.builder().name("2027-2028").build());
        when(studentAcademicRecordRepository.findByStudentDocId("student-id-123")).thenReturn(new ArrayList<>());
        when(studentAcademicRecordRepository.findByStudentDocIdAndAcademicYear("student-id-123", "2027-2028"))
                .thenReturn(Optional.empty());
        when(studentAcademicRecordRepository.save(any(StudentAcademicRecord.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        StudentAcademicRecord record = studentService.promoteStudent("student-id-123", input);

        assertNotNull(record);
        assertEquals("class-new", record.getClassDocId());
        assertEquals("2027-2028", record.getAcademicYear());
        verify(studentAcademicRecordRepository, times(1)).save(any(StudentAcademicRecord.class));
    }

    @Test
    void createOrUpdateAcademicRecord_rejectsNullDetails() {
        assertThrows(IllegalArgumentException.class,
                () -> studentService.createOrUpdateAcademicRecord("student-id-123", null));
        verifyNoInteractions(studentRepository, studentAcademicRecordRepository, academicYearResolver,
                schoolClassRepository);
    }

    @Test
    void createOrUpdateAcademicRecord_rejectsBlankStudentIdOrAcademicYear() {
        StudentAcademicRecord input = StudentAcademicRecord.builder().academicYear("2026-2027").build();
        assertThrows(IllegalArgumentException.class,
                () -> studentService.createOrUpdateAcademicRecord(" ", input));
        assertThrows(IllegalArgumentException.class,
                () -> studentService.createOrUpdateAcademicRecord("student-id-123",
                        StudentAcademicRecord.builder().academicYear(" ").build()));
        verifyNoInteractions(studentRepository, studentAcademicRecordRepository, academicYearResolver,
                schoolClassRepository);
    }

    @Test
    void createOrUpdateAcademicRecord_rejectsAcademicYearNotOwnedBySchool() {
        StudentAcademicRecord input = StudentAcademicRecord.builder().academicYear("2028-2029").build();
        when(studentRepository.findById("student-id-123")).thenReturn(Optional.of(student));
        when(academicYearResolver.resolve("school-id-123", "2028-2029", null))
                .thenThrow(new ResourceNotFoundException("Academic year not found for this school."));

        assertThrows(ResourceNotFoundException.class,
                () -> studentService.createOrUpdateAcademicRecord("student-id-123", input));
        verify(studentAcademicRecordRepository, never()).save(any(StudentAcademicRecord.class));
    }

    @Test
    void createOrUpdateAcademicRecord_rejectsClassFromAnotherAcademicYear() {
        StudentAcademicRecord input = StudentAcademicRecord.builder()
                .academicYear("2026-2027").classDocId("class-old").build();
        when(studentRepository.findById("student-id-123")).thenReturn(Optional.of(student));
        when(academicYearResolver.resolve("school-id-123", "2026-2027", null)).thenReturn(academicYear);
        when(studentAcademicRecordRepository.findByStudentDocIdAndAcademicYear("student-id-123", "2026-2027"))
                .thenReturn(Optional.empty());
        when(schoolClassRepository.findById("class-old")).thenReturn(Optional.of(
                SchoolClass.builder().id("class-old").schoolId("school-id-123")
                        .academicYear("2025-2026").build()));

        assertThrows(IllegalArgumentException.class,
                () -> studentService.createOrUpdateAcademicRecord("student-id-123", input));
        verify(studentAcademicRecordRepository, never()).save(any(StudentAcademicRecord.class));
    }

    @Test
    void createOrUpdateAcademicRecord_rejectsUnknownSectionAndSectionWithoutClass() {
        StudentAcademicRecord unknownSection = StudentAcademicRecord.builder()
                .academicYear("2026-2027").classDocId("class-new").sectionNo("C").build();
        when(studentRepository.findById("student-id-123")).thenReturn(Optional.of(student));
        when(academicYearResolver.resolve("school-id-123", "2026-2027", null)).thenReturn(academicYear);
        when(studentAcademicRecordRepository.findByStudentDocIdAndAcademicYear("student-id-123", "2026-2027"))
                .thenReturn(Optional.empty());
        when(schoolClassRepository.findById("class-new")).thenReturn(Optional.of(
                SchoolClass.builder().id("class-new").schoolId("school-id-123")
                        .academicYear("2026-2027").sections(List.of("A", "B")).build()));

        assertThrows(IllegalArgumentException.class,
                () -> studentService.createOrUpdateAcademicRecord("student-id-123", unknownSection));

        StudentAcademicRecord sectionWithoutClass = StudentAcademicRecord.builder()
                .academicYear("2026-2027").sectionNo("A").build();
        assertThrows(IllegalArgumentException.class,
                () -> studentService.createOrUpdateAcademicRecord("student-id-123", sectionWithoutClass));
        verify(studentAcademicRecordRepository, never()).save(any(StudentAcademicRecord.class));
    }

    @Test
    void createOrUpdateAcademicRecord_normalizesSectionAndBlankOptionalValues() {
        StudentAcademicRecord input = StudentAcademicRecord.builder()
                .academicYear("2026-2027").classDocId("class-new").sectionNo("a")
                .studentNo(" ").rollNo(" ").hostelRoomNo(" ").build();
        when(studentRepository.findById("student-id-123")).thenReturn(Optional.of(student));
        when(academicYearResolver.resolve("school-id-123", "2026-2027", null)).thenReturn(academicYear);
        when(studentAcademicRecordRepository.findByStudentDocIdAndAcademicYear("student-id-123", "2026-2027"))
                .thenReturn(Optional.empty());
        when(schoolClassRepository.findById("class-new")).thenReturn(Optional.of(
                SchoolClass.builder().id("class-new").schoolId("school-id-123")
                        .academicYear("2026-2027").sections(List.of("A", "B")).build()));
        when(studentAcademicRecordRepository.save(any(StudentAcademicRecord.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        StudentAcademicRecord saved = studentService.createOrUpdateAcademicRecord("student-id-123", input);

        assertEquals("A", saved.getSectionNo());
        assertNull(saved.getStudentNo());
        assertNull(saved.getRollNo());
        assertNull(saved.getHostelRoomNo());
    }

    @Test
    void createOrUpdateAcademicRecord_preservesExistingStatusWhenOmitted() {
        StudentAcademicRecord existing = StudentAcademicRecord.builder()
                .id("record-2026").studentDocId("student-id-123").academicYear("2026-2027")
                .status(StudentStatus.INACTIVE).build();
        StudentAcademicRecord input = StudentAcademicRecord.builder()
                .academicYear("2026-2027").studentNo("STD-UPDATED").build();
        when(studentRepository.findById("student-id-123")).thenReturn(Optional.of(student));
        when(academicYearResolver.resolve("school-id-123", "2026-2027", null)).thenReturn(academicYear);
        when(studentAcademicRecordRepository.findByStudentDocIdAndAcademicYear("student-id-123", "2026-2027"))
                .thenReturn(Optional.of(existing));
        when(studentAcademicRecordRepository.findByStudentDocId("student-id-123"))
                .thenReturn(List.of(existing));
        when(studentAcademicRecordRepository.save(any(StudentAcademicRecord.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        StudentAcademicRecord saved = studentService.createOrUpdateAcademicRecord("student-id-123", input);

        assertEquals(StudentStatus.INACTIVE, saved.getStatus());
        assertEquals("STD-UPDATED", saved.getStudentNo());
    }

    @Test
    void createOrUpdateAcademicRecord_translatesDuplicateIndexToConflict() {
        StudentAcademicRecord input = StudentAcademicRecord.builder()
                .academicYear("2026-2027").studentNo("STD-001").build();
        when(studentRepository.findById("student-id-123")).thenReturn(Optional.of(student));
        when(academicYearResolver.resolve("school-id-123", "2026-2027", null)).thenReturn(academicYear);
        when(studentAcademicRecordRepository.findByStudentDocIdAndAcademicYear("student-id-123", "2026-2027"))
                .thenReturn(Optional.empty());
        when(studentAcademicRecordRepository.save(any(StudentAcademicRecord.class)))
                .thenThrow(new DuplicateKeyException("school_year_student_no_unique_idx"));

        ConflictException error = assertThrows(ConflictException.class,
                () -> studentService.createOrUpdateAcademicRecord("student-id-123", input));
        assertTrue(error.getMessage().contains("studentNo"));
    }

    @Test
    void promoteStudent_rejectsMissingAcademicYear() {
        assertThrows(IllegalArgumentException.class,
                () -> studentService.promoteStudent("student-id-123", null));
        assertThrows(IllegalArgumentException.class,
                () -> studentService.promoteStudent("student-id-123",
                        StudentAcademicRecord.builder().academicYear(" ").build()));
    }

    @Test
    void getSiblings_Success() {
        // Siblings now share a guardian, not a parent.
        student.setGuardians(new ArrayList<>(List.of(
                GuardianLink.builder().guardianDocsId("guardian-1").build())));

        Student sibling = new Student();
        sibling.setId("sibling-id-999");
        sibling.setSchoolId("school-id-123");
        sibling.setAdmissionNo("ADM-999");

        when(studentRepository.findById("student-id-123")).thenReturn(Optional.of(student));
        when(studentRepository.findByGuardiansGuardianDocsId("guardian-1")).thenReturn(List.of(student, sibling));
        when(studentAcademicRecordRepository.findByStudentDocIdIn(anyList())).thenReturn(new ArrayList<>());

        List<StudentResponse> siblings = studentService.getSiblings("student-id-123");

        assertEquals(1, siblings.size());
        assertEquals("sibling-id-999", siblings.get(0).getId());
        verify(studentRepository, times(1)).findByGuardiansGuardianDocsId("guardian-1");
    }
}
