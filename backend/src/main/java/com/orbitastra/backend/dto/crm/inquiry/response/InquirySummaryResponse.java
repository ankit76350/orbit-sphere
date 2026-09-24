package com.orbitastra.backend.dto.crm.inquiry.response;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.orbitastra.backend.models.crm.Inquiry;
import com.orbitastra.backend.models.crm.enums.InquiryStatus;

/**
 * One row of #13's worklist.
 *
 * <p><b>Thinner than the lead.</b> No notes, no source details, no timeline — all three can be
 * long, and a page of twenty would carry every word a counsellor ever wrote to draw a list that
 * shows none of them. #14 is where the timeline is.
 *
 * <p><b>But the guardian's phone is here</b>, which is the one field a worklist exists to act on:
 * the whole point of the list is to pick up the phone. It is the <i>primary</i> guardian's, or the
 * first one with a number — a row that showed nothing because the first guardian happened to have
 * no phone would be a row nobody can use.
 */
public record InquirySummaryResponse(

        String inquiryId,

        String inquiryNo,

        String prospectiveStudentName,

        String academicYear,

        InquiryStatus status,

        @JsonInclude(JsonInclude.Include.NON_NULL)
        String assignedCounselorDocsId,

        @JsonInclude(JsonInclude.Include.NON_NULL)
        String assignedCounselorName,

        @JsonInclude(JsonInclude.Include.NON_NULL)
        String contactPhoneNumber,

        @JsonInclude(JsonInclude.Include.NON_NULL)
        Instant nextFollowUpAt,

        /**
         * Whether that date has gone by on a lead still worth chasing.
         *
         * <p><b>Worked out for the row, not filtered on.</b> A caller listing everything still
         * wants to see which rows are late, and asking them to compare a timestamp themselves is
         * how two screens end up disagreeing about what "overdue" means.
         */
        boolean overdue,

        int followUpCount,

        Instant createdAt,

        Long version) {

    public static InquirySummaryResponse fromInquiry(Inquiry one, String assignedCounselorName,
            String contactPhoneNumber, boolean overdue) {

        return new InquirySummaryResponse(
                one.getId(),
                one.getInquiryNo(),
                one.getProspectiveStudentName(),
                one.getAcademicYear(),
                one.getStatus(),
                one.getAssignedCounselorDocsId(),
                assignedCounselorName,
                contactPhoneNumber,
                one.getNextFollowUpAt(),
                overdue,
                one.getFollowUps() == null ? 0 : one.getFollowUps().size(),
                one.getCreatedAt(),
                one.getVersion());
    }
}
