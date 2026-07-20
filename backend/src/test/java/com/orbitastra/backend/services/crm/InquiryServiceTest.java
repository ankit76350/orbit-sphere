package com.orbitastra.backend.services.crm;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.orbitastra.backend.models.crm.Inquiry;
import com.orbitastra.backend.models.crm.InquiryFollowUp;
import com.orbitastra.backend.models.crm.enums.InquiryStatus;
import com.orbitastra.backend.repositories.crm.InquiryRepository;
import com.orbitastra.backend.repositories.staff.StaffRepository;

@ExtendWith(MockitoExtension.class)
class InquiryServiceTest {

    @Mock
    private InquiryRepository inquiryRepository;

    @Mock
    private StaffRepository staffRepository;

    @InjectMocks
    private InquiryService inquiryService;

    @Test
    void recordFollowUp_withPastNextFollowUp_throwsIllegalArgumentException() {
        String inquiryId = "6a5e1bb4faffc52a626a30af";
        Inquiry existing = Inquiry.builder()
                .id(inquiryId)
                .schoolId("school-123")
                .status(InquiryStatus.INQUIRY)
                .build();

        when(inquiryRepository.findById(inquiryId)).thenReturn(Optional.of(existing));

        InquiryFollowUp entry = InquiryFollowUp.builder()
                .status(InquiryStatus.COUNSELING)
                .note("Follow-up with parent")
                .nextFollowUp(LocalDate.now().minusDays(1)) // Past date
                .build();

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            inquiryService.recordFollowUp(inquiryId, entry);
        });

        assertEquals("Next follow-up date cannot be in the past.", ex.getMessage());
    }

    @Test
    void recordFollowUp_withFutureNextFollowUp_succeeds() {
        String inquiryId = "6a5e1bb4faffc52a626a30af";
        Inquiry existing = Inquiry.builder()
                .id(inquiryId)
                .schoolId("school-123")
                .status(InquiryStatus.INQUIRY)
                .build();

        when(inquiryRepository.findById(inquiryId)).thenReturn(Optional.of(existing));
        when(inquiryRepository.save(existing)).thenReturn(existing);

        InquiryFollowUp entry = InquiryFollowUp.builder()
                .status(InquiryStatus.COUNSELING)
                .note("Scheduled campus visit")
                .nextFollowUp(LocalDate.now().plusDays(5)) // Future date
                .build();

        Inquiry result = inquiryService.recordFollowUp(inquiryId, entry);
        assertEquals(InquiryStatus.COUNSELING, result.getStatus());
    }
}
