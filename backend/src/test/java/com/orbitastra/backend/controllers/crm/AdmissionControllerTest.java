package com.orbitastra.backend.controllers.crm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.orbitastra.backend.dto.crm.ConvertAdmissionRequest;
import com.orbitastra.backend.dto.student.StudentResponse;
import com.orbitastra.backend.services.crm.AdmissionService;

@ExtendWith(MockitoExtension.class)
class AdmissionControllerTest {

    @Mock
    private AdmissionService admissionService;

    @InjectMocks
    private AdmissionController admissionController;

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
