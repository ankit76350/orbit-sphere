package com.orbitastra.backend.dto.academics;

import java.util.List;

import com.orbitastra.backend.models.academics.SchoolClass;

import lombok.Data;

/**
 * Partial-update payload for a class (PATCH). All fields optional; only non-null
 * fields are applied. {@code academicYear} is accepted only so the service can
 * reject an attempt to change it.
 */
@Data
public class UpdateSchoolClassRequest {

    private String schoolId;

    private String name;

    private String classTeacher;

    private List<SchoolClass.ClassSubject> subjects;

    private String academicYear;

    private List<String> sections;
}
