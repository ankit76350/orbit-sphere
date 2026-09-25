package com.orbitastra.backend.dto.crm.inquiry.response;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.orbitastra.backend.models.common.enums.Gender;
import com.orbitastra.backend.models.crm.Inquiry;
import com.orbitastra.backend.models.crm.embedded.InquiryFollowUp;
import com.orbitastra.backend.models.crm.enums.InquiryStatus;

/**
 * Endpoint #14 — <b>one lead with its whole timeline</b>.
 *
 * <p><b>Everything #13's row leaves off</b>, plus the thing that is not a field on the document at
 * all in any useful sense: the follow-ups, in the order they happened, with the person who logged
 * each one named.
 */
public record InquiryDetailResponse(

        String inquiryId,
        String inquiryNo,
        String prospectiveStudentName,
        String academicYear,

        @JsonInclude(JsonInclude.Include.NON_NULL) LocalDate dateOfBirth,
        @JsonInclude(JsonInclude.Include.NON_NULL) Gender gender,
        @JsonInclude(JsonInclude.Include.NON_NULL) String interestedClassDocsId,
        @JsonInclude(JsonInclude.Include.NON_NULL) String interestedClassName,

        InquiryStatus status,

        List<InquiryResponse.Guardian> guardians,

        @JsonInclude(JsonInclude.Include.NON_NULL) String source,
        @JsonInclude(JsonInclude.Include.NON_NULL) String sourceDetails,
        @JsonInclude(JsonInclude.Include.NON_NULL) String notes,
        @JsonInclude(JsonInclude.Include.NON_NULL) String lostReason,

        @JsonInclude(JsonInclude.Include.NON_NULL) Instant nextFollowUpAt,

        /** Past its follow-up date and still open. Worked out here, as it is on #13's row. */
        boolean overdue,

        /**
         * The timeline, <b>oldest first</b>.
         *
         * <p>A conversation reads forwards. #10 pushes entries and this is the only endpoint that
         * reads them back.
         */
        List<FollowUp> followUps,

        int followUpCount,

        Instant createdAt,
        Instant updatedAt,
        Long version,

        @JsonInclude(JsonInclude.Include.NON_NULL) String nextStep) {

    /** One logged interaction. */
    public record FollowUp(
            @JsonInclude(JsonInclude.Include.NON_NULL) Instant recordedAt,
            @JsonInclude(JsonInclude.Include.NON_NULL) InquiryStatus status,
            @JsonInclude(JsonInclude.Include.NON_NULL) String note,
            @JsonInclude(JsonInclude.Include.NON_NULL) String communicationChannel,
            @JsonInclude(JsonInclude.Include.NON_NULL) Instant nextFollowUpAt,
            @JsonInclude(JsonInclude.Include.NON_NULL) String counselorDocsId,

            /**
             * Who logged it.
             *
             * <p><b>Absent when they have left the school</b>, which is the honest answer: a call
             * somebody made two years ago still happened, and dropping the entry or inventing a
             * name would hide that.
             */
            @JsonInclude(JsonInclude.Include.NON_NULL) String counselorName) {
    }

    /**
     * @param staffNames everybody who logged a follow-up, to their name, <b>from one query</b>
     */
    public static InquiryDetailResponse fromInquiry(Inquiry inquiry, String interestedClassName,
            Map<String, String> staffNames, boolean overdue, String nextStep) {

        //! OLDEST FIRST, and sorted here rather than trusted. #10 pushes in order, but a $push is
        //! not a promise about order once anything else touches the array — and a timeline that
        //! reads backwards is worse than no timeline.
        //!
        //! AN ENTRY WITH NO recordedAt SORTS LAST rather than throwing. Nothing writes one, but a
        //! null comparator that blew up would take the whole lead with it.
        List<FollowUp> timeline = inquiry.getFollowUps() == null ? List.of()
                : inquiry.getFollowUps().stream()
                        .sorted(Comparator.comparing(InquiryFollowUp::getRecordedAt,
                                Comparator.nullsLast(Comparator.naturalOrder())))
                        .map(one -> new FollowUp(
                                one.getRecordedAt(),
                                one.getStatus(),
                                one.getNote(),
                                one.getCommunicationChannel(),
                                one.getNextFollowUpAt(),
                                one.getCounselorDocsId(),
                                one.getCounselorDocsId() == null ? null
                                        : staffNames.get(one.getCounselorDocsId())))
                        .toList();

        return new InquiryDetailResponse(
                inquiry.getId(),
                inquiry.getInquiryNo(),
                inquiry.getProspectiveStudentName(),
                inquiry.getAcademicYear(),
                inquiry.getDateOfBirth(),
                inquiry.getGender(),
                inquiry.getInterestedClassDocsId(),
                interestedClassName,
                inquiry.getStatus(),
                InquiryResponse.Guardian.fromGuardians(inquiry.getGuardians()),
                inquiry.getSource(),
                inquiry.getSourceDetails(),
                inquiry.getNotes(),
                inquiry.getLostReason(),
                inquiry.getNextFollowUpAt(),
                overdue,
                timeline,
                timeline.size(),
                inquiry.getCreatedAt(),
                inquiry.getUpdatedAt(),
                inquiry.getVersion(),
                nextStep);
    }
}
