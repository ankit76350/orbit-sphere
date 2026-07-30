package com.orbitastra.backend.models.new_new.crm.embedded;

import java.time.Instant;

import com.orbitastra.backend.models.new_new.crm.enums.InquiryStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InquiryFollowUp {

    // Example: InquiryStatus.CONTACTED
    private InquiryStatus status;

    // Example: "Called the parent and shared the admission brochure."
    private String note;

    // Example: "PHONE"
    private String communicationChannel;

    // Example: 2026-07-15T10:30:00Z
    private Instant nextFollowUpAt;

    // Example: "67aa15d9dc3f7d0055555555"
    private String counselorDocsId;

    // Example: 2026-07-10T09:15:00Z
    private Instant recordedAt;
}
