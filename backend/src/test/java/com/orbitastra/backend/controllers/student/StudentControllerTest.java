package com.orbitastra.backend.controllers.student;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.orbitastra.backend.dto.crm.ConvertAdmissionRequest;
import com.orbitastra.backend.dto.student.CreateStudentRequest;
import com.orbitastra.backend.models.student.Student;
import com.orbitastra.backend.services.crm.AdmissionService;
import com.orbitastra.backend.services.student.StudentService;

@ExtendWith(MockitoExtension.class)
class StudentControllerTest {

    @Mock
    private StudentService studentService;

    @Mock
    private AdmissionService admissionService;

    @InjectMocks
    private StudentController studentController;

    @Test
    void createStudent_normal_returnsCreated() {
        CreateStudentRequest req = new CreateStudentRequest();
        req.setSchoolId("school-1");
        req.setAdmissionNo("ADM-001");
        req.setName("John Doe");

        Student created = Student.builder().id("std-1").schoolId("school-1").name("John Doe").build();
        when(studentService.createStudent(org.mockito.ArgumentMatchers.any(CreateStudentRequest.class))).thenReturn(created);

        ResponseEntity<Student> res = studentController.createStudent(req);
        assertEquals(HttpStatus.CREATED, res.getStatusCode());
        assertNotNull(res.getBody());
        assertEquals("std-1", res.getBody().getId());
    }


    @Test
    void createStudentFromAdmissionBody_returnsCreated() {
        ConvertAdmissionRequest req = new ConvertAdmissionRequest();
        req.setAdmissionId("adm-456");
        req.setName("Alice Smith");

        Student converted = Student.builder().id("std-3").schoolId("school-1").name("Alice Smith").build();
        when(admissionService.convertToStudent(org.mockito.ArgumentMatchers.eq("adm-456"), org.mockito.ArgumentMatchers.any(Student.class)))
                .thenReturn(converted);

        ResponseEntity<Student> res = studentController.createStudentFromAdmissionBody(req);
        assertEquals(HttpStatus.CREATED, res.getStatusCode());
        assertNotNull(res.getBody());
        assertEquals("std-3", res.getBody().getId());
    }
}
