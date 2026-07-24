package com.orbitastra.backend.services.crm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;

import com.orbitastra.backend.dto.crm.ConvertAdmissionRequest;
import com.orbitastra.backend.dto.student.AcademicRecordRequest;
import com.orbitastra.backend.dto.student.StudentGuardianRequest;
import com.orbitastra.backend.dto.student.StudentResponse;
import com.orbitastra.backend.exceptions.ConflictException;
import com.orbitastra.backend.exceptions.ResourceNotFoundException;
import com.orbitastra.backend.models.crm.Admission;
import com.orbitastra.backend.models.crm.Inquiry;
import com.orbitastra.backend.models.crm.embedded.InquiryGuardian;
import com.orbitastra.backend.models.crm.enums.AdmissionStatus;
import com.orbitastra.backend.models.student.Student;
import com.orbitastra.backend.models.student.StudentAcademicRecord;
import com.orbitastra.backend.models.student.embedded.GuardianLink;
import com.orbitastra.backend.models.student.enums.Gender;
import com.orbitastra.backend.models.student.enums.GuardianRelation;
import com.orbitastra.backend.repositories.crm.AdmissionRepository;
import com.orbitastra.backend.services.student.GuardianService;
import com.orbitastra.backend.services.student.StudentService;

@ExtendWith(MockitoExtension.class)
class AdmissionServiceTest {

    @Mock
    private AdmissionRepository admissionRepository;

    @Mock
    private InquiryService inquiryService;

    @Mock
    private StudentService studentService;

    @Mock
    private GuardianService guardianService;

    @InjectMocks
    private AdmissionService admissionService;

    @Test
    void createAdmission_directAdmission_keepsInquiryIdNullAndAppliesDefaults() {
        Admission request = Admission.builder()
                .schoolId("school-123")
                .studentName("Direct Applicant")
                .inquiryDocsId("   ")
                .build();
        saveWithGeneratedId();

        Admission saved = admissionService.createAdmission(request);

        assertNull(saved.getInquiryDocsId());
        assertEquals("school-123", saved.getSchoolId());
        assertEquals("Direct Applicant", saved.getStudentName());
        assertEquals(AdmissionStatus.PENDING, saved.getStatus());
        assertNotNull(saved.getAdmissionNo());
        assertTrue(saved.getAdmissionNo().startsWith("ADM-"));
        assertNotNull(saved.getAdmissionDate());
        assertNotNull(saved.getDocuments());
        assertNotNull(saved.getGuardians());
        verifyNoInteractions(inquiryService);
    }

    @Test
    void createAdmission_customAdmissionNo_isTrimmedAndPreserved() {
        Admission request = Admission.builder()
                .schoolId("school-123")
                .admissionNo("  ADM-2026-0001  ")
                .studentName("Direct Applicant")
                .build();
        saveWithGeneratedId();

        Admission saved = admissionService.createAdmission(request);

        assertEquals("ADM-2026-0001", saved.getAdmissionNo());
        verify(admissionRepository).existsByAdmissionNo("ADM-2026-0001");
    }

    @Test
    void createAdmission_duplicateAdmissionNo_returnsConflictWithoutSaving() {
        when(admissionRepository.existsByAdmissionNo("ADM-2026-0001")).thenReturn(true);
        Admission request = Admission.builder()
                .schoolId("school-123")
                .admissionNo("ADM-2026-0001")
                .studentName("Direct Applicant")
                .build();

        ConflictException ex = assertThrows(
                ConflictException.class,
                () -> admissionService.createAdmission(request));

        assertEquals("An admission already exists with admissionNo: ADM-2026-0001", ex.getMessage());
        verify(admissionRepository, never()).save(any());
    }

    @Test
    void createAdmission_concurrentDuplicateAdmissionNo_returnsConflict() {
        Admission request = Admission.builder()
                .schoolId("school-123")
                .admissionNo("ADM-2026-0001")
                .studentName("Direct Applicant")
                .build();
        when(admissionRepository.save(any(Admission.class)))
                .thenThrow(new DuplicateKeyException("E11000 index: admissionNo_1 dup key"));

        ConflictException ex = assertThrows(
                ConflictException.class,
                () -> admissionService.createAdmission(request));

        assertEquals("An admission already exists with admissionNo: ADM-2026-0001", ex.getMessage());
    }

