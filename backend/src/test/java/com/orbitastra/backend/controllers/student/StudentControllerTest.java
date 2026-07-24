package com.orbitastra.backend.controllers.student;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.orbitastra.backend.dto.student.AcademicRecordRequest;
import com.orbitastra.backend.dto.student.CreateStudentRequest;
import com.orbitastra.backend.dto.student.StudentResponse;
import com.orbitastra.backend.services.student.StudentService;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;

@ExtendWith(MockitoExtension.class)
class StudentControllerTest {

    @Mock
    private StudentService studentService;

    @InjectMocks
    private StudentController studentController;

    @Test
    void getStudentByAdmissionNoQuery_preservesSlashFormattedNumber() {
        StudentResponse student = StudentResponse.builder()
                .admissionNo("ADM/2026/05/0519")
                .build();
        when(studentService.getStudentByAdmissionNo("ADM/2026/05/0519"))
                .thenReturn(student);

        ResponseEntity<StudentResponse> response =
                studentController.getStudentByAdmissionNoQuery("ADM/2026/05/0519");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("ADM/2026/05/0519", response.getBody().getAdmissionNo());
    }

    @Test
    void createStudent_normal_returnsCreated() {
        CreateStudentRequest req = new CreateStudentRequest();
        req.setSchoolId("school-1");
        req.setAdmissionNo("ADM-001");
        req.setName("John Doe");

        StudentResponse created = StudentResponse.builder().id("std-1").schoolId("school-1").name("John Doe").build();
        when(studentService.createStudent(any(CreateStudentRequest.class))).thenReturn(created);

        ResponseEntity<StudentResponse> res = studentController.createStudent(req);
        assertEquals(HttpStatus.CREATED, res.getStatusCode());
        assertNotNull(res.getBody());
        assertEquals("std-1", res.getBody().getId());
    }

    @Test
    void deleteStudent_isNotAllowedAndDoesNotCallService() {
        ResponseEntity<?> response = studentController.deleteStudent("student-id-123");

        assertEquals(HttpStatus.METHOD_NOT_ALLOWED, response.getStatusCode());
        assertEquals(
                "Student deletion is not allowed. Student and academic records must be retained.",
                ((Map<?, ?>) response.getBody()).get("message"));
        verifyNoInteractions(studentService);
    }

    @Test
    void academicRecordRequest_acceptsClassDocsId() throws Exception {
        AcademicRecordRequest request = new ObjectMapper().readValue(
                """
                {
                  "academicYear": "2027-2028",
                  "classDocsId": "class-mongo-id"
                }
                """,
                AcademicRecordRequest.class);

        assertEquals("class-mongo-id", request.getClassDocsId());
    }

    @Test
    void testValidation_whenLegacyFieldsPresent_fails() {
        jakarta.validation.Validator validator = jakarta.validation.Validation.buildDefaultValidatorFactory().getValidator();
        CreateStudentRequest req = new CreateStudentRequest();
        req.setSchoolId("school-1");
        req.setAdmissionNo("ADM-001");
        req.setName("John Doe");
        req.setAcademicYear("2026-2027");

        java.util.Set<jakarta.validation.ConstraintViolation<CreateStudentRequest>> violations = validator.validate(req);
        assertEquals(1, violations.size());
        assertEquals("Academic placement must be provided inside currentAcademicRecord; top-level fields are not supported.", 
                     violations.iterator().next().getMessage());
    }
}
