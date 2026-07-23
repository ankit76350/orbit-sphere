package com.orbitastra.backend.controllers.student;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.orbitastra.backend.dto.student.CreateStudentRequest;
import com.orbitastra.backend.dto.student.StudentResponse;
import com.orbitastra.backend.services.student.StudentService;

@ExtendWith(MockitoExtension.class)
class StudentControllerTest {

    @Mock
    private StudentService studentService;

    @InjectMocks
    private StudentController studentController;

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
