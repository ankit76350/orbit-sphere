package com.orbitastra.backend.dto.crm.admissionapplication.response;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.orbitastra.backend.models.common.enums.Gender;
import com.orbitastra.backend.models.common.enums.GuardianRelation;
import com.orbitastra.backend.models.crm.AdmissionApplication;
import com.orbitastra.backend.models.crm.embedded.InquiryGuardian;
import com.orbitastra.backend.models.crm.enums.AdmissionApplicationStatus;

/**
 * One admission application, as the API gives it back. Endpoint #17, and the reads when built.
 */
public record AdmissionApplicationResponse(

        /** The id reviews, offers and the resulting student all point at. */
        String admissionApplicationId,

        /** Generated, never supplied. Nobody picks their own application number. */
        String applicationNo,

        String admissionCycleDocsId,

        @JsonInclude(JsonInclude.Include.NON_NULL)
        String inquiryDocsId,

        String appliedClassDocsId,

        @JsonInclude(JsonInclude.Include.NON_NULL)
        String appliedClassName,

        String applicantName,
        LocalDate dateOfBirth,
        Gender gender,
        AdmissionApplicationStatus status,
        List<Guardian> guardians,

        /** Stored as sent. Nothing validates it — there is no form definition to validate against. */
        @JsonInclude(JsonInclude.Include.NON_NULL)
        Map<String, Object> formAnswers,

        Instant createdAt,

        /**
         * What #20 must send back to decide this application safely.
         *
         * <p><b>Added with #20, because a parameter nobody can learn the value of is a parameter
         * that cannot be used.</b> #20 accepts a {@code version} and refuses a stale one — and
         * until this was here, the only way to find it was to read the document out of Mongo. The
         * same call {@code timetable} made for its full-day replace.
         *
         * <p>The cycle endpoints (#2, #3, #4) accept one too and still return none; that is a
         * pre-existing gap rather than something this endpoint introduced.
         */
        Long version,

        @JsonInclude(JsonInclude.Include.NON_NULL)
        String nextStep) {

    /** A guardian as stored on the application — a snapshot, not a link. */
    public record Guardian(
            String fullName,
            GuardianRelation relation,
            @JsonInclude(JsonInclude.Include.NON_NULL) String phoneNumber,
            @JsonInclude(JsonInclude.Include.NON_NULL) String emailAddress,
            @JsonInclude(JsonInclude.Include.NON_NULL) String address,
            @JsonInclude(JsonInclude.Include.NON_NULL) String occupation,
            Boolean primaryContact) {

        /**
         * The stored guardians as this record, or an empty list.
         *
         * <p><b>Shared with #25</b>, which gives the same shape back. It was copied there first
         * and pulled up here the same day: two records with identical fields, mapped in two
         * places, are two things that drift the first time a field is added to one of them.
         */
        public static List<Guardian> fromGuardians(List<InquiryGuardian> stored) {
            return stored == null ? List.of() : stored.stream()
                    .map(one -> new Guardian(one.getFullName(), one.getRelation(),
                            one.getPhoneNumber(), one.getEmailAddress(), one.getAddress(),
                            one.getOccupation(), one.getPrimaryContact()))
                    .toList();
        }
    }

    public static AdmissionApplicationResponse fromApplication(AdmissionApplication application,
            String appliedClassName, String nextStep) {

        List<Guardian> guardians = Guardian.fromGuardians(application.getGuardians());

        return new AdmissionApplicationResponse(
                application.getId(),
                application.getApplicationNo(),
                application.getAdmissionCycleDocsId(),
                application.getInquiryDocsId(),
                application.getAppliedClassDocsId(),
                appliedClassName,
                application.getApplicantName(),
                application.getDateOfBirth(),
                application.getGender(),
                application.getStatus(),
                guardians,
                // Left out when empty rather than sent as {} — an absent map and an empty one say
                // the same thing, and one of them is noise on every row.
                application.getFormAnswers() == null || application.getFormAnswers().isEmpty()
                        ? null : application.getFormAnswers(),
                application.getCreatedAt(),
                application.getVersion(),
                nextStep);
    }
}
