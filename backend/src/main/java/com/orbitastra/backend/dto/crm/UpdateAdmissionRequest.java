package com.orbitastra.backend.dto.crm;

import java.time.LocalDate;
import java.util.List;

import com.orbitastra.backend.models.crm.enums.AdmissionStatus;
import com.orbitastra.backend.models.student.enums.Gender;

import jakarta.validation.Valid;
import lombok.Data;

/**
 * Partial-update payload for an admission (PATCH). All fields optional; only
 * non-null fields are applied. {@code studentId} is set only by convert and is
 * never editable here. {@code academicYear} is accepted only so the service can
 * reject an attempt to change it.
 */
@Data
public class UpdateAdmissionRequest {

    private String academicYear;

    private AdmissionStatus status;

    private List<String> documents;

    private LocalDate admissionDate;

    private String inquiryId;

    private String studentName;

    private LocalDate dob;

    private Gender gender;

    @Valid
    private List<InquiryGuardianRequest> guardians;
}
