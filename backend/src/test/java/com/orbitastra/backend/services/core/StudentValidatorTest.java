package com.orbitastra.backend.services.core;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.orbitastra.backend.exceptions.ResourceNotFoundException;
import com.orbitastra.backend.models.student.Student;
import com.orbitastra.backend.repositories.student.StudentRepository;
import com.orbitastra.backend.services.utils.StudentValidator;

@ExtendWith(MockitoExtension.class)
public class StudentValidatorTest {

    @Mock
    private StudentRepository studentRepository;

    @InjectMocks
    private StudentValidator studentValidator;

    private Student student;

    @BeforeEach
    void setUp() {
        student = new Student();
        student.setId("student-123");
        student.setSchoolId("school-123");
    }

    @Test
    void validateStudent_Success_ReturnsStudent() {
        when(studentRepository.findById("student-123")).thenReturn(Optional.of(student));

        Student result = studentValidator.validateStudent("student-123", "school-123");

        assertSame(student, result);
        verify(studentRepository, times(1)).findById("student-123");
    }

    @Test
    void validateStudent_NullId_ThrowsIllegalArgument() {
        assertThrows(IllegalArgumentException.class,
                () -> studentValidator.validateStudent(null, "school-123"));
        verifyNoInteractions(studentRepository);
    }

    @Test
    void validateStudent_EmptyId_ThrowsIllegalArgument() {
        assertThrows(IllegalArgumentException.class,
                () -> studentValidator.validateStudent("", "school-123"));
        verifyNoInteractions(studentRepository);
    }

    @Test
    void validateStudent_NotFound_ThrowsResourceNotFound() {
        when(studentRepository.findById("student-123")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> studentValidator.validateStudent("student-123", "school-123"));
    }

    @Test
    void validateStudent_DifferentSchool_ThrowsIllegalArgument() {
        when(studentRepository.findById("student-123")).thenReturn(Optional.of(student));

        assertThrows(IllegalArgumentException.class,
                () -> studentValidator.validateStudent("student-123", "other-school"));
    }
}
