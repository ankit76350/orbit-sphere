package com.orbitastra.backend.dto.academics;

import java.util.List;

import com.orbitastra.backend.models.academics.SchoolClass;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Client payload for creating a class. Server-owned fields ({@code id} and audit
 * timestamps) are not accepted from the request body.
 */
@Data
public class CreateSchoolClassRequest {

    @NotBlank(message = "schoolId is required")
    private String schoolId;

    @NotBlank(message = "name is required")
    private String name;

    private String classTeacher;

    private List<SchoolClass.ClassSubject> subjects;

    private String academicYear;

    private List<String> sections;
}
