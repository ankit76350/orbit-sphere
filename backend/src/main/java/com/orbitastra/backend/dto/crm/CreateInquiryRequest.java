package com.orbitastra.backend.dto.crm;

import java.util.List;

import com.orbitastra.backend.models.crm.enums.InquiryStatus;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Client payload for opening a CRM inquiry (lead). Server-owned fields
 * ({@code id}, audit timestamps) are not accepted from the request body.
 */
@Data
public class CreateInquiryRequest {

    @NotBlank(message = "schoolId is required")
    private String schoolId;

    private String studentName;

    @Valid
    private List<InquiryGuardianRequest> guardians;

    private String source;

    private String counselorId;

    private InquiryStatus status;

    @Valid
    private List<FollowUpRequest> followUps;
}
