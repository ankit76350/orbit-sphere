package com.orbitastra.backend.services.crm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;

import com.orbitastra.backend.dto.student.StudentResponse;
import com.orbitastra.backend.exceptions.ConflictException;
import com.orbitastra.backend.exceptions.ResourceNotFoundException;
import com.orbitastra.backend.models.crm.Admission;
import com.orbitastra.backend.models.crm.Inquiry;
import com.orbitastra.backend.models.crm.InquiryGuardian;
import com.orbitastra.backend.models.crm.enums.AdmissionStatus;
import com.orbitastra.backend.models.student.Student;
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
        String inquiryId = "inquiry-456";
        InquiryGuardian guardian = guardian("Parent One", GuardianRelation.MOTHER,
                "0400111222", "parent@example.com", "Old Address");
        Inquiry inquiry = Inquiry.builder()
                .id(inquiryId)
                .schoolId("school-123")
                .studentName("Inquiry Applicant")
                .guardians(List.of(guardian))
                .build();
        when(inquiryService.getInquiryById(inquiryId)).thenReturn(inquiry);
        when(admissionRepository.existsByInquiryDocsId(inquiryId)).thenReturn(false);
        saveWithGeneratedId();

        Admission saved = admissionService.createAdmission(
                Admission.builder().inquiryDocsId(inquiryId).build());

        assertEquals("school-123", saved.getSchoolId());
        assertEquals("Inquiry Applicant", saved.getStudentName());
        assertEquals(inquiryId, saved.getInquiryDocsId());
        assertEquals(1, saved.getGuardians().size());
        assertEquals("parent@example.com", saved.getGuardians().get(0).getEmail());
        assertFalse(saved.getGuardians().get(0) == guardian, "The admission must own its snapshot copy");
        verify(inquiryService).linkAdmission(inquiryId, "admission-789");
    }

    @Test
    void createAdmission_inquiryWithOverrides_mergesApplicantAndGuardianDetails() {
        String inquiryId = "inquiry-456";
        InquiryGuardian existingMother = guardian("Meera Nair", GuardianRelation.MOTHER,
                "+61-400-111-222", "meera@example.com", "Old Address");
        Inquiry inquiry = Inquiry.builder()
                .id(inquiryId)
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
                .inquiryDocsId(inquiryId)
                .studentName("Aarav Kumar Nair")
                .dob(dob)
                .gender(Gender.MALE)
                .documents(List.of("birth-certificate.pdf"))
                .guardians(List.of(motherOverride, additionalFather))
                .build();
        when(inquiryService.getInquiryById(inquiryId)).thenReturn(inquiry);
        when(admissionRepository.existsByInquiryDocsId(inquiryId)).thenReturn(false);
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
        String inquiryId = "inquiry-456";
        Inquiry inquiry = Inquiry.builder()
                .id(inquiryId)
                .schoolId("school-123")
                .guardians(List.of(guardian("Parent One", GuardianRelation.MOTHER,
                        "0400111222", null, null)))
                .build();
        when(inquiryService.getInquiryById(inquiryId)).thenReturn(inquiry);
        saveWithGeneratedId();

        Admission saved = admissionService.createAdmission(
                Admission.builder().inquiryDocsId(inquiryId).build());

        assertNull(saved.getStudentName());
        assertEquals(1, saved.getGuardians().size());
        verify(inquiryService).linkAdmission(inquiryId, "admission-789");
    }

    @Test
    void createAdmission_missingInquiry_propagatesNotFoundAndDoesNotSave() {
        String inquiryId = "missing-inquiry";
        when(inquiryService.getInquiryById(inquiryId))
                .thenThrow(new ResourceNotFoundException("Inquiry not found with id: " + inquiryId));

        assertThrows(ResourceNotFoundException.class, () -> admissionService.createAdmission(
                Admission.builder().inquiryDocsId(inquiryId).build()));

        verify(admissionRepository, never()).save(any());
    }

    @Test
    void createAdmission_duplicateInquiry_returnsConflict() {
        String inquiryId = "inquiry-456";
        Inquiry inquiry = Inquiry.builder()
                .id(inquiryId)
                .schoolId("school-123")
                .studentName("John Doe")
                .build();
        when(inquiryService.getInquiryById(inquiryId)).thenReturn(inquiry);
        when(admissionRepository.existsByInquiryDocsId(inquiryId)).thenReturn(true);

        ConflictException ex = assertThrows(ConflictException.class, () -> admissionService.createAdmission(
                Admission.builder().inquiryDocsId(inquiryId).build()));

        assertEquals("An admission already exists for inquiryDocsId: " + inquiryId, ex.getMessage());
        verify(admissionRepository, never()).save(any());
    }

    @Test
    void createAdmission_concurrentDuplicateDetectedByMongoIndex_returnsConflict() {
        String inquiryId = "inquiry-456";
        Inquiry inquiry = Inquiry.builder()
                .id(inquiryId)
                .schoolId("school-123")
                .studentName("John Doe")
                .build();
        when(inquiryService.getInquiryById(inquiryId)).thenReturn(inquiry);
        when(admissionRepository.save(any(Admission.class)))
                .thenThrow(new DuplicateKeyException("duplicate inquiryDocsId"));

        ConflictException ex = assertThrows(ConflictException.class, () -> admissionService.createAdmission(
                Admission.builder().inquiryDocsId(inquiryId).build()));

        assertEquals("An admission already exists for inquiryDocsId: " + inquiryId, ex.getMessage());
        verify(inquiryService, never()).linkAdmission(any(), any());
    }

    @Test
    void createAdmission_inquiryFromDifferentRequestedSchool_rejected() {
        String inquiryId = "inquiry-456";
        Inquiry inquiry = Inquiry.builder()
                .id(inquiryId)
                .schoolId("school-123")
                .studentName("John Doe")
                .build();
        when(inquiryService.getInquiryById(inquiryId)).thenReturn(inquiry);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> admissionService.createAdmission(
                Admission.builder()
                        .schoolId("different-school")
                        .inquiryDocsId(inquiryId)
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
    void convertToStudent_copiesAdmissionNoFromAdmission() {
        Admission admission = Admission.builder()
                .id("admission-789")
                .schoolId("school-123")
                .admissionNo("ADM-2026-0001")
                .studentName("Applicant")
                .status(AdmissionStatus.APPROVED)
                .guardians(List.of())
                .build();
        Student studentPayload = Student.builder().build();
        Student savedStudent = Student.builder()
                .id("student-456")
                .schoolId("school-123")
                .admissionNo("ADM-2026-0001")
                .build();
        StudentResponse response = StudentResponse.builder()
                .id("student-456")
                .admissionNo("ADM-2026-0001")
                .build();
        when(admissionRepository.findById("admission-789")).thenReturn(Optional.of(admission));
        when(studentService.persistStudent(any(Student.class), any())).thenReturn(savedStudent);
        when(studentService.buildResponse(savedStudent)).thenReturn(response);

        StudentResponse converted = admissionService.convertToStudent(
                "admission-789", studentPayload, null);

        assertEquals("ADM-2026-0001", studentPayload.getAdmissionNo());
        assertEquals("ADM-2026-0001", converted.getAdmissionNo());
        assertEquals("student-456", admission.getStudentDocsId());
        assertEquals(AdmissionStatus.CONFIRMED, admission.getStatus());
        verify(admissionRepository).save(admission);
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
