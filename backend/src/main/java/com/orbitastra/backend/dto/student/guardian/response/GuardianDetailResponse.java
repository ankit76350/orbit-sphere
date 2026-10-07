package com.orbitastra.backend.dto.student.guardian.response;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.orbitastra.backend.models.common.enums.SchoolLocale;
import com.orbitastra.backend.models.student.Guardian;

/**
 * One guardian <b>on their own</b>, as #7 answers with.
 *
 * <p><b>Different from {@link GuardianResponse}, and the difference is the point.</b> That one is a
 * guardian <i>as seen from a child</i>: it carries the relation and the flags, which are facts
 * about the two of them together. This one is the person, who exists whether or not any child is
 * linked to them.
 *
 * <p><b>So there is no {@code relation}, no {@code primaryContact}, no {@code matched}.</b> A
 * guardian created here belongs to nobody yet — attaching them is #11 — and inventing a relation
 * for a person with no child would be answering a question nobody asked.
 */
public record GuardianDetailResponse(

        String guardianDocsId,
        String fullName,

        @JsonInclude(JsonInclude.Include.NON_NULL)
        String phoneNumber,

        @JsonInclude(JsonInclude.Include.NON_NULL)
        String alternatePhoneNumber,

        @JsonInclude(JsonInclude.Include.NON_NULL)
        String emailAddress,

        @JsonInclude(JsonInclude.Include.NON_NULL)
        String address,

        @JsonInclude(JsonInclude.Include.NON_NULL)
        String occupation,

        @JsonInclude(JsonInclude.Include.NON_NULL)
        SchoolLocale preferredLanguage,

        Instant createdAt,
        Instant updatedAt,
        Long version,

        /** What to do next. Null on a read: a read changed nothing. */
        @JsonInclude(JsonInclude.Include.NON_NULL)
        String nextStep) {

    public static GuardianDetailResponse of(Guardian person, String nextStep) {
        return new GuardianDetailResponse(
                person.getId(),
                person.getFullName(),
                person.getPhoneNumber(),
                person.getAlternatePhoneNumber(),
                person.getEmailAddress(),
                person.getAddress(),
                person.getOccupation(),
                person.getPreferredLanguage(),
                person.getCreatedAt(),
                person.getUpdatedAt(),
                person.getVersion(),
                nextStep);
    }
}
