package com.orbitastra.backend.models.crm.embedded;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.orbitastra.backend.models.crm.enums.InquiryStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * One entry in an inquiry's follow-up timeline. Recorded whenever the lead is
 * worked — typically on a status change — capturing the stage reached, a note
 * about what happened, and the agreed next follow-up date. The inquiry keeps an
 * ordered list of these; its top-level {@code status} mirrors the latest entry.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InquiryFollowUp {

    private InquiryStatus status;

    private String note;

    private LocalDate nextFollowUp;

    // Who recorded this follow-up (references Staff.id).
    private String counselorId;

    private LocalDateTime recordedAt;
}
