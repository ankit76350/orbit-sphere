package com.orbitastra.backend.dto.crm;

import java.util.List;

import jakarta.validation.Valid;
import lombok.Data;

/**
 * Partial-update payload for an inquiry (PATCH). All fields optional; only
 * non-null fields are applied. Status changes go through the /follow-ups
 * sub-resource, not here.
 */
@Data
public class UpdateInquiryRequest {

    private String counselorDocsId;

    @Valid
    private List<InquiryGuardianRequest> guardians;

    private String studentName;

    private String source;
}
