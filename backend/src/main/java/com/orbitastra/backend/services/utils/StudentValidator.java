package com.orbitastra.backend.services.utils;

import org.springframework.stereotype.Component;

import com.orbitastra.backend.exceptions.ResourceNotFoundException;
import com.orbitastra.backend.models.student.Student;
import com.orbitastra.backend.repositories.student.StudentRepository;
import com.orbitastra.backend.services.core.AcademicYearResolver;

import lombok.RequiredArgsConstructor;

/**
 * Shared guard for student-scoped operations. Every service that stores or
 * mutates a record tied to a student resolves it through here so the rules
 * (non-empty id, student exists, and belongs to the acting school) stay
 * identical everywhere — mirrors {@link AcademicYearResolver} for years.
 */
@Component
@RequiredArgsConstructor
public class StudentValidator {

    private final StudentRepository studentRepository;

    /**
     * Validates that a student with this id exists and belongs to the given
     * school, returning the resolved {@link Student} for callers that need it.
     */
    public Student validateStudent(String studentId, String schoolId) {
        if (studentId == null || studentId.isEmpty()) {
            throw new IllegalArgumentException("Student ID cannot be null or empty.");
        }
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found with id: " + studentId));
        if (!student.getSchoolId().equals(schoolId)) {
            throw new IllegalArgumentException(
                    "Student does not belong to the specified school, so this action cannot be performed.");
        }
        return student;
    }
}
