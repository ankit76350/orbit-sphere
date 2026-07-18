package com.orbitastra.backend.dto.student;

import java.time.LocalDate;
import java.util.List;

import com.orbitastra.backend.models.student.enums.Gender;
import com.orbitastra.backend.models.student.enums.StudentStatus;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Client payload for admitting a student. Server-owned fields ({@code id} and
 * audit timestamps) are not accepted from the request body. The optional
 * {@code currentAcademicRecord} carries the year/class/roll placement.
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

    private String walletId;

    private String medicalRecordId;

    private StudentStatus status;

    private LocalDate admissionDate;

    @Valid
    private List<GuardianLinkRequest> guardians;

    @Valid
    private AcademicRecordRequest currentAcademicRecord;
}
