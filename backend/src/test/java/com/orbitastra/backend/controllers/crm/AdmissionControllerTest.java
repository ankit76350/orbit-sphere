package com.orbitastra.backend.controllers.crm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.orbitastra.backend.dto.crm.AddAdmissionDocumentRequest;
import com.orbitastra.backend.dto.crm.ConvertAdmissionRequest;
import com.orbitastra.backend.dto.crm.InquiryGuardianRequest;
import com.orbitastra.backend.dto.student.StudentResponse;
import com.orbitastra.backend.models.crm.Admission;
import com.orbitastra.backend.models.crm.embedded.InquiryGuardian;
import com.orbitastra.backend.models.student.enums.GuardianRelation;
import com.orbitastra.backend.services.crm.AdmissionService;

@ExtendWith(MockitoExtension.class)
class AdmissionControllerTest {

    @Mock
    private AdmissionService admissionService;

    @InjectMocks
    private AdmissionController admissionController;

    @Test
    void addGuardian_mapsValidatedRequestAndReturnsUpdatedAdmission() {
        InquiryGuardianRequest request = new InquiryGuardianRequest();
        request.setName("Meera Nair");
        request.setRelation(GuardianRelation.MOTHER);
        request.setEmail("meera@example.com");
        Admission updated = Admission.builder().id("admission-789").build();
        when(admissionService.addGuardian(eq("admission-789"), any(InquiryGuardian.class)))
                .thenReturn(updated);

        ResponseEntity<Admission> response =
                admissionController.addGuardian("admission-789", request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("admission-789", response.getBody().getId());
        ArgumentCaptor<InquiryGuardian> guardianCaptor =
                ArgumentCaptor.forClass(InquiryGuardian.class);
        verify(admissionService).addGuardian(eq("admission-789"), guardianCaptor.capture());
        assertEquals("Meera Nair", guardianCaptor.getValue().getName());
        assertEquals(GuardianRelation.MOTHER, guardianCaptor.getValue().getRelation());
        assertEquals("meera@example.com", guardianCaptor.getValue().getEmail());
    }

    @Test
    void addDocument_mapsRequestAndReturnsUpdatedAdmission() {
        AddAdmissionDocumentRequest request = new AddAdmissionDocumentRequest();
        request.setDocument("birth-certificate.pdf");
        Admission updated = Admission.builder()
                .id("admission-789")
                .documents(java.util.List.of("birth-certificate.pdf"))
                .build();
        when(admissionService.addDocument("admission-789", "birth-certificate.pdf"))
                .thenReturn(updated);

        ResponseEntity<Admission> response =
                admissionController.addDocument("admission-789", request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(java.util.List.of("birth-certificate.pdf"), response.getBody().getDocuments());
    }

    @Test
    void createStudentFromAdmission_withIdOnly_returnsCreated() {
        StudentResponse created = StudentResponse.builder()
                .id("student-123")
                .admissionDocsId("admission-789")
                .build();
        when(admissionService.convertToStudent("admission-789", null)).thenReturn(created);

        ResponseEntity<StudentResponse> response =
                admissionController.createStudentFromAdmission("admission-789", null);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("student-123", response.getBody().getId());
    }

    @Test
    void createStudentFromAdmission_withOverrides_returnsCreated() {
        ConvertAdmissionRequest request = new ConvertAdmissionRequest();
        request.setName("Corrected Name");
        StudentResponse created = StudentResponse.builder()
                .id("student-123")
                .name("Corrected Name")
                .build();
        when(admissionService.convertToStudent("admission-789", request)).thenReturn(created);

        ResponseEntity<StudentResponse> response =
                admissionController.createStudentFromAdmission("admission-789", request);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals("Corrected Name", response.getBody().getName());
    }
}
