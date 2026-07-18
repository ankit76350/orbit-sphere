package com.orbitastra.backend.dto.student;

import java.time.LocalDate;

import com.orbitastra.backend.models.student.enums.Gender;
import com.orbitastra.backend.models.student.enums.StudentStatus;

import jakarta.validation.Valid;
import lombok.Data;

/**
 * Partial-update payload for a student (PATCH). All fields optional; only
 * non-null fields are applied. {@code schoolId} is intentionally omitted so a
 * student cannot be moved between tenants via an update. Guardian links are
 * managed through the dedicated /guardians sub-resource, not here.
 */
@Data
public class UpdateStudentRequest {

    private String admissionNo;

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
    private AcademicRecordRequest currentAcademicRecord;
}
