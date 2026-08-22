package com.orbitastra.backend.models.crm.embedded;

import java.time.Instant;

import com.orbitastra.backend.models.crm.enums.InquiryStatus;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * One immutable CRM interaction embedded in an {@code Inquiry.followUps} list.
 *
 * <p>{@code counselorDocsId} references the staff member who recorded the
 * interaction. {@code nextFollowUpAt} is copied to the parent Inquiry's
 * top-level field when it becomes the current next action, allowing efficient
 * counselor dashboard queries.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InquiryFollowUp {

    // Example: InquiryStatus.CONTACTED
    @NotNull
    private InquiryStatus status;

    // Example: "Called the parent and shared the admission brochure."
    private String note;

    // Example: "PHONE"
    @NotBlank
    private String communicationChannel;

    // Example: 2026-07-15T10:30:00Z
    private Instant nextFollowUpAt;

    // Links to the staff/counselor document. Example: "67aa15d9dc3f7d0055555555"
    @NotBlank
    private String counselorDocsId;

    // Example: 2026-07-10T09:15:00Z
    @NotNull
    private Instant recordedAt;
}
