package com.orbitastra.backend.dto.crm;

import java.time.LocalDate;
import java.util.List;

import com.orbitastra.backend.models.crm.Admission;
import com.orbitastra.backend.models.crm.enums.AdmissionStatus;
import com.orbitastra.backend.models.student.enums.Gender;

import jakarta.validation.Valid;
import lombok.Data;

/** Shared, client-editable admission fields used by create and patch requests. */
@Data
public abstract class AdmissionDetailsRequest {

    private String studentName;

    private LocalDate dob;

    private Gender gender;

    @Valid
    private List<InquiryGuardianRequest> guardians;

    private AdmissionStatus status;

    private List<String> documents;

    private LocalDate admissionDate;

    public Admission toModel() {
        return Admission.builder()
                .studentName(studentName)
                .dob(dob)
                .gender(gender)
                .guardians(InquiryGuardianRequest.toModels(guardians))
                .status(status)
                .documents(documents)
                .admissionDate(admissionDate)
                .build();
    }
}