    @Test
    void createAdmission_directAdmissionWithoutSchool_rejected() {
        Admission request = Admission.builder().studentName("Direct Applicant").build();

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> admissionService.createAdmission(request));

        assertEquals("schoolId is required when inquiryDocsId is not provided.", ex.getMessage());
        verify(admissionRepository, never()).save(any());
    }

    @Test
    void createAdmission_inquiryIdOnly_copiesSnapshotAndInfersSchool() {
        String inquiryDocsId = "inquiry-456";
        InquiryGuardian guardian = guardian("Parent One", GuardianRelation.MOTHER,
                "0400111222", "parent@example.com", "Old Address");
        Inquiry inquiry = Inquiry.builder()
                .id(inquiryDocsId)
                .schoolId("school-123")
                .studentName("Inquiry Applicant")
                .guardians(List.of(guardian))
                .build();
        when(inquiryService.getInquiryById(inquiryDocsId)).thenReturn(inquiry);
        when(admissionRepository.existsByInquiryDocsId(inquiryDocsId)).thenReturn(false);
        saveWithGeneratedId();

        Admission saved = admissionService.createAdmission(
                Admission.builder().inquiryDocsId(inquiryDocsId).build());

        assertEquals("school-123", saved.getSchoolId());
        assertEquals("Inquiry Applicant", saved.getStudentName());
        assertEquals(inquiryDocsId, saved.getInquiryDocsId());
        assertEquals(1, saved.getGuardians().size());
        assertEquals("parent@example.com", saved.getGuardians().get(0).getEmail());
        assertFalse(saved.getGuardians().get(0) == guardian, "The admission must own its snapshot copy");
        verify(inquiryService).linkAdmission(inquiryDocsId, "admission-789");
    }

    @Test
    void createAdmission_inquiryWithOverrides_mergesApplicantAndGuardianDetails() {
        String inquiryDocsId = "inquiry-456";
        InquiryGuardian existingMother = guardian("Meera Nair", GuardianRelation.MOTHER,
                "+61-400-111-222", "meera@example.com", "Old Address");
        Inquiry inquiry = Inquiry.builder()
                .id(inquiryDocsId)
                .schoolId("school-123")
                .studentName("Aarav Nair")
                .guardians(List.of(existingMother))
                .build();
        InquiryGuardian motherOverride = guardian("Meera Nair", GuardianRelation.MOTHER,
                null, null, "12 Main Street");
        InquiryGuardian additionalFather = guardian("Raj Nair", GuardianRelation.FATHER,
                "+61-400-333-444", null, "12 Main Street");
        LocalDate dob = LocalDate.of(2015, 5, 10);
        Admission request = Admission.builder()
                .schoolId("school-123")
                .inquiryDocsId(inquiryDocsId)
                .studentName("Aarav Kumar Nair")
                .dob(dob)
                .gender(Gender.MALE)
                .documents(List.of("birth-certificate.pdf"))
                .guardians(List.of(motherOverride, additionalFather))
                .build();
        when(inquiryService.getInquiryById(inquiryDocsId)).thenReturn(inquiry);
        when(admissionRepository.existsByInquiryDocsId(inquiryDocsId)).thenReturn(false);
        saveWithGeneratedId();

        Admission saved = admissionService.createAdmission(request);

        assertEquals("Aarav Kumar Nair", saved.getStudentName());
        assertEquals(dob, saved.getDob());
        assertEquals(2, saved.getGuardians().size());
        InquiryGuardian mergedMother = saved.getGuardians().get(0);
        assertEquals("+61-400-111-222", mergedMother.getPhone());
        assertEquals("meera@example.com", mergedMother.getEmail());
        assertEquals("12 Main Street", mergedMother.getAddress());
        assertEquals("Raj Nair", saved.getGuardians().get(1).getName());
    }

    @Test
    void createAdmission_inquiryWithGuardianOnly_stillSupportsIdOnlyConversion() {
        String inquiryDocsId = "inquiry-456";
        Inquiry inquiry = Inquiry.builder()
                .id(inquiryDocsId)
                .schoolId("school-123")
                .guardians(List.of(guardian("Parent One", GuardianRelation.MOTHER,
                        "0400111222", null, null)))
                .build();
        when(inquiryService.getInquiryById(inquiryDocsId)).thenReturn(inquiry);
        saveWithGeneratedId();

        Admission saved = admissionService.createAdmission(
                Admission.builder().inquiryDocsId(inquiryDocsId).build());

        assertNull(saved.getStudentName());
        assertEquals(1, saved.getGuardians().size());
        verify(inquiryService).linkAdmission(inquiryDocsId, "admission-789");
    }

    @Test
    void createAdmission_missingInquiry_propagatesNotFoundAndDoesNotSave() {
        String inquiryDocsId = "missing-inquiry";
        when(inquiryService.getInquiryById(inquiryDocsId))
                .thenThrow(new ResourceNotFoundException("Inquiry not found with id: " + inquiryDocsId));

        assertThrows(ResourceNotFoundException.class, () -> admissionService.createAdmission(
                Admission.builder().inquiryDocsId(inquiryDocsId).build()));

        verify(admissionRepository, never()).save(any());
    }

    @Test
    void createAdmission_duplicateInquiry_returnsConflict() {
        String inquiryDocsId = "inquiry-456";
        Inquiry inquiry = Inquiry.builder()
                .id(inquiryDocsId)
                .schoolId("school-123")
                .studentName("John Doe")
                .build();
        when(inquiryService.getInquiryById(inquiryDocsId)).thenReturn(inquiry);
        when(admissionRepository.existsByInquiryDocsId(inquiryDocsId)).thenReturn(true);

        ConflictException ex = assertThrows(ConflictException.class, () -> admissionService.createAdmission(
                Admission.builder().inquiryDocsId(inquiryDocsId).build()));

        assertEquals("An admission already exists for inquiryDocsId: " + inquiryDocsId, ex.getMessage());
        verify(admissionRepository, never()).save(any());
    }

    @Test
    void createAdmission_concurrentDuplicateDetectedByMongoIndex_returnsConflict() {
        String inquiryDocsId = "inquiry-456";
        Inquiry inquiry = Inquiry.builder()
                .id(inquiryDocsId)
                .schoolId("school-123")
                .studentName("John Doe")
                .build();
        when(inquiryService.getInquiryById(inquiryDocsId)).thenReturn(inquiry);
        when(admissionRepository.save(any(Admission.class)))
                .thenThrow(new DuplicateKeyException("duplicate inquiryDocsId"));

        ConflictException ex = assertThrows(ConflictException.class, () -> admissionService.createAdmission(
                Admission.builder().inquiryDocsId(inquiryDocsId).build()));

        assertEquals("An admission already exists for inquiryDocsId: " + inquiryDocsId, ex.getMessage());
        verify(inquiryService, never()).linkAdmission(any(), any());
    }

    @Test
    void createAdmission_inquiryFromDifferentRequestedSchool_rejected() {
        String inquiryDocsId = "inquiry-456";
        Inquiry inquiry = Inquiry.builder()
                .id(inquiryDocsId)
                .schoolId("school-123")
                .studentName("John Doe")
                .build();
        when(inquiryService.getInquiryById(inquiryDocsId)).thenReturn(inquiry);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> admissionService.createAdmission(
                Admission.builder()
                        .schoolId("different-school")
                        .inquiryDocsId(inquiryDocsId)
                        .build()));

        assertEquals("Inquiry does not belong to the requested school.", ex.getMessage());
        verify(admissionRepository, never()).save(any());
    }

    @Test
    void getAdmissionByAdmissionNo_normalizesLookupValue() {
        Admission admission = Admission.builder()
                .id("admission-789")
                .admissionNo("ADM-2026-0001")
                .build();
        when(admissionRepository.findByAdmissionNo("ADM-2026-0001"))
                .thenReturn(Optional.of(admission));

        Admission found = admissionService.getAdmissionByAdmissionNo("  ADM-2026-0001  ");

        assertEquals("admission-789", found.getId());
    }

    @Test
    void convertToStudent_idOnly_copiesAdmissionSnapshotAndConfirmsInquiry() {
        LocalDate dob = LocalDate.of(2014, 8, 22);
        Admission admission = Admission.builder()
                .id("admission-789")
                .inquiryDocsId("inquiry-456")
                .schoolId("school-123")
                .admissionNo("ADM-2026-0001")
                .studentName("Aarav Nair")
                .dob(dob)
                .gender(Gender.MALE)
                .status(AdmissionStatus.APPROVED)
                .documents(List.of("birth-certificate.pdf"))
                .guardians(List.of(guardian(
                        "Meera Nair", GuardianRelation.MOTHER,
                        "+61-400-111-222", "meera@example.com", "Old Address")))
                .build();
        GuardianLink link = GuardianLink.builder()
                .guardianDocsId("guardian-meera")
                .primary(true)
                .build();
        Student savedStudent = Student.builder().id("student-456").build();
        StudentResponse response = StudentResponse.builder()
                .id("student-456")
                .admissionDocsId("admission-789")
                .build();
        when(admissionRepository.findById("admission-789")).thenReturn(Optional.of(admission));
        when(guardianService.buildDedupedLinks(eq("school-123"), anyList(), anyList()))
                .thenReturn(List.of(link));
        when(studentService.persistStudent(any(Student.class), isNull())).thenReturn(savedStudent);
        when(studentService.buildResponse(savedStudent)).thenReturn(response);

        StudentResponse converted = admissionService.convertToStudent(
                "admission-789", new ConvertAdmissionRequest());

        ArgumentCaptor<Student> studentCaptor = ArgumentCaptor.forClass(Student.class);
        verify(studentService).persistStudent(studentCaptor.capture(), isNull());
        Student payload = studentCaptor.getValue();
        assertEquals("school-123", payload.getSchoolId());
        assertEquals("ADM-2026-0001", payload.getAdmissionNo());
        assertEquals("admission-789", payload.getAdmissionDocsId());
        assertEquals("Aarav Nair", payload.getName());
        assertEquals(dob, payload.getDob());
        assertEquals(Gender.MALE, payload.getGender());
        assertEquals(List.of("birth-certificate.pdf"), payload.getDocuments());
        assertEquals(List.of(link), payload.getGuardians());
        assertEquals("student-456", converted.getId());
        assertEquals("student-456", admission.getStudentDocsId());
        assertEquals(AdmissionStatus.CONFIRMED, admission.getStatus());
        verify(inquiryService).confirmEnrollment("inquiry-456", "admission-789");
    }

    @Test
    void convertToStudent_overridesAdmissionDataMergesGuardiansAndCreatesAcademicRecord() {
        Admission admission = Admission.builder()
                .id("admission-789")
                .schoolId("school-123")
                .admissionNo("ADM-2026-0001")
                .studentName("Incorrect Name")
                .dob(LocalDate.of(2014, 1, 1))
                .gender(Gender.MALE)
                .status(AdmissionStatus.APPROVED)
                .documents(List.of("old-document.pdf"))
                .guardians(List.of(guardian(
                        "Meera Nair", GuardianRelation.MOTHER,
                        "+61-400-111-222", "meera@example.com", "Old Address")))
                .build();
        ConvertAdmissionRequest request = new ConvertAdmissionRequest();
        request.setName("Corrected Name");
        request.setDob(LocalDate.of(2014, 8, 22));
        request.setDocuments(List.of("corrected-document.pdf"));
        AcademicRecordRequest academicRecord = new AcademicRecordRequest();
        academicRecord.setAcademicYear("2026-2027");
        academicRecord.setClassDocsId("class-9");
        academicRecord.setRollNo("12");
        request.setCurrentAcademicRecord(academicRecord);
        StudentGuardianRequest existingGuardian = StudentGuardianRequest.builder()
                .guardianDocsId("guardian-existing")
                .relation(GuardianRelation.FATHER)
                .portalAccess(true)
                .build();
        StudentGuardianRequest correctedMother = StudentGuardianRequest.builder()
                .name("Meera Nair")
                .email("meera@example.com")
                .address("12 New Street")
                .relation(GuardianRelation.MOTHER)
                .primary(true)
                .build();
        request.setGuardians(List.of(existingGuardian, correctedMother));

        Student savedStudent = Student.builder().id("student-456").build();
        when(admissionRepository.findById("admission-789")).thenReturn(Optional.of(admission));
        when(guardianService.buildDedupedLinks(eq("school-123"), anyList(), anyList()))
                .thenReturn(List.of(
                        GuardianLink.builder().guardianDocsId("guardian-existing").build(),
                        GuardianLink.builder().guardianDocsId("guardian-meera").build()));
        when(studentService.persistStudent(any(Student.class), any(StudentAcademicRecord.class)))
                .thenReturn(savedStudent);
        when(studentService.buildResponse(savedStudent))
                .thenReturn(StudentResponse.builder().id("student-456").build());

        admissionService.convertToStudent("admission-789", request);

        ArgumentCaptor<Student> studentCaptor = ArgumentCaptor.forClass(Student.class);
        ArgumentCaptor<StudentAcademicRecord> academicCaptor =
                ArgumentCaptor.forClass(StudentAcademicRecord.class);
        verify(studentService).persistStudent(studentCaptor.capture(), academicCaptor.capture());
        assertEquals("Corrected Name", studentCaptor.getValue().getName());
        assertEquals(LocalDate.of(2014, 8, 22), studentCaptor.getValue().getDob());
        assertEquals(List.of("corrected-document.pdf"), studentCaptor.getValue().getDocuments());
        assertEquals("2026-2027", academicCaptor.getValue().getAcademicYear());
        assertEquals("class-9", academicCaptor.getValue().getClassDocsId());
        assertEquals("12", academicCaptor.getValue().getRollNo());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<GuardianService.GuardianDraft>> draftsCaptor =
                ArgumentCaptor.forClass(List.class);
        verify(guardianService).buildDedupedLinks(
                eq("school-123"), anyList(), draftsCaptor.capture());
        List<GuardianService.GuardianDraft> drafts = draftsCaptor.getValue();
        assertEquals("guardian-existing", drafts.get(0).guardianDocsId());
        assertEquals("Meera Nair", drafts.get(1).name());
        assertEquals("+61-400-111-222", drafts.get(1).phone());
        assertEquals("12 New Street", drafts.get(1).address());
        assertEquals(Boolean.TRUE, drafts.get(1).primary());
    }

    @Test
    void convertToStudent_alreadyConverted_returnsConflictWithoutWriting() {
        Admission admission = Admission.builder()
                .id("admission-789")
                .studentDocsId("student-existing")
                .build();
        when(admissionRepository.findById("admission-789")).thenReturn(Optional.of(admission));

        ConflictException error = assertThrows(
                ConflictException.class,
                () -> admissionService.convertToStudent(
                        "admission-789", new ConvertAdmissionRequest()));

        assertEquals(
                "This admission has already been converted to student student-existing.",
                error.getMessage());
        verifyNoInteractions(studentService, guardianService);
        verify(admissionRepository, never()).save(any());
    }

    @Test
    void convertToStudent_rejectedAdmission_isRejected() {
        Admission admission = Admission.builder()
                .id("admission-789")
                .status(AdmissionStatus.REJECTED)
                .build();
        when(admissionRepository.findById("admission-789")).thenReturn(Optional.of(admission));

        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> admissionService.convertToStudent(
                        "admission-789", new ConvertAdmissionRequest()));

        assertEquals("A rejected admission cannot be converted to a student.", error.getMessage());
        verifyNoInteractions(studentService, guardianService);
    }

    private void saveWithGeneratedId() {
        when(admissionRepository.save(any(Admission.class))).thenAnswer(invocation -> {
            Admission admission = invocation.getArgument(0);
            admission.setId("admission-789");
            return admission;
        });
    }

    private InquiryGuardian guardian(String name, GuardianRelation relation, String phone,
                                     String email, String address) {
        return InquiryGuardian.builder()
                .name(name)
                .relation(relation)
                .phone(phone)
                .email(email)
                .address(address)
                .build();
    }
}
