package com.orbitastra.backend.dto.crm;

import java.time.LocalDate;
import java.util.List;

import com.orbitastra.backend.models.crm.enums.AdmissionStatus;
import com.orbitastra.backend.models.student.enums.Gender;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Client payload for raising an admission — either from an inquiry
 * ({@code inquiryDocsId} set, snapshot copied over) or a direct/walk-in admission
 * (applicant fields supplied inline). {@code studentId} is set only on convert
 * and is never accepted here (prevents mass-assignment).
 */
@Data
public class CreateAdmissionRequest {

    @NotBlank(message = "schoolId is required")
    private String schoolId;

    private String inquiryDocsId;

    private String studentName;

    private LocalDate dob;

    private Gender gender;

    @Valid
    private List<InquiryGuardianRequest> guardians;

    private AdmissionStatus status;

    private List<String> documents;

    private LocalDate admissionDate;
}
