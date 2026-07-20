package com.orbitastra.backend.services.crm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.orbitastra.backend.models.core.AcademicYear;
import com.orbitastra.backend.models.crm.Admission;
import com.orbitastra.backend.models.crm.Inquiry;
import com.orbitastra.backend.repositories.crm.AdmissionRepository;
import com.orbitastra.backend.services.utils.AcademicYearResolver;

@ExtendWith(MockitoExtension.class)
class AdmissionServiceTest {

    @Mock
    private AdmissionRepository admissionRepository;

    @Mock
    private InquiryService inquiryService;

    @Mock
    private AcademicYearResolver academicYearResolver;

    @InjectMocks
    private AdmissionService admissionService;

    @Test
    void createAdmission_duplicateInquiryId_throwsIllegalArgumentException() {
        String schoolId = "school-123";
        String inquiryId = "inquiry-456";

        Admission admission = Admission.builder()
                .schoolId(schoolId)
                .inquiryId(inquiryId)
                .academicYear("2026-2027")
                .build();

        AcademicYear mockYear = AcademicYear.builder().name("2026-2027").build();
        when(academicYearResolver.resolve(schoolId, "2026-2027", null)).thenReturn(mockYear);
        when(admissionRepository.existsByInquiryId(inquiryId)).thenReturn(true);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            admissionService.createAdmission(admission);
        });

        assertEquals("An admission already exists for inquiry ID: " + inquiryId, ex.getMessage());
    }

    @Test
    void createAdmission_uniqueInquiryId_succeeds() {
        String schoolId = "school-123";
        String inquiryId = "inquiry-456";

        Admission admission = Admission.builder()
                .schoolId(schoolId)
                .inquiryId(inquiryId)
                .academicYear("2026-2027")
                .build();

        AcademicYear mockYear = AcademicYear.builder().name("2026-2027").build();
        when(academicYearResolver.resolve(schoolId, "2026-2027", null)).thenReturn(mockYear);
        when(admissionRepository.existsByInquiryId(inquiryId)).thenReturn(false);

        Inquiry mockInquiry = Inquiry.builder().id(inquiryId).schoolId(schoolId).studentName("John Doe").build();
        when(inquiryService.getInquiryById(inquiryId)).thenReturn(mockInquiry);

        when(admissionRepository.save(admission)).thenReturn(admission);

        Admission result = admissionService.createAdmission(admission);
        assertEquals("John Doe", result.getStudentName());
    }
}
