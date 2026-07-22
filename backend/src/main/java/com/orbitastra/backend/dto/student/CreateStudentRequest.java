package com.orbitastra.backend.dto.student;

import java.time.LocalDate;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.orbitastra.backend.models.student.enums.Gender;
import com.orbitastra.backend.models.student.enums.StudentStatus;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Client payload for admitting a student. Server-owned fields ({@code id} and
 * audit timestamps) are not accepted from the request body. Supports top-level
 * placement fields ({@code academicYear}, {@code classDocId}, etc.) and flexible
 * guardian payloads that are automatically deduplicated server-side.
 */
@Data
public class CreateStudentRequest {

    @NotBlank(message = "schoolId is required")
    private String schoolId;

    private String admissionNo;

    @NotBlank(message = "name is required")
    private String name;

    private LocalDate dob;

    private Gender gender;

    private String bloodGroup;

    private String photoUrl;

    @JsonAlias("walletId")
    private String walletDocsId;

    @JsonAlias("medicalRecordId")
    private String medicalRecordDocsId;

    private List<String> documents;

    private List<String> medicalRemark;

    private StudentStatus status;

    private LocalDate admissionDate;

    // Optional top-level academic placement fields
    private String academicYear;

    private String classDocId;

    private String classId; // Alias for classDocId

    private String sectionNo;

    private String rollNo;

    @Valid
    private List<StudentGuardianRequest> guardians;

    @Valid
    private AcademicRecordRequest currentAcademicRecord;
}
