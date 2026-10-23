package com.orbitastra.backend.dto.student.guardian.response;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.orbitastra.backend.models.common.enums.GuardianRelation;
import com.orbitastra.backend.models.common.enums.SchoolLocale;
import com.orbitastra.backend.models.student.Guardian;
import com.orbitastra.backend.models.student.Student;
import com.orbitastra.backend.models.student.embedded.GuardianLink;
import com.orbitastra.backend.models.student.enums.StudentStatus;

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
 *
 * <p><b>{@code children} is #10's half, and it is where those flags do appear</b> — once per
 * child, because that is the only place they mean anything. It is absent on #7 and #9: neither
 * reads {@code students}, and an empty list there would say "this guardian has no children" when
 * the truth is "nobody asked".
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

        /**
         * Every child in this school attached to this guardian, <b>#10 only</b>.
         *
         * <p>Absent on #7 and #9 — neither reads {@code students}, and an empty list would say
         * "this guardian has no children" where the truth is "nobody asked". <b>Present and empty
         * on #10</b> is the real answer: a guardian #7 created and #11 has never attached.
         */
        @JsonInclude(JsonInclude.Include.NON_NULL)
        List<AttachedChild> children,

        /**
         * How many children this correction changed, <b>#8 only</b>.
         *
         * <p><b>The point of the shared row, said out loud.</b> Correcting a mother's number
         * changes it on all four of her children at once, and a caller who did not expect that
         * should find out from the response rather than from a parent.
         *
         * <p>A count rather than the children themselves — #10 is where they are listed, and this
         * is a write.
         */
        @JsonInclude(JsonInclude.Include.NON_NULL)
        Long childrenAffected,

        /** What to do next. Null on a read: a read changed nothing. */
        @JsonInclude(JsonInclude.Include.NON_NULL)
        String nextStep) {

    /**
     * One child this guardian belongs to, <b>and what they are to that child</b>.
     *
     * <p><b>The flags are the point of this shape.</b> They live on {@code GuardianLink}, embedded
     * in the <i>student</i> — not on the guardian — because the same man is "father, primary
     * contact, may collect, portal" to one child and only an emergency number for their cousin.
     * A guardian's page that printed one set of flags would be printing a fiction.
     *
     * <p>The child's own details are the thin ones: enough to recognise who is meant and open
     * them with {@code student} #5. Their guardians are not here — this is already a read of one
     * guardian, and listing each child's <i>other</i> contacts would be a third collection deep
     * for a question nobody asked.
     */
    public record AttachedChild(

            String studentDocsId,
            String admissionNo,
            String fullName,
            LocalDate dateOfBirth,
            StudentStatus status,

            /** Whether anybody has put them in a class. {@code student} #14 does, and is not built. */
            Boolean placed,

            /** What this guardian is <b>to this child</b>. */
            GuardianRelation relation,
            Boolean primaryContact,
            Boolean emergencyContact,
            Boolean pickupAuthorized,
            Boolean portalAccess) {

        public static AttachedChild of(Student child, GuardianLink link) {
            return new AttachedChild(
                    child.getId(),
                    child.getAdmissionNo(),
                    child.getFullName(),
                    child.getDateOfBirth(),
                    child.getStatus(),
                    child.getCurrentAcademicRecordDocsId() != null,
                    link.getRelation(),
                    link.getPrimaryContact(),
                    link.getEmergencyContact(),
                    link.getPickupAuthorized(),
                    link.getPortalAccess());
        }
    }

    /** For #7 and #9, which do not read {@code students}. */
    public static GuardianDetailResponse of(Guardian person, String nextStep) {
        return build(person, null, null, nextStep);
    }

    /** For #10, which lists the children. */
    public static GuardianDetailResponse of(Guardian person, List<AttachedChild> children,
            String nextStep) {
        return build(person, children, null, nextStep);
    }

    /** For #8, which only counts them. */
    public static GuardianDetailResponse corrected(Guardian person, long childrenAffected,
            String nextStep) {
        return build(person, null, childrenAffected, nextStep);
    }

    private static GuardianDetailResponse build(Guardian person, List<AttachedChild> children,
            Long childrenAffected, String nextStep) {
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
                children,
                childrenAffected,
                nextStep);
    }
}
