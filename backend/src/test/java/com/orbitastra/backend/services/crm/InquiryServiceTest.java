package com.orbitastra.backend.services.crm;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.orbitastra.backend.exceptions.ConflictException;
import com.orbitastra.backend.models.crm.Admission;
import com.orbitastra.backend.models.crm.Inquiry;
import com.orbitastra.backend.models.crm.embedded.InquiryFollowUp;
import com.orbitastra.backend.models.crm.enums.InquiryStatus;
import com.orbitastra.backend.models.staff.Staff;
import com.orbitastra.backend.repositories.crm.AdmissionRepository;
import com.orbitastra.backend.repositories.crm.InquiryRepository;
import com.orbitastra.backend.repositories.staff.StaffRepository;

@ExtendWith(MockitoExtension.class)
class InquiryServiceTest {

    @Mock
    private InquiryRepository inquiryRepository;

    @Mock
    private StaffRepository staffRepository;

    @Mock
    private AdmissionRepository admissionRepository;

    @InjectMocks
    private InquiryService inquiryService;

    @Test
    void createInquiry_withAdmittedStatus_createsAndLinksAdmission() {
        Inquiry inquiry = Inquiry.builder()
                .schoolId("school-123")
                .studentName("Alice Smith")
                .status(InquiryStatus.ADMITTED)
                .build();
        when(inquiryRepository.save(any(Inquiry.class))).thenAnswer(invocation -> {
            Inquiry saved = invocation.getArgument(0);
            if (saved.getId() == null) {
                saved.setId("inquiry-123");
            }
            return saved;
        });
        when(admissionRepository.findByInquiryDocsId("inquiry-123")).thenReturn(List.of());
        when(admissionRepository.save(any(Admission.class))).thenAnswer(invocation -> {
            Admission saved = invocation.getArgument(0);
            saved.setId("admission-123");
            return saved;
        });

        Inquiry created = inquiryService.createInquiry(inquiry);

        assertEquals(InquiryStatus.ADMITTED, created.getStatus());
        assertEquals("admission-123", created.getAdmissionDocsId());
        assertNotNull(created.getCreatedAt());
        assertNotNull(created.getUpdatedAt());
        verify(inquiryRepository, times(2)).save(inquiry);

        ArgumentCaptor<Admission> admissionCaptor = ArgumentCaptor.forClass(Admission.class);
        verify(admissionRepository).save(admissionCaptor.capture());
        Admission admission = admissionCaptor.getValue();
        assertEquals("inquiry-123", admission.getInquiryDocsId());
        assertEquals("school-123", admission.getSchoolId());
        assertEquals("Alice Smith", admission.getStudentName());
        assertNotNull(admission.getAdmissionNo());
    }

    @Test
    void createInquiry_withInitialAdmittedFollowUp_usesEffectiveStatusAndCreatesAdmission() {
        Inquiry inquiry = Inquiry.builder()
                .schoolId("school-123")
                .status(InquiryStatus.INQUIRY)
                .followUps(List.of(InquiryFollowUp.builder()
                        .status(InquiryStatus.ADMITTED)
                        .note("Application submitted")
                        .build()))
                .build();
        when(inquiryRepository.save(any(Inquiry.class))).thenAnswer(invocation -> {
            Inquiry saved = invocation.getArgument(0);
            saved.setId("inquiry-123");
            return saved;
        });
        when(admissionRepository.findByInquiryDocsId("inquiry-123")).thenReturn(List.of());
        when(admissionRepository.save(any(Admission.class)))
                .thenReturn(Admission.builder().id("admission-123").build());

        Inquiry created = inquiryService.createInquiry(inquiry);

        assertEquals(InquiryStatus.ADMITTED, created.getStatus());
        assertEquals("admission-123", created.getAdmissionDocsId());
        assertEquals(InquiryStatus.ADMITTED, created.getFollowUps().get(0).getStatus());
    }

    @Test
    void createInquiry_withoutAdmittedStatus_doesNotCreateAdmission() {
        Inquiry inquiry = Inquiry.builder()
                .schoolId("school-123")
                .studentName("Alice Smith")
                .status(InquiryStatus.INQUIRY)
                .build();
        when(inquiryRepository.save(inquiry)).thenAnswer(invocation -> {
            Inquiry saved = invocation.getArgument(0);
            saved.setId("inquiry-123");
            return saved;
        });

        Inquiry created = inquiryService.createInquiry(inquiry);

        assertEquals(InquiryStatus.INQUIRY, created.getStatus());
        verify(inquiryRepository).save(inquiry);
        verify(admissionRepository, never()).findByInquiryDocsId(any());
        verify(admissionRepository, never()).save(any());
    }

