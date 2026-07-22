package com.orbitastra.backend.dto.crm;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Partial-update payload for an admission (PATCH). All fields optional; only
 * non-null fields are applied. {@code studentDocsId} is set only by convert and is
 * never editable here. The source {@code inquiryDocsId} is immutable after
 * creation so changing it cannot desynchronise the inquiry/admission relationship.
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class UpdateAdmissionRequest extends AdmissionDetailsRequest {
}
