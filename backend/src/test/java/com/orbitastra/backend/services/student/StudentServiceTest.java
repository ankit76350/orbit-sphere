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

import com.orbitastra.backend.models.old.academics.SchoolClass;
import com.orbitastra.backend.models.old.core.AcademicYear;
import com.orbitastra.backend.models.old.core.School;
import com.orbitastra.backend.models.old.student.Student;
import com.orbitastra.backend.models.old.student.StudentAcademicRecord;
import com.orbitastra.backend.models.old.student.embedded.GuardianLink;
import com.orbitastra.backend.models.old.student.enums.GuardianRelation;
import com.orbitastra.backend.models.old.student.enums.StudentStatus;
import com.orbitastra.backend.old.dto.student.AcademicRecordRequest;
import com.orbitastra.backend.old.dto.student.CreateStudentRequest;
import com.orbitastra.backend.old.dto.student.StudentGuardianRequest;
import com.orbitastra.backend.old.dto.student.StudentResponse;
import com.orbitastra.backend.old.dto.student.UpdateAcademicRecordRequest;
import com.orbitastra.backend.old.exceptions.ConflictException;
import com.orbitastra.backend.old.exceptions.ResourceNotFoundException;
import com.orbitastra.backend.old.repositories.academics.SchoolClassRepository;
import com.orbitastra.backend.old.repositories.core.SchoolRepository;
import com.orbitastra.backend.old.repositories.student.GuardianRepository;
import com.orbitastra.backend.old.repositories.student.StudentAcademicRecordRepository;
import com.orbitastra.backend.old.repositories.student.StudentRepository;
import com.orbitastra.backend.old.services.student.GuardianService;
import com.orbitastra.backend.old.services.student.StudentService;
import com.orbitastra.backend.old.services.utils.AcademicYearResolver;

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

                Student created = studentService.persistStudent(
                                student,
                                StudentAcademicRecord.builder().academicYear("2026-2027").build());

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

                // GuardianService owns the dedup + link building (covered by
                // GuardianServiceTest);
                // here we only assert StudentService forwards the drafts and uses the result.
                GuardianLink link = GuardianLink.builder().guardianDocsId("guardian-priya").primary(true).build();
                when(guardianService.buildDedupedLinks(eq("school-id-123"), isNull(), anyList()))
                                .thenReturn(new ArrayList<>(List.of(link)));

                when(schoolRepository.findById("school-id-123")).thenReturn(Optional.of(school));
                when(studentRepository.countBySchoolId("school-id-123")).thenReturn(0L);
                when(studentRepository.findByAdmissionNo("ADM-2026-0003")).thenReturn(Optional.empty());
                when(studentRepository.save(any(Student.class))).thenAnswer(i -> i.getArgument(0));
                when(academicYearResolver.resolve(anyString(), any(), any())).thenReturn(academicYear);
                // A nested currentAcademicRecord is present, so a record is created; give it an
                // id so
                // the currentAcademicRecordDocsId pointer can be set.
                when(studentAcademicRecordRepository.save(any(StudentAcademicRecord.class)))
                                .thenAnswer(i -> {
                                        StudentAcademicRecord r = i.getArgument(0);
                                        r.setId("acad-rec-1");
                                        return r;
                                });

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
        void createStudent_withEmptyAcademicRecord_doesNotCreateRecordOrPointer() {
                CreateStudentRequest request = new CreateStudentRequest();
                request.setSchoolId("school-id-123");
                request.setAdmissionNo("ADM-EMPTY-ACADEMIC");
                request.setName("Student Without Placement");
                request.setCurrentAcademicRecord(new AcademicRecordRequest());

                when(schoolRepository.findById("school-id-123")).thenReturn(Optional.of(school));
                when(studentRepository.countBySchoolId("school-id-123")).thenReturn(0L);
                when(studentRepository.findByAdmissionNo("ADM-EMPTY-ACADEMIC")).thenReturn(Optional.empty());
                when(studentRepository.save(any(Student.class))).thenAnswer(invocation -> {
                        Student value = invocation.getArgument(0);
                        value.setId("created-student");
                        return value;
                });

                StudentResponse response = studentService.createStudent(request);

                assertNull(response.getCurrentAcademicRecordDocsId());
                assertNull(response.getCurrentAcademicRecord());
                verify(studentAcademicRecordRepository, never()).save(any(StudentAcademicRecord.class));
                verifyNoInteractions(academicYearResolver);
                verify(studentRepository, times(1)).save(any(Student.class));
        }

        @Test
        void academicRecordRequest_withOnlyBlankValues_isTreatedAsOmitted() {
                AcademicRecordRequest request = new AcademicRecordRequest();
                request.setAcademicYear(" ");
                request.setIdentityNo("\t");
                request.setRollNo("");
                request.setClassDocsId("  ");
                request.setSectionNo("\n");
                request.setHostelRoomNo(" ");

                assertFalse(request.hasAnyValue());
                assertNull(request.toModel());
        }

        @Test
        void createStudent_withAcademicDetailsButNoAcademicYear_isRejectedBeforeWrites() {
                CreateStudentRequest request = new CreateStudentRequest();
                request.setSchoolId("school-id-123");
                request.setAdmissionNo("ADM-MISSING-YEAR");
                request.setName("Student With Missing Academic Year");
                AcademicRecordRequest academicRecord = new AcademicRecordRequest();
                academicRecord.setIdentityNo("TC-STD-1784892426");
                request.setCurrentAcademicRecord(academicRecord);

                IllegalArgumentException error = assertThrows(
                                IllegalArgumentException.class,
                                () -> studentService.createStudent(request));

                assertEquals(
                                "currentAcademicRecord.academicYear is required when currentAcademicRecord contains academic details.",
                                error.getMessage());
                verifyNoInteractions(guardianService, schoolRepository, studentRepository,
                                studentAcademicRecordRepository, academicYearResolver);
        }

        @Test
        void persistStudent_withAcademicDetailsButNoAcademicYear_isRejectedWithoutWrites() {
                StudentAcademicRecord academicRecord = StudentAcademicRecord.builder()
                                .identityNo("TC-STD-1784892426")
                                .build();

                IllegalArgumentException error = assertThrows(
                                IllegalArgumentException.class,
                                () -> studentService.persistStudent(student, academicRecord));

                assertEquals(
                                "currentAcademicRecord.academicYear is required when currentAcademicRecord contains academic details.",
                                error.getMessage());
                verifyNoInteractions(schoolRepository, studentRepository,
                                studentAcademicRecordRepository, academicYearResolver);
        }

        @Test
        void createStudent_rejectsSectionThatDoesNotBelongToClassBeforeStudentWrite() {
                CreateStudentRequest request = validDirectStudentRequest();
                request.getCurrentAcademicRecord().setClassDocsId("class-9");
                request.getCurrentAcademicRecord().setSectionNo("C");

                stubCreationChecks();
                when(schoolClassRepository.findById("class-9")).thenReturn(Optional.of(
                                SchoolClass.builder()
                                                .id("class-9")
                                                .schoolId("school-id-123")
                                                .academicYear("2026-2027")
                                                .sections(List.of("A", "B"))
                                                .build()));

                IllegalArgumentException error = assertThrows(
                                IllegalArgumentException.class,
                                () -> studentService.createStudent(request));

                assertEquals("Section 'C' does not exist in class 'class-9'.", error.getMessage());
                verify(studentRepository, never()).save(any());
                verify(studentAcademicRecordRepository, never()).save(any());
        }

        @Test
        void createStudent_allowsClassWithoutSection() {
                CreateStudentRequest request = validDirectStudentRequest();
                request.getCurrentAcademicRecord().setClassDocsId("class-9");

                stubCreationChecks();
                when(schoolClassRepository.findById("class-9")).thenReturn(Optional.of(
                                SchoolClass.builder()
                                                .id("class-9")
                                                .schoolId("school-id-123")
                                                .academicYear("2026-2027")
                                                .sections(List.of("A", "B"))
                                                .build()));
                when(studentRepository.save(any(Student.class))).thenAnswer(invocation -> {
                        Student value = invocation.getArgument(0);
                        if (value.getId() == null)
                                value.setId("created-student");
                        return value;
                });
                when(studentAcademicRecordRepository.save(any(StudentAcademicRecord.class)))
                                .thenAnswer(invocation -> {
                                        StudentAcademicRecord value = invocation.getArgument(0);
                                        value.setId("created-record");
                                        return value;
                                });

                StudentResponse response = studentService.createStudent(request);

                ArgumentCaptor<StudentAcademicRecord> recordCaptor = ArgumentCaptor
                                .forClass(StudentAcademicRecord.class);
                verify(studentAcademicRecordRepository).save(recordCaptor.capture());
                assertEquals("class-9", recordCaptor.getValue().getClassDocsId());
                assertNull(recordCaptor.getValue().getSectionNo());
                assertEquals("created-record", response.getCurrentAcademicRecordDocsId());
        }

        @Test
        void createStudent_rejectsSectionOrRollNumberWithoutClass() {
                CreateStudentRequest sectionRequest = validDirectStudentRequest();
                sectionRequest.getCurrentAcademicRecord().setSectionNo("A");
                stubCreationChecks();

                IllegalArgumentException sectionError = assertThrows(
                                IllegalArgumentException.class,
                                () -> studentService.createStudent(sectionRequest));
                assertEquals("sectionNo requires a classDocsId in the academic record.",
                                sectionError.getMessage());

                CreateStudentRequest rollRequest = validDirectStudentRequest();
                rollRequest.setAdmissionNo("ADM-002");
                rollRequest.getCurrentAcademicRecord().setRollNo("9C-03");
                when(studentRepository.findByAdmissionNo("ADM-002")).thenReturn(Optional.empty());

                IllegalArgumentException rollError = assertThrows(
                                IllegalArgumentException.class,
                                () -> studentService.createStudent(rollRequest));
                assertEquals("rollNo requires both a classDocsId and a sectionNo in the academic record.",
                                rollError.getMessage());

                CreateStudentRequest rollWithClassNoSectionRequest = validDirectStudentRequest();
                rollWithClassNoSectionRequest.setAdmissionNo("ADM-003");
                rollWithClassNoSectionRequest.getCurrentAcademicRecord().setClassDocsId("class-9");
                rollWithClassNoSectionRequest.getCurrentAcademicRecord().setRollNo("9C-03");
                when(studentRepository.findByAdmissionNo("ADM-003")).thenReturn(Optional.empty());

                IllegalArgumentException rollWithClassNoSectionError = assertThrows(
                                IllegalArgumentException.class,
                                () -> studentService.createStudent(rollWithClassNoSectionRequest));
                assertEquals("rollNo requires both a classDocsId and a sectionNo in the academic record.",
                                rollWithClassNoSectionError.getMessage());
                verify(studentRepository, never()).save(any());
        }

        @Test
        void createStudent_rejectsClassFromAnotherAcademicYear() {
                CreateStudentRequest request = validDirectStudentRequest();
                request.getCurrentAcademicRecord().setClassDocsId("class-old");

                stubCreationChecks();
                when(schoolClassRepository.findById("class-old")).thenReturn(Optional.of(
                                SchoolClass.builder()
                                                .id("class-old")
                                                .schoolId("school-id-123")
                                                .academicYear("2025-2026")
                                                .build()));

                IllegalArgumentException error = assertThrows(
                                IllegalArgumentException.class,
                                () -> studentService.createStudent(request));

                assertEquals("Class does not belong to academic year '2026-2027'.", error.getMessage());
                verify(studentRepository, never()).save(any());
        }

        @Test
        void createStudent_rejectsDuplicateIdentityNumberBeforeStudentWrite() {
                CreateStudentRequest request = validDirectStudentRequest();
                request.getCurrentAcademicRecord().setIdentityNo("IDN/2026/07/2410");
                stubCreationChecks();
                when(studentAcademicRecordRepository
                                .findFirstBySchoolIdAndAcademicYearAndIdentityNoAndStatus(
                                                "school-id-123", "2026-2027", "IDN/2026/07/2410", StudentStatus.ACTIVE))
                                .thenReturn(Optional.of(StudentAcademicRecord.builder()
                                                .id("existing-record")
                                                .studentDocsId("another-student")
                                                .build()));

                ConflictException error = assertThrows(
                                ConflictException.class,
                                () -> studentService.createStudent(request));

                assertTrue(error.getMessage().contains(
                                "identityNo 'IDN/2026/07/2410' is already used"));
                verify(studentRepository, never()).save(any());
        }

        @Test
        void createStudent_rejectsDuplicateRollNumberWithinSameSection() {
                CreateStudentRequest request = validDirectStudentRequest();
                request.getCurrentAcademicRecord().setClassDocsId("class-9");
                request.getCurrentAcademicRecord().setSectionNo("c");
                request.getCurrentAcademicRecord().setRollNo("9C-03");
                stubCreationChecks();
                when(schoolClassRepository.findById("class-9")).thenReturn(Optional.of(
                                SchoolClass.builder()
                                                .id("class-9")
                                                .schoolId("school-id-123")
                                                .academicYear("2026-2027")
                                                .sections(List.of("A", "B", "C"))
                                                .build()));
                when(studentAcademicRecordRepository
                                .findFirstByClassDocsIdAndSectionNoAndAcademicYearAndRollNoAndStatus(
                                                "class-9", "C", "2026-2027", "9C-03",
                                                StudentStatus.ACTIVE))
                                .thenReturn(Optional.of(StudentAcademicRecord.builder()
                                                .id("existing-record")
                                                .studentDocsId("another-student")
                                                .sectionNo("C")
                                                .build()));

                ConflictException error = assertThrows(
                                ConflictException.class,
                                () -> studentService.createStudent(request));

                assertTrue(error.getMessage().contains("rollNo '9C-03' is already used"));
                verify(studentRepository, never()).save(any());
        }

        @Test
        void createStudent_allowsDuplicateRollNumberInDifferentSections() {
                CreateStudentRequest request = validDirectStudentRequest();
                request.getCurrentAcademicRecord().setClassDocsId("class-9");
                request.getCurrentAcademicRecord().setSectionNo("c");
                request.getCurrentAcademicRecord().setRollNo("9C-03");
                stubCreationChecks();
                when(schoolClassRepository.findById("class-9")).thenReturn(Optional.of(
                                SchoolClass.builder()
                                                .id("class-9")
                                                .schoolId("school-id-123")
                                                .academicYear("2026-2027")
                                                .sections(List.of("A", "B", "C"))
                                                .build()));
                when(studentAcademicRecordRepository
                                .findFirstByClassDocsIdAndSectionNoAndAcademicYearAndRollNoAndStatus(
                                                "class-9", "C", "2026-2027", "9C-03",
                                                StudentStatus.ACTIVE))
                                .thenReturn(Optional.empty());
                when(studentRepository.save(any(Student.class))).thenAnswer(invocation -> {
                        Student value = invocation.getArgument(0);
                        if (value.getId() == null)
                                value.setId("created-student");
                        return value;
                });
                when(studentAcademicRecordRepository.save(any(StudentAcademicRecord.class)))
                                .thenAnswer(invocation -> {
                                        StudentAcademicRecord value = invocation.getArgument(0);
                                        value.setId("created-record");
                                        return value;
                                });

                StudentResponse response = studentService.createStudent(request);
                assertNotNull(response);
                assertEquals("created-record", response.getCurrentAcademicRecordDocsId());
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
                                .studentDocsId("student-id-123")
                                .academicYear("2026-2027")
                                .identityNo("IDN/2026/07/2401")
                                .rollNo("12")
                                .status(StudentStatus.ACTIVE)
                                .build();

                when(studentRepository.findById("student-id-123")).thenReturn(Optional.of(student));
                when(studentAcademicRecordRepository.findByStudentDocsId("student-id-123")).thenReturn(List.of(record));

                StudentResponse found = studentService.getStudentById("student-id-123");

                assertNotNull(found);
                assertEquals("student-id-123", found.getId());
                assertEquals("admission-789", found.getAdmissionDocsId());
                assertNotNull(found.getCurrentAcademicRecord());
                assertEquals("IDN/2026/07/2401", found.getCurrentAcademicRecord().getIdentityNo());
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
                when(studentAcademicRecordRepository.findByStudentDocsId("student-id-123"))
                                .thenReturn(new ArrayList<>());
                when(studentAcademicRecordRepository
                                .findFirstByStudentDocsIdAndAcademicYearAndStatusOrderByCreatedAtDesc(
                                                anyString(), anyString(), eq(StudentStatus.ACTIVE)))
                                .thenReturn(Optional.empty());
                when(studentRepository.save(any(Student.class))).thenAnswer(invocation -> invocation.getArgument(0));
                when(academicYearResolver.resolve(anyString(), any(), any())).thenReturn(academicYear);

                StudentResponse updated = studentService.updateStudent("student-id-123", details, null);

                assertNotNull(updated);
                assertEquals("Jane Doe", updated.getName());
                assertEquals("ADM-001", updated.getAdmissionNo()); // unchanged
        }

        @Test
        void createAcademicRecord_Success() {
                StudentAcademicRecord input = StudentAcademicRecord.builder()
                                .academicYear("2026-2027")
                                .classDocsId("class-new")
                                .build();

                when(studentRepository.findById("student-id-123")).thenReturn(Optional.of(student));
                when(schoolClassRepository.findById("class-new")).thenReturn(Optional.of(
                                SchoolClass.builder().id("class-new").schoolId("school-id-123")
                                                .academicYear("2026-2027").build()));
                when(academicYearResolver.resolve("school-id-123", "2026-2027", null)).thenReturn(academicYear);
                when(studentAcademicRecordRepository.findByStudentDocsId("student-id-123"))
                                .thenReturn(new ArrayList<>());
                when(studentAcademicRecordRepository
                                .findFirstByStudentDocsIdAndAcademicYearAndStatusOrderByCreatedAtDesc(
                                                "student-id-123", "2026-2027", StudentStatus.ACTIVE))
                                .thenReturn(Optional.empty());
                when(studentAcademicRecordRepository.save(any(StudentAcademicRecord.class)))
                                .thenAnswer(invocation -> invocation.getArgument(0));

                StudentAcademicRecord record = studentService.createAcademicRecord("student-id-123", input);

                assertNotNull(record);
                assertEquals("class-new", record.getClassDocsId());
                assertEquals("2026-2027", record.getAcademicYear());
                verify(studentAcademicRecordRepository, times(1)).save(any(StudentAcademicRecord.class));
        }

        @Test
        void createAcademicRecord_updatesStudentCurrentAcademicRecordPointer() {
                StudentAcademicRecord input = StudentAcademicRecord.builder()
                                .academicYear("2026-2027")
                                .classDocsId("class-new")
                                .build();

                when(studentRepository.findById("student-id-123")).thenReturn(Optional.of(student));
                when(schoolClassRepository.findById("class-new")).thenReturn(Optional.of(
                                SchoolClass.builder().id("class-new").schoolId("school-id-123")
                                                .academicYear("2026-2027").build()));
                when(academicYearResolver.resolve("school-id-123", "2026-2027", null)).thenReturn(academicYear);
                when(studentAcademicRecordRepository
                                .findFirstByStudentDocsIdAndAcademicYearAndStatusOrderByCreatedAtDesc(
                                                "student-id-123", "2026-2027", StudentStatus.ACTIVE))
                                .thenReturn(Optional.empty());
                when(studentAcademicRecordRepository.save(any(StudentAcademicRecord.class)))
                                .thenAnswer(i -> {
                                        StudentAcademicRecord r = i.getArgument(0);
                                        r.setId("rec-2026");
                                        return r;
                                });
                when(studentRepository.save(any(Student.class))).thenAnswer(i -> i.getArgument(0));

                studentService.createAcademicRecord("student-id-123", input);

                assertEquals("rec-2026", student.getCurrentAcademicRecordDocsId());
                verify(studentRepository, times(1)).save(student); // pointer persisted once
        }

        @Test
        void getAcademicHistory_Success() {
                StudentAcademicRecord r1 = StudentAcademicRecord.builder().academicYear("2025-2026").build();
                StudentAcademicRecord r2 = StudentAcademicRecord.builder().academicYear("2026-2027").build();

                when(studentRepository.existsById("student-id-123")).thenReturn(true);
                when(studentAcademicRecordRepository.findByStudentDocsId("student-id-123")).thenReturn(List.of(r1, r2));

                List<StudentAcademicRecord> history = studentService.getAcademicHistory("student-id-123");

                assertEquals(2, history.size());
                verify(studentAcademicRecordRepository, times(1)).findByStudentDocsId("student-id-123");
        }

        @Test
        void createAcademicRecord_sameAcademicYearKeepsHistoryAndRepointsStudent() {
                StudentAcademicRecord previous = StudentAcademicRecord.builder()
                                .id("record-old")
                                .schoolId("school-id-123")
                                .studentDocsId("student-id-123")
                                .academicYear("2026-2027")
                                .identityNo("IDN/2026/07/2410")
                                .rollNo("8A-01")
                                .classDocsId("class-8")
                                .sectionNo("A")
                                .status(StudentStatus.ACTIVE)
                                .build();
                StudentAcademicRecord details = StudentAcademicRecord.builder()
                                .academicYear("2026-2027")
                                .classDocsId("class-9")
                                .sectionNo("C")
                                .rollNo("9C-03")
                                .build();
                student.setCurrentAcademicRecordDocsId("record-old");

                when(studentRepository.findById("student-id-123")).thenReturn(Optional.of(student));
                when(academicYearResolver.resolve("school-id-123", "2026-2027", null))
                                .thenReturn(academicYear);
                when(studentAcademicRecordRepository
                                .findFirstByStudentDocsIdAndAcademicYearAndStatusOrderByCreatedAtDesc(
                                                "student-id-123", "2026-2027", StudentStatus.ACTIVE))
                                .thenReturn(Optional.of(previous));
                when(studentAcademicRecordRepository.findByStudentDocsId("student-id-123"))
                                .thenReturn(List.of(previous));
                when(schoolClassRepository.findById("class-9")).thenReturn(Optional.of(
                                SchoolClass.builder()
                                                .id("class-9")
                                                .schoolId("school-id-123")
                                                .academicYear("2026-2027")
                                                .sections(List.of("A", "B", "C"))
                                                .build()));
                when(studentAcademicRecordRepository.save(any(StudentAcademicRecord.class)))
                                .thenAnswer(invocation -> {
                                        StudentAcademicRecord value = invocation.getArgument(0);
                                        if (value != previous)
                                                value.setId("record-new");
                                        return value;
                                });
                when(studentRepository.save(any(Student.class))).thenAnswer(invocation -> invocation.getArgument(0));

                StudentAcademicRecord created = studentService.createAcademicRecord("student-id-123", details);

                assertEquals(StudentStatus.INACTIVE, previous.getStatus());
                assertEquals(StudentStatus.ACTIVE, created.getStatus());
                assertEquals("record-new", created.getId());
                assertEquals("IDN/2026/07/2410", created.getIdentityNo());
                assertEquals("class-9", created.getClassDocsId());
                assertEquals("C", created.getSectionNo());
                assertEquals("record-new", student.getCurrentAcademicRecordDocsId());
                verify(studentAcademicRecordRepository, times(2))
                                .save(any(StudentAcademicRecord.class));
                verify(studentRepository).save(student);
        }

        @Test
        void createAcademicRecord_deactivatesAllExistingRecordsBeforeAssigningNewCurrent() {
                StudentAcademicRecord current = StudentAcademicRecord.builder()
                                .id("record-current")
                                .schoolId("school-id-123")
                                .studentDocsId("student-id-123")
                                .academicYear("2026-2027")
                                .identityNo("IDN/2026/07/2410")
                                .classDocsId("class-8")
                                .sectionNo("A")
                                .status(StudentStatus.ACTIVE)
                                .build();
                StudentAcademicRecord otherActiveRecord = StudentAcademicRecord.builder()
                                .id("record-other-year")
                                .schoolId("school-id-123")
                                .studentDocsId("student-id-123")
                                .academicYear("2027-2028")
                                .identityNo("IDN/2027/07/2411")
                                .classDocsId("class-9")
                                .sectionNo("B")
                                .status(StudentStatus.ACTIVE)
                                .build();
                StudentAcademicRecord details = StudentAcademicRecord.builder()
                                .academicYear("2027-2028")
                                .classDocsId("class-9")
                                .sectionNo("C")
                                .rollNo("9C-03")
                                .build();
                AcademicYear nextAcademicYear = AcademicYear.builder()
                                .name("2027-2028")
                                .build();
                student.setCurrentAcademicRecordDocsId("record-current");

                when(studentRepository.findById("student-id-123")).thenReturn(Optional.of(student));
                when(academicYearResolver.resolve("school-id-123", "2027-2028", null))
                                .thenReturn(nextAcademicYear);
                when(studentAcademicRecordRepository
                                .findFirstByStudentDocsIdAndAcademicYearAndStatusOrderByCreatedAtDesc(
                                                "student-id-123", "2027-2028", StudentStatus.ACTIVE))
                                .thenReturn(Optional.of(otherActiveRecord));
                when(studentAcademicRecordRepository.findByStudentDocsId("student-id-123"))
                                .thenReturn(List.of(current, otherActiveRecord));
                when(schoolClassRepository.findById("class-9")).thenReturn(Optional.of(
                                SchoolClass.builder()
                                                .id("class-9")
                                                .schoolId("school-id-123")
                                                .academicYear("2027-2028")
                                                .sections(List.of("A", "B", "C"))
                                                .build()));
                when(studentAcademicRecordRepository.save(any(StudentAcademicRecord.class)))
                                .thenAnswer(invocation -> {
                                        StudentAcademicRecord value = invocation.getArgument(0);
                                        if (value != current && value != otherActiveRecord) {
                                                value.setId("record-new");
                                        }
                                        return value;
                                });
                when(studentRepository.save(any(Student.class)))
                                .thenAnswer(invocation -> invocation.getArgument(0));

                StudentAcademicRecord created =
                                studentService.createAcademicRecord("student-id-123", details);

                assertEquals(StudentStatus.INACTIVE, current.getStatus());
                assertEquals(StudentStatus.INACTIVE, otherActiveRecord.getStatus());
                assertEquals(StudentStatus.ACTIVE, created.getStatus());
                assertEquals("record-new", created.getId());
                assertEquals("IDN/2027/07/2411", created.getIdentityNo());
                assertEquals("record-new", student.getCurrentAcademicRecordDocsId());

                var saveOrder = inOrder(studentAcademicRecordRepository);
                saveOrder.verify(studentAcademicRecordRepository).save(current);
                saveOrder.verify(studentAcademicRecordRepository).save(otherActiveRecord);
                saveOrder.verify(studentAcademicRecordRepository).save(created);
                verify(studentRepository).save(student);
        }

        @Test
        void createAcademicRecord_rejectsNullDetails() {
                assertThrows(IllegalArgumentException.class,
                                () -> studentService.createAcademicRecord("student-id-123", null));
                verifyNoInteractions(studentRepository, studentAcademicRecordRepository, academicYearResolver,
                                schoolClassRepository);
        }

        @Test
        void createAcademicRecord_rejectsBlankStudentDocsIdOrAcademicYear() {
                StudentAcademicRecord input = StudentAcademicRecord.builder().academicYear("2026-2027").build();
                assertThrows(IllegalArgumentException.class,
                                () -> studentService.createAcademicRecord(" ", input));
                assertThrows(IllegalArgumentException.class,
                                () -> studentService.createAcademicRecord("student-id-123",
                                                StudentAcademicRecord.builder().academicYear(" ").build()));
                verifyNoInteractions(studentRepository, studentAcademicRecordRepository, academicYearResolver,
                                schoolClassRepository);
        }

        @Test
        void createAcademicRecord_rejectsAcademicYearNotOwnedBySchool() {
                StudentAcademicRecord input = StudentAcademicRecord.builder().academicYear("2028-2029").build();
                when(studentRepository.findById("student-id-123")).thenReturn(Optional.of(student));
                when(academicYearResolver.resolve("school-id-123", "2028-2029", null))
                                .thenThrow(new ResourceNotFoundException("Academic year not found for this school."));

                assertThrows(ResourceNotFoundException.class,
                                () -> studentService.createAcademicRecord("student-id-123", input));
                verify(studentAcademicRecordRepository, never()).save(any(StudentAcademicRecord.class));
        }

        @Test
        void createAcademicRecord_rejectsClassFromAnotherAcademicYear() {
                StudentAcademicRecord input = StudentAcademicRecord.builder()
                                .academicYear("2026-2027").classDocsId("class-old").build();
                when(studentRepository.findById("student-id-123")).thenReturn(Optional.of(student));
                when(academicYearResolver.resolve("school-id-123", "2026-2027", null)).thenReturn(academicYear);
                when(studentAcademicRecordRepository
                                .findFirstByStudentDocsIdAndAcademicYearAndStatusOrderByCreatedAtDesc(
                                                "student-id-123", "2026-2027", StudentStatus.ACTIVE))
                                .thenReturn(Optional.empty());
                when(schoolClassRepository.findById("class-old")).thenReturn(Optional.of(
                                SchoolClass.builder().id("class-old").schoolId("school-id-123")
                                                .academicYear("2025-2026").build()));

                assertThrows(IllegalArgumentException.class,
                                () -> studentService.createAcademicRecord("student-id-123", input));
                verify(studentAcademicRecordRepository, never()).save(any(StudentAcademicRecord.class));
        }

        @Test
        void createAcademicRecord_rejectsClassThatDoesNotExist() {
                StudentAcademicRecord input = StudentAcademicRecord.builder()
                                .academicYear("2026-2027")
                                .classDocsId("missing-class")
                                .build();
                when(studentRepository.findById("student-id-123")).thenReturn(Optional.of(student));
                when(academicYearResolver.resolve("school-id-123", "2026-2027", null)).thenReturn(academicYear);
                when(studentAcademicRecordRepository
                                .findFirstByStudentDocsIdAndAcademicYearAndStatusOrderByCreatedAtDesc(
                                                "student-id-123", "2026-2027", StudentStatus.ACTIVE))
                                .thenReturn(Optional.empty());
                when(schoolClassRepository.findById("missing-class")).thenReturn(Optional.empty());

                ResourceNotFoundException error = assertThrows(
                                ResourceNotFoundException.class,
                                () -> studentService.createAcademicRecord("student-id-123", input));

                assertEquals("Class not found with id: missing-class", error.getMessage());
                verify(studentAcademicRecordRepository, never()).save(any(StudentAcademicRecord.class));
        }

        @Test
        void createAcademicRecord_rejectsUnknownSectionAndSectionWithoutClass() {
                StudentAcademicRecord unknownSection = StudentAcademicRecord.builder()
                                .academicYear("2026-2027").classDocsId("class-new").sectionNo("C").build();
                when(studentRepository.findById("student-id-123")).thenReturn(Optional.of(student));
                when(academicYearResolver.resolve("school-id-123", "2026-2027", null)).thenReturn(academicYear);
                when(studentAcademicRecordRepository
                                .findFirstByStudentDocsIdAndAcademicYearAndStatusOrderByCreatedAtDesc(
                                                "student-id-123", "2026-2027", StudentStatus.ACTIVE))
                                .thenReturn(Optional.empty());
                when(schoolClassRepository.findById("class-new")).thenReturn(Optional.of(
                                SchoolClass.builder().id("class-new").schoolId("school-id-123")
                                                .academicYear("2026-2027").sections(List.of("A", "B")).build()));

                assertThrows(IllegalArgumentException.class,
                                () -> studentService.createAcademicRecord("student-id-123", unknownSection));

                StudentAcademicRecord sectionWithoutClass = StudentAcademicRecord.builder()
                                .academicYear("2026-2027").sectionNo("A").build();
                assertThrows(IllegalArgumentException.class,
                                () -> studentService.createAcademicRecord("student-id-123",
                                                sectionWithoutClass));
                verify(studentAcademicRecordRepository, never()).save(any(StudentAcademicRecord.class));
        }

        @Test
        void createAcademicRecord_normalizesSectionAndBlankOptionalValues() {
                StudentAcademicRecord input = StudentAcademicRecord.builder()
                                .academicYear("2026-2027").classDocsId("class-new").sectionNo("a")
                                .identityNo(" ").rollNo(" ").hostelRoomNo(" ").build();
                when(studentRepository.findById("student-id-123")).thenReturn(Optional.of(student));
                when(academicYearResolver.resolve("school-id-123", "2026-2027", null)).thenReturn(academicYear);
                when(studentAcademicRecordRepository
                                .findFirstByStudentDocsIdAndAcademicYearAndStatusOrderByCreatedAtDesc(
                                                "student-id-123", "2026-2027", StudentStatus.ACTIVE))
                                .thenReturn(Optional.empty());
                when(schoolClassRepository.findById("class-new")).thenReturn(Optional.of(
                                SchoolClass.builder().id("class-new").schoolId("school-id-123")
                                                .academicYear("2026-2027").sections(List.of("A", "B")).build()));
                when(studentAcademicRecordRepository.save(any(StudentAcademicRecord.class)))
                                .thenAnswer(invocation -> invocation.getArgument(0));

                StudentAcademicRecord saved = studentService.createAcademicRecord("student-id-123", input);

                assertEquals("A", saved.getSectionNo());
                assertNull(saved.getIdentityNo());
                assertNull(saved.getRollNo());
                assertNull(saved.getHostelRoomNo());
        }

        @Test
        void createAcademicRecord_createsActivePlacementWhenOnlyHistoryExists() {
                StudentAcademicRecord input = StudentAcademicRecord.builder()
                                .academicYear("2026-2027").identityNo("CUSTOM-IDENTITY").build();
                when(studentRepository.findById("student-id-123")).thenReturn(Optional.of(student));
                when(academicYearResolver.resolve("school-id-123", "2026-2027", null)).thenReturn(academicYear);
                when(studentAcademicRecordRepository
                                .findFirstByStudentDocsIdAndAcademicYearAndStatusOrderByCreatedAtDesc(
                                                "student-id-123", "2026-2027", StudentStatus.ACTIVE))
                                .thenReturn(Optional.empty());
                when(studentAcademicRecordRepository.save(any(StudentAcademicRecord.class)))
                                .thenAnswer(invocation -> invocation.getArgument(0));

                StudentAcademicRecord saved = studentService.createAcademicRecord("student-id-123", input);

                assertEquals(StudentStatus.ACTIVE, saved.getStatus());
                assertEquals("CUSTOM-IDENTITY", saved.getIdentityNo());
        }

        @Test
        void createAcademicRecord_translatesDuplicateIndexToConflict() {
                StudentAcademicRecord input = StudentAcademicRecord.builder()
                                .academicYear("2026-2027").identityNo("IDN/2026/07/2401").build();
                when(studentRepository.findById("student-id-123")).thenReturn(Optional.of(student));
                when(academicYearResolver.resolve("school-id-123", "2026-2027", null)).thenReturn(academicYear);
                when(studentAcademicRecordRepository
                                .findFirstByStudentDocsIdAndAcademicYearAndStatusOrderByCreatedAtDesc(
                                                "student-id-123", "2026-2027", StudentStatus.ACTIVE))
                                .thenReturn(Optional.empty());
                when(studentAcademicRecordRepository.save(any(StudentAcademicRecord.class)))
                                .thenThrow(new DuplicateKeyException("school_year_active_identity_no_unique_idx"));

                ConflictException error = assertThrows(ConflictException.class,
                                () -> studentService.createAcademicRecord("student-id-123", input));
                assertTrue(error.getMessage().contains("identityNo"));
        }

        @Test
        void createAcademicRecord_rejectsNonActiveCurrentStatus() {
                StudentAcademicRecord input = StudentAcademicRecord.builder()
                                .academicYear("2026-2027")
                                .status(StudentStatus.INACTIVE)
                                .build();
                when(studentRepository.findById("student-id-123")).thenReturn(Optional.of(student));
                when(academicYearResolver.resolve("school-id-123", "2026-2027", null)).thenReturn(academicYear);
                when(studentAcademicRecordRepository
                                .findFirstByStudentDocsIdAndAcademicYearAndStatusOrderByCreatedAtDesc(
                                                "student-id-123", "2026-2027", StudentStatus.ACTIVE))
                                .thenReturn(Optional.empty());

                IllegalArgumentException error = assertThrows(
                                IllegalArgumentException.class,
                                () -> studentService.createAcademicRecord("student-id-123", input));

                assertEquals("A newly assigned current academic record must have status ACTIVE.",
                                error.getMessage());
                verify(studentAcademicRecordRepository, never()).save(any(StudentAcademicRecord.class));
        }

        @Test
        void updateAcademicRecord_updatesOnlyProvidedFieldsAndNormalizesValues() {
                StudentAcademicRecord record = StudentAcademicRecord.builder()
                                .id("record-1")
                                .schoolId("school-id-123")
                                .studentDocsId("student-id-123")
                                .academicYear("2026-2027")
                                .identityNo("OLD-ID")
                                .rollNo("8A-01")
                                .classDocsId("class-old")
                                .sectionNo("A")
                                .hostelRoomNo("101")
                                .status(StudentStatus.ACTIVE)
                                .build();
                UpdateAcademicRecordRequest request = new UpdateAcademicRecordRequest();
                request.setIdentityNo("  NEW-ID  ");
                request.setClassDocsId("class-new");
                request.setSectionNo("b");
                request.setHostelRoomNo(null);
                student.setCurrentAcademicRecordDocsId("record-1");

                when(studentRepository.findById("student-id-123")).thenReturn(Optional.of(student));
                when(studentAcademicRecordRepository.findById("record-1"))
                                .thenReturn(Optional.of(record));
                when(academicYearResolver.resolve("school-id-123", "2026-2027", null))
                                .thenReturn(academicYear);
                when(schoolClassRepository.findById("class-new")).thenReturn(Optional.of(
                                SchoolClass.builder()
                                                .id("class-new")
                                                .schoolId("school-id-123")
                                                .academicYear("2026-2027")
                                                .sections(List.of("A", "B"))
                                                .build()));
                when(studentAcademicRecordRepository.findByStudentDocsId("student-id-123"))
                                .thenReturn(List.of(record));
                when(studentAcademicRecordRepository.save(record)).thenReturn(record);

                StudentAcademicRecord updated = studentService.updateAcademicRecord(
                                "student-id-123", "record-1", request);

                assertEquals("NEW-ID", updated.getIdentityNo());
                assertEquals("8A-01", updated.getRollNo());
                assertEquals("class-new", updated.getClassDocsId());
                assertEquals("B", updated.getSectionNo());
                assertNull(updated.getHostelRoomNo());
                assertEquals("2026-2027", updated.getAcademicYear());
                assertEquals(StudentStatus.ACTIVE, updated.getStatus());
                verify(studentAcademicRecordRepository).save(record);
                verify(studentRepository, never()).save(any(Student.class));
        }

        @Test
        void updateAcademicRecord_clearingClassClearsOmittedSectionAndRoll() {
                StudentAcademicRecord record = StudentAcademicRecord.builder()
                                .id("record-1")
                                .schoolId("school-id-123")
                                .studentDocsId("student-id-123")
                                .academicYear("2026-2027")
                                .rollNo("8A-01")
                                .classDocsId("class-old")
                                .sectionNo("A")
                                .status(StudentStatus.ACTIVE)
                                .build();
                UpdateAcademicRecordRequest request = new UpdateAcademicRecordRequest();
                request.setClassDocsId(" ");

                when(studentRepository.findById("student-id-123")).thenReturn(Optional.of(student));
                when(studentAcademicRecordRepository.findById("record-1"))
                                .thenReturn(Optional.of(record));
                when(academicYearResolver.resolve("school-id-123", "2026-2027", null))
                                .thenReturn(academicYear);
                when(studentAcademicRecordRepository.save(record)).thenReturn(record);

                StudentAcademicRecord updated = studentService.updateAcademicRecord(
                                "student-id-123", "record-1", request);

                assertNull(updated.getClassDocsId());
                assertNull(updated.getSectionNo());
                assertNull(updated.getRollNo());
                assertEquals(StudentStatus.ACTIVE, updated.getStatus());
        }

        @Test
        void updateAcademicRecord_rejectsEmptyBodyAndNullStatus() {
                IllegalArgumentException emptyError = assertThrows(
                                IllegalArgumentException.class,
                                () -> studentService.updateAcademicRecord(
                                                "student-id-123",
                                                "record-1",
                                                new UpdateAcademicRecordRequest()));
                assertEquals(
                                "At least one editable academic-record field is required.",
                                emptyError.getMessage());

                StudentAcademicRecord record = StudentAcademicRecord.builder()
                                .id("record-1")
                                .schoolId("school-id-123")
                                .studentDocsId("student-id-123")
                                .academicYear("2026-2027")
                                .status(StudentStatus.ACTIVE)
                                .build();
                UpdateAcademicRecordRequest nullStatus = new UpdateAcademicRecordRequest();
                nullStatus.setStatus(null);
                when(studentRepository.findById("student-id-123")).thenReturn(Optional.of(student));
                when(studentAcademicRecordRepository.findById("record-1"))
                                .thenReturn(Optional.of(record));
                when(academicYearResolver.resolve("school-id-123", "2026-2027", null))
                                .thenReturn(academicYear);

                IllegalArgumentException statusError = assertThrows(
                                IllegalArgumentException.class,
                                () -> studentService.updateAcademicRecord(
                                                "student-id-123", "record-1", nullStatus));
                assertEquals("status cannot be null.", statusError.getMessage());
                verify(studentAcademicRecordRepository, never())
                                .save(any(StudentAcademicRecord.class));
        }

        @Test
        void updateAcademicRecord_rejectsMissingOrForeignRecord() {
                UpdateAcademicRecordRequest request = new UpdateAcademicRecordRequest();
                request.setIdentityNo("NEW-ID");
                when(studentRepository.findById("student-id-123")).thenReturn(Optional.of(student));
                when(studentAcademicRecordRepository.findById("missing-record"))
                                .thenReturn(Optional.empty());

                assertThrows(
                                ResourceNotFoundException.class,
                                () -> studentService.updateAcademicRecord(
                                                "student-id-123", "missing-record", request));

                StudentAcademicRecord foreignRecord = StudentAcademicRecord.builder()
                                .id("foreign-record")
                                .schoolId("school-id-123")
                                .studentDocsId("another-student")
                                .academicYear("2026-2027")
                                .status(StudentStatus.ACTIVE)
                                .build();
                when(studentAcademicRecordRepository.findById("foreign-record"))
                                .thenReturn(Optional.of(foreignRecord));

                IllegalArgumentException ownershipError = assertThrows(
                                IllegalArgumentException.class,
                                () -> studentService.updateAcademicRecord(
                                                "student-id-123", "foreign-record", request));
                assertTrue(ownershipError.getMessage().contains("does not belong to student"));
                verify(studentAcademicRecordRepository, never())
                                .save(any(StudentAcademicRecord.class));
        }

        @Test
        void updateAcademicRecord_rejectsClassFromWrongYearAndUnknownSection() {
                StudentAcademicRecord record = StudentAcademicRecord.builder()
                                .id("record-1")
                                .schoolId("school-id-123")
                                .studentDocsId("student-id-123")
                                .academicYear("2026-2027")
                                .status(StudentStatus.ACTIVE)
                                .build();
                UpdateAcademicRecordRequest wrongYear = new UpdateAcademicRecordRequest();
                wrongYear.setClassDocsId("class-wrong-year");

                when(studentRepository.findById("student-id-123")).thenReturn(Optional.of(student));
                when(studentAcademicRecordRepository.findById("record-1"))
                                .thenReturn(Optional.of(record));
                when(academicYearResolver.resolve("school-id-123", "2026-2027", null))
                                .thenReturn(academicYear);
                when(schoolClassRepository.findById("class-wrong-year")).thenReturn(Optional.of(
                                SchoolClass.builder()
                                                .id("class-wrong-year")
                                                .schoolId("school-id-123")
                                                .academicYear("2025-2026")
                                                .build()));

                IllegalArgumentException yearError = assertThrows(
                                IllegalArgumentException.class,
                                () -> studentService.updateAcademicRecord(
                                                "student-id-123", "record-1", wrongYear));
                assertTrue(yearError.getMessage().contains(
                                "does not belong to academic year '2026-2027'"));

                record.setClassDocsId(null);
                UpdateAcademicRecordRequest unknownSection = new UpdateAcademicRecordRequest();
                unknownSection.setClassDocsId("class-valid");
                unknownSection.setSectionNo("Z");
                when(schoolClassRepository.findById("class-valid")).thenReturn(Optional.of(
                                SchoolClass.builder()
                                                .id("class-valid")
                                                .schoolId("school-id-123")
                                                .academicYear("2026-2027")
                                                .sections(List.of("A", "B"))
                                                .build()));

                IllegalArgumentException sectionError = assertThrows(
                                IllegalArgumentException.class,
                                () -> studentService.updateAcademicRecord(
                                                "student-id-123", "record-1", unknownSection));
                assertTrue(sectionError.getMessage().contains("does not exist in class"));
                verify(studentAcademicRecordRepository, never())
                                .save(any(StudentAcademicRecord.class));
        }

        @Test
        void updateAcademicRecord_rejectsDuplicateIdentityAndRollForActiveRecord() {
                StudentAcademicRecord record = StudentAcademicRecord.builder()
                                .id("record-1")
                                .schoolId("school-id-123")
                                .studentDocsId("student-id-123")
                                .academicYear("2026-2027")
                                .classDocsId("class-9")
                                .sectionNo("A")
                                .status(StudentStatus.ACTIVE)
                                .build();
                UpdateAcademicRecordRequest duplicateIdentity = new UpdateAcademicRecordRequest();
                duplicateIdentity.setIdentityNo("DUPLICATE-ID");

                when(studentRepository.findById("student-id-123")).thenReturn(Optional.of(student));
                when(studentAcademicRecordRepository.findById("record-1"))
                                .thenReturn(Optional.of(record));
                when(academicYearResolver.resolve("school-id-123", "2026-2027", null))
                                .thenReturn(academicYear);
                when(schoolClassRepository.findById("class-9")).thenReturn(Optional.of(
                                SchoolClass.builder()
                                                .id("class-9")
                                                .schoolId("school-id-123")
                                                .academicYear("2026-2027")
                                                .sections(List.of("A", "B"))
                                                .build()));
                when(studentAcademicRecordRepository
                                .findFirstBySchoolIdAndAcademicYearAndIdentityNoAndStatus(
                                                "school-id-123",
                                                "2026-2027",
                                                "DUPLICATE-ID",
                                                StudentStatus.ACTIVE))
                                .thenReturn(Optional.of(StudentAcademicRecord.builder()
                                                .id("other-record")
                                                .studentDocsId("another-student")
                                                .build()));

                assertThrows(
                                ConflictException.class,
                                () -> studentService.updateAcademicRecord(
                                                "student-id-123",
                                                "record-1",
                                                duplicateIdentity));

                record.setIdentityNo(null);
                UpdateAcademicRecordRequest duplicateRoll = new UpdateAcademicRecordRequest();
                duplicateRoll.setRollNo("9A-01");
                when(studentAcademicRecordRepository
                                .findFirstByClassDocsIdAndSectionNoAndAcademicYearAndRollNoAndStatus(
                                                "class-9", "A", "2026-2027", "9A-01",
                                                StudentStatus.ACTIVE))
                                .thenReturn(Optional.of(StudentAcademicRecord.builder()
                                                .id("other-record")
                                                .studentDocsId("another-student")
                                                .build()));

                assertThrows(
                                ConflictException.class,
                                () -> studentService.updateAcademicRecord(
                                                "student-id-123", "record-1", duplicateRoll));
                verify(studentAcademicRecordRepository, never())
                                .save(any(StudentAcademicRecord.class));
        }

        @Test
        void updateAcademicRecord_rejectsInactiveRecordBeforeMutation() {
                StudentAcademicRecord record = StudentAcademicRecord.builder()
                                .id("record-1")
                                .schoolId("school-id-123")
                                .studentDocsId("student-id-123")
                                .academicYear("2026-2027")
                                .classDocsId("class-9")
                                .sectionNo("A")
                                .rollNo("OLD-ROLL")
                                .status(StudentStatus.INACTIVE)
                                .build();
                UpdateAcademicRecordRequest request = new UpdateAcademicRecordRequest();
                request.setRollNo("9A-01");

                when(studentRepository.findById("student-id-123")).thenReturn(Optional.of(student));
                when(studentAcademicRecordRepository.findById("record-1"))
                                .thenReturn(Optional.of(record));
                ConflictException error = assertThrows(
                                ConflictException.class,
                                () -> studentService.updateAcademicRecord(
                                                "student-id-123", "record-1", request));

                assertEquals(
                                "Academic record 'record-1' is INACTIVE and cannot be edited. "
                                                + "Only ACTIVE academic records are editable.",
                                error.getMessage());
                assertEquals("OLD-ROLL", record.getRollNo());
                verifyNoInteractions(academicYearResolver);
                verify(studentAcademicRecordRepository, never())
                                .save(any(StudentAcademicRecord.class));
        }

        @Test
        void updateAcademicRecord_cannotReactivateInactiveHistory() {
                StudentAcademicRecord target = StudentAcademicRecord.builder()
                                .id("record-target")
                                .schoolId("school-id-123")
                                .studentDocsId("student-id-123")
                                .academicYear("2026-2027")
                                .status(StudentStatus.INACTIVE)
                                .build();
                UpdateAcademicRecordRequest request = new UpdateAcademicRecordRequest();
                request.setStatus(StudentStatus.ACTIVE);
                student.setCurrentAcademicRecordDocsId("record-current");

                when(studentRepository.findById("student-id-123")).thenReturn(Optional.of(student));
                when(studentAcademicRecordRepository.findById("record-target"))
                                .thenReturn(Optional.of(target));

                ConflictException error = assertThrows(
                                ConflictException.class,
                                () -> studentService.updateAcademicRecord(
                                                "student-id-123", "record-target", request));

                assertTrue(error.getMessage().contains("Only ACTIVE academic records are editable"));
                assertEquals(StudentStatus.INACTIVE, target.getStatus());
                assertEquals("record-current", student.getCurrentAcademicRecordDocsId());
                verifyNoInteractions(academicYearResolver);
                verify(studentAcademicRecordRepository, never()).save(any());
                verify(studentRepository, never()).save(any());
        }

        @Test
        void updateAcademicRecord_inactivatingCurrentClearsPointerWhenNoActiveRecordRemains() {
                StudentAcademicRecord current = StudentAcademicRecord.builder()
                                .id("record-current")
                                .schoolId("school-id-123")
                                .studentDocsId("student-id-123")
                                .academicYear("2026-2027")
                                .status(StudentStatus.ACTIVE)
                                .build();
                UpdateAcademicRecordRequest request = new UpdateAcademicRecordRequest();
                request.setStatus(StudentStatus.INACTIVE);
                student.setCurrentAcademicRecordDocsId("record-current");

                when(studentRepository.findById("student-id-123")).thenReturn(Optional.of(student));
                when(studentAcademicRecordRepository.findById("record-current"))
                                .thenReturn(Optional.of(current));
                when(academicYearResolver.resolve("school-id-123", "2026-2027", null))
                                .thenReturn(academicYear);
                when(studentAcademicRecordRepository.save(current)).thenReturn(current);
                when(studentAcademicRecordRepository.findByStudentDocsId("student-id-123"))
                                .thenReturn(List.of(current));
                when(studentRepository.save(student)).thenReturn(student);

                StudentAcademicRecord updated = studentService.updateAcademicRecord(
                                "student-id-123", "record-current", request);

                assertEquals(StudentStatus.INACTIVE, updated.getStatus());
                assertNull(student.getCurrentAcademicRecordDocsId());
                verify(studentRepository).save(student);
        }

        @Test
        void updateAcademicRecord_inactivatingCurrentRepointsToNewestRemainingActiveRecord() {
                StudentAcademicRecord current = StudentAcademicRecord.builder()
                                .id("record-current")
                                .schoolId("school-id-123")
                                .studentDocsId("student-id-123")
                                .academicYear("2026-2027")
                                .status(StudentStatus.ACTIVE)
                                .build();
                StudentAcademicRecord remainingActive = StudentAcademicRecord.builder()
                                .id("record-remaining")
                                .schoolId("school-id-123")
                                .studentDocsId("student-id-123")
                                .academicYear("2025-2026")
                                .status(StudentStatus.ACTIVE)
                                .build();
                UpdateAcademicRecordRequest request = new UpdateAcademicRecordRequest();
                request.setStatus(StudentStatus.SUSPENDED);
                student.setCurrentAcademicRecordDocsId("record-current");

                when(studentRepository.findById("student-id-123")).thenReturn(Optional.of(student));
                when(studentAcademicRecordRepository.findById("record-current"))
                                .thenReturn(Optional.of(current));
                when(academicYearResolver.resolve("school-id-123", "2026-2027", null))
                                .thenReturn(academicYear);
                when(studentAcademicRecordRepository.save(current)).thenReturn(current);
                when(studentAcademicRecordRepository.findByStudentDocsId("student-id-123"))
                                .thenReturn(List.of(current, remainingActive));
                when(studentRepository.save(student)).thenReturn(student);

                StudentAcademicRecord updated = studentService.updateAcademicRecord(
                                "student-id-123", "record-current", request);

                assertEquals(StudentStatus.SUSPENDED, updated.getStatus());
                assertEquals("record-remaining", student.getCurrentAcademicRecordDocsId());
                verify(studentRepository).save(student);
        }

        @Test
        void updateAcademicRecord_translatesConcurrentDuplicateToConflict() {
                StudentAcademicRecord record = StudentAcademicRecord.builder()
                                .id("record-1")
                                .schoolId("school-id-123")
                                .studentDocsId("student-id-123")
                                .academicYear("2026-2027")
                                .status(StudentStatus.ACTIVE)
                                .build();
                UpdateAcademicRecordRequest request = new UpdateAcademicRecordRequest();
                request.setIdentityNo("CONCURRENT-ID");

                when(studentRepository.findById("student-id-123")).thenReturn(Optional.of(student));
                when(studentAcademicRecordRepository.findById("record-1"))
                                .thenReturn(Optional.of(record));
                when(academicYearResolver.resolve("school-id-123", "2026-2027", null))
                                .thenReturn(academicYear);
                when(studentAcademicRecordRepository.findByStudentDocsId("student-id-123"))
                                .thenReturn(List.of(record));
                when(studentAcademicRecordRepository.save(record))
                                .thenThrow(new DuplicateKeyException(
                                                "school_year_active_identity_no_unique_idx"));

                ConflictException error = assertThrows(
                                ConflictException.class,
                                () -> studentService.updateAcademicRecord(
                                                "student-id-123", "record-1", request));
                assertTrue(error.getMessage().contains("identityNo"));
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
                when(studentRepository.findByGuardiansGuardianDocsId("guardian-1"))
                                .thenReturn(List.of(student, sibling));
                when(studentAcademicRecordRepository.findByStudentDocsIdIn(anyList())).thenReturn(new ArrayList<>());

                List<StudentResponse> siblings = studentService.getSiblings("student-id-123");

                assertEquals(1, siblings.size());
                assertEquals("sibling-id-999", siblings.get(0).getId());
                verify(studentRepository, times(1)).findByGuardiansGuardianDocsId("guardian-1");
        }

        private CreateStudentRequest validDirectStudentRequest() {
                CreateStudentRequest request = new CreateStudentRequest();
                request.setSchoolId("school-id-123");
                request.setAdmissionNo("ADM-001");
                request.setName("John Doe");
                AcademicRecordRequest academicRecordRequest = new AcademicRecordRequest();
                academicRecordRequest.setAcademicYear("2026-2027");
                request.setCurrentAcademicRecord(academicRecordRequest);
                return request;
        }

        private void stubCreationChecks() {
                when(schoolRepository.findById("school-id-123")).thenReturn(Optional.of(school));
                when(studentRepository.countBySchoolId("school-id-123")).thenReturn(0L);
                when(studentRepository.findByAdmissionNo("ADM-001")).thenReturn(Optional.empty());
                when(academicYearResolver.resolve("school-id-123", "2026-2027", null))
                                .thenReturn(academicYear);
        }
}
