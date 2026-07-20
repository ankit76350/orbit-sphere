package com.orbitastra.backend.dto.crm;

import java.time.LocalDate;

import com.orbitastra.backend.models.crm.InquiryFollowUp;
import com.orbitastra.backend.models.crm.enums.InquiryStatus;

import jakarta.validation.constraints.FutureOrPresent;
import lombok.Data;

/**
 * A follow-up / status-change entry on an inquiry. {@code recordedAt} is stamped
 * server-side and is not accepted from the request body.
 */
@Data
public class FollowUpRequest {

    private InquiryStatus status;

    private String note;

    @FutureOrPresent(message = "Next follow-up date cannot be in the past.")
    private LocalDate nextFollowUp;

    private String counselorId;

    public InquiryFollowUp toModel() {
        return InquiryFollowUp.builder()
                .status(status)
                .note(note)
                .nextFollowUp(nextFollowUp)
                .counselorId(counselorId)
                .build();
    }
}
