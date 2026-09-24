package com.orbitastra.backend.dto.crm.inquiry.response;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.orbitastra.backend.models.common.enums.Gender;
import com.orbitastra.backend.models.common.enums.GuardianRelation;
import com.orbitastra.backend.models.crm.Inquiry;
import com.orbitastra.backend.models.crm.embedded.InquiryGuardian;
import com.orbitastra.backend.models.crm.enums.InquiryStatus;

/** One lead. */
public record InquiryResponse(

        String inquiryId,

        String inquiryNo,

        String prospectiveStudentName,

        String academicYear,

        @JsonInclude(JsonInclude.Include.NON_NULL)
        LocalDate dateOfBirth,

        @JsonInclude(JsonInclude.Include.NON_NULL)
        Gender gender,

        @JsonInclude(JsonInclude.Include.NON_NULL)
        String interestedClassDocsId,

        @JsonInclude(JsonInclude.Include.NON_NULL)
        String interestedClassName,

        InquiryStatus status,

        @JsonInclude(JsonInclude.Include.NON_NULL)
        String assignedCounselorDocsId,

        @JsonInclude(JsonInclude.Include.NON_NULL)
        String assignedCounselorName,

        List<Guardian> guardians,

        @JsonInclude(JsonInclude.Include.NON_NULL)
        String source,

        @JsonInclude(JsonInclude.Include.NON_NULL)
        String sourceDetails,

        @JsonInclude(JsonInclude.Include.NON_NULL)
        String notes,

        @JsonInclude(JsonInclude.Include.NON_NULL)
        Instant nextFollowUpAt,

        /**
         * How many follow-ups are logged against it.
         *
         * <p><b>A count, not the entries.</b> #10 pushes them and #14 reads the timeline; a
         * freshly captured lead has none, and a create response carrying an empty list would be
         * noise on every one of them.
         */
        int followUpCount,

        Instant createdAt,

        Long version,

        @JsonInclude(JsonInclude.Include.NON_NULL)
        String nextStep) {

    /** One guardian, as it reads back. */
    public record Guardian(
            @JsonInclude(JsonInclude.Include.NON_NULL) String fullName,
            @JsonInclude(JsonInclude.Include.NON_NULL) GuardianRelation relation,
            @JsonInclude(JsonInclude.Include.NON_NULL) String phoneNumber,
            @JsonInclude(JsonInclude.Include.NON_NULL) String emailAddress,
            @JsonInclude(JsonInclude.Include.NON_NULL) String address,
            @JsonInclude(JsonInclude.Include.NON_NULL) String occupation,
            Boolean primaryContact) {

        /**
         * <p><b>Public because #14 shares it.</b> Two records with identical guardian fields,
         * mapped in two places, are two things that drift the first time a field is added to one.
         */
        public static List<Guardian> fromGuardians(List<InquiryGuardian> stored) {
            return stored == null ? List.of() : stored.stream()
                    .map(one -> new Guardian(one.getFullName(), one.getRelation(),
                            one.getPhoneNumber(), one.getEmailAddress(), one.getAddress(),
                            one.getOccupation(), one.getPrimaryContact()))
                    .toList();
        }
    }

    public static InquiryResponse fromInquiry(Inquiry inquiry, String interestedClassName,
            String assignedCounselorName, String nextStep) {

        return new InquiryResponse(
                inquiry.getId(),
                inquiry.getInquiryNo(),
                inquiry.getProspectiveStudentName(),
                inquiry.getAcademicYear(),
                inquiry.getDateOfBirth(),
                inquiry.getGender(),
                inquiry.getInterestedClassDocsId(),
                interestedClassName,
                inquiry.getStatus(),
                inquiry.getAssignedCounselorDocsId(),
                assignedCounselorName,
                Guardian.fromGuardians(inquiry.getGuardians()),
                inquiry.getSource(),
                inquiry.getSourceDetails(),
                inquiry.getNotes(),
                inquiry.getNextFollowUpAt(),
                inquiry.getFollowUps() == null ? 0 : inquiry.getFollowUps().size(),
                inquiry.getCreatedAt(),
                inquiry.getVersion(),
                nextStep);
    }
}