    @Test
    void recordFollowUp_withPastNextFollowUp_throwsIllegalArgumentException() {
        String inquiryDocsId = "6a5e1bb4faffc52a626a30af";
        Inquiry existing = Inquiry.builder()
                .id(inquiryDocsId)
                .schoolId("school-123")
                .status(InquiryStatus.INQUIRY)
                .build();

        when(inquiryRepository.findById(inquiryDocsId)).thenReturn(Optional.of(existing));

        InquiryFollowUp entry = InquiryFollowUp.builder()
                .status(InquiryStatus.COUNSELING)
                .note("Follow-up with parent")
                .nextFollowUp(LocalDate.now().minusDays(1)) // Past date
                .build();

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            inquiryService.recordFollowUp(inquiryDocsId, entry);
        });

        assertEquals("Next follow-up date cannot be in the past.", ex.getMessage());
    }

    @Test
    void recordFollowUp_withFutureNextFollowUp_succeeds() {
        String inquiryDocsId = "6a5e1bb4faffc52a626a30af";
        Inquiry existing = Inquiry.builder()
                .id(inquiryDocsId)
                .schoolId("school-123")
                .status(InquiryStatus.INQUIRY)
                .build();

        when(inquiryRepository.findById(inquiryDocsId)).thenReturn(Optional.of(existing));
        when(inquiryRepository.save(existing)).thenReturn(existing);

        InquiryFollowUp entry = InquiryFollowUp.builder()
                .status(InquiryStatus.COUNSELING)
                .note("Scheduled campus visit")
                .nextFollowUp(LocalDate.now().plusDays(5)) // Future date
                .build();

        Inquiry result = inquiryService.recordFollowUp(inquiryDocsId, entry);
        assertEquals(InquiryStatus.COUNSELING, result.getStatus());
    }

    @Test
    void recordFollowUp_withoutHandler_inheritsTopLevelCounselorDocsId() {
        String inquiryDocsId = "6a5e1bb4faffc52a626a30af";
        Inquiry existing = Inquiry.builder()
                .id(inquiryDocsId)
                .schoolId("school-123")
                .counselorDocsId("staff-456")
                .status(InquiryStatus.INQUIRY)
                .build();
        Staff counselor = Staff.builder()
                .id("staff-456")
                .schoolId("school-123")
                .build();
        when(inquiryRepository.findById(inquiryDocsId)).thenReturn(Optional.of(existing));
        when(inquiryRepository.save(existing)).thenReturn(existing);
        when(staffRepository.findById("staff-456")).thenReturn(Optional.of(counselor));

        InquiryFollowUp entry = InquiryFollowUp.builder()
                .status(InquiryStatus.COUNSELING)
                .build();

        Inquiry result = inquiryService.recordFollowUp(inquiryDocsId, entry);

        assertEquals("staff-456", entry.getCounselorDocsId());
        assertEquals("staff-456", result.getCounselorDocsId());
    }

    @Test
    void recordFollowUp_whenAlreadyAdmitted_changingStatus_throwsIllegalArgumentException() {
        String inquiryDocsId = "6a5e1bb4faffc52a626a30af";
        Inquiry existing = Inquiry.builder()
                .id(inquiryDocsId)
                .schoolId("school-123")
                .status(InquiryStatus.ADMITTED)
                .build();

        when(inquiryRepository.findById(inquiryDocsId)).thenReturn(Optional.of(existing));

        InquiryFollowUp entry = InquiryFollowUp.builder()
                .status(InquiryStatus.COUNSELING) // Trying to revert back to COUNSELING from ADMITTED
                .note("Reverting status")
                .build();

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            inquiryService.recordFollowUp(inquiryDocsId, entry);
        });

        assertEquals("Cannot change status of an inquiry that is already ADMITTED.", ex.getMessage());
    }

    @Test
    void recordFollowUp_whenAlreadyAdmitted_sameStatus_succeeds() {
        String inquiryDocsId = "6a5e1bb4faffc52a626a30af";
        Inquiry existing = Inquiry.builder()
                .id(inquiryDocsId)
                .schoolId("school-123")
                .status(InquiryStatus.ADMITTED)
                .build();

        when(inquiryRepository.findById(inquiryDocsId)).thenReturn(Optional.of(existing));
        when(inquiryRepository.save(existing)).thenReturn(existing);

        Admission existingAdmission = Admission.builder().id("admission-777").build();
        when(admissionRepository.findByInquiryDocsId(inquiryDocsId)).thenReturn(java.util.List.of(existingAdmission));

        InquiryFollowUp entry = InquiryFollowUp.builder()
                .status(InquiryStatus.ADMITTED) // Remaining ADMITTED
                .note("Added additional documentation note")
                .build();

        Inquiry result = inquiryService.recordFollowUp(inquiryDocsId, entry);
        assertEquals(InquiryStatus.ADMITTED, result.getStatus());
        assertEquals("admission-777", result.getAdmissionDocsId());
    }

    @Test
    void recordFollowUp_transitionToAdmitted_createsAdmissionAndLinksId() {
        String inquiryDocsId = "6a5e1bb4faffc52a626a30af";
        Inquiry existing = Inquiry.builder()
                .id(inquiryDocsId)
                .schoolId("school-123")
                .status(InquiryStatus.INQUIRY)
                .studentName("Alice Smith")
                .build();

        when(inquiryRepository.findById(inquiryDocsId)).thenReturn(Optional.of(existing));
        when(inquiryRepository.save(existing)).thenReturn(existing);

        Admission mockAdmission = Admission.builder().id("admission-999").build();
        when(admissionRepository.findByInquiryDocsId(inquiryDocsId)).thenReturn(java.util.Collections.emptyList());
        when(admissionRepository.save(org.mockito.ArgumentMatchers.any(Admission.class))).thenReturn(mockAdmission);

        InquiryFollowUp entry = InquiryFollowUp.builder()
                .status(InquiryStatus.ADMITTED)
                .note("Inquiry converted to admission")
                .build();

        Inquiry result = inquiryService.recordFollowUp(inquiryDocsId, entry);
        assertEquals(InquiryStatus.ADMITTED, result.getStatus());
        assertEquals("admission-999", result.getAdmissionDocsId());
    }

    @Test
    void linkAdmission_updatesStatusAndReferenceTogether() {
        String inquiryDocsId = "6a5e1bb4faffc52a626a30af";
        Inquiry existing = Inquiry.builder()
                .id(inquiryDocsId)
                .schoolId("school-123")
                .status(InquiryStatus.COUNSELING)
                .build();
        when(inquiryRepository.findById(inquiryDocsId)).thenReturn(Optional.of(existing));
        when(inquiryRepository.save(existing)).thenReturn(existing);

        Inquiry result = inquiryService.linkAdmission(inquiryDocsId, "admission-123");

        assertEquals(InquiryStatus.ADMITTED, result.getStatus());
        assertEquals("admission-123", result.getAdmissionDocsId());
        assertEquals(1, result.getFollowUps().size());
        assertEquals(InquiryStatus.ADMITTED, result.getFollowUps().get(0).getStatus());
    }

    @Test
    void linkAdmission_whenAlreadyAdmitted_repairsMissingReferenceWithoutDuplicateFollowUp() {
        String inquiryDocsId = "6a5e1bb4faffc52a626a30af";
        Inquiry existing = Inquiry.builder()
                .id(inquiryDocsId)
                .schoolId("school-123")
                .status(InquiryStatus.ADMITTED)
                .build();
        when(inquiryRepository.findById(inquiryDocsId)).thenReturn(Optional.of(existing));
        when(inquiryRepository.save(existing)).thenReturn(existing);

        Inquiry result = inquiryService.linkAdmission(inquiryDocsId, "admission-123");

        assertEquals("admission-123", result.getAdmissionDocsId());
        assertEquals(0, result.getFollowUps().size());
    }

    @Test
    void linkAdmission_whenLinkedToDifferentAdmission_returnsConflict() {
        String inquiryDocsId = "6a5e1bb4faffc52a626a30af";
        Inquiry existing = Inquiry.builder()
                .id(inquiryDocsId)
                .schoolId("school-123")
                .status(InquiryStatus.ADMITTED)
                .admissionDocsId("existing-admission")
                .build();
        when(inquiryRepository.findById(inquiryDocsId)).thenReturn(Optional.of(existing));

        ConflictException ex = assertThrows(
                ConflictException.class,
                () -> inquiryService.linkAdmission(inquiryDocsId, "different-admission"));

        assertEquals("Inquiry " + inquiryDocsId
                + " is already linked to admission existing-admission.", ex.getMessage());
    }

    @Test
    void confirmEnrollment_marksLinkedInquiryConfirmedAndRecordsTimelineEntry() {
        String inquiryDocsId = "inquiry-123";
        Inquiry existing = Inquiry.builder()
                .id(inquiryDocsId)
                .schoolId("school-123")
                .status(InquiryStatus.ADMITTED)
                .admissionDocsId("admission-123")
                .build();
        when(inquiryRepository.findById(inquiryDocsId)).thenReturn(Optional.of(existing));
        when(inquiryRepository.save(existing)).thenReturn(existing);

        Inquiry result = inquiryService.confirmEnrollment(inquiryDocsId, "admission-123");

        assertEquals(InquiryStatus.CONFIRMED, result.getStatus());
        assertEquals("admission-123", result.getAdmissionDocsId());
        assertEquals(1, result.getFollowUps().size());
        assertEquals(InquiryStatus.CONFIRMED, result.getFollowUps().get(0).getStatus());
    }

    @Test
    void confirmEnrollment_forLostInquiry_isRejected() {
        Inquiry existing = Inquiry.builder()
                .id("inquiry-123")
                .status(InquiryStatus.LOST)
                .admissionDocsId("admission-123")
                .build();
        when(inquiryRepository.findById("inquiry-123")).thenReturn(Optional.of(existing));

        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> inquiryService.confirmEnrollment("inquiry-123", "admission-123"));

        assertEquals("A lost inquiry cannot be confirmed as enrolled.", error.getMessage());
    }
}
