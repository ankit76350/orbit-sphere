package com.orbitastra.backend.dto.student.guardian.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.orbitastra.backend.models.common.enums.GuardianRelation;
import com.orbitastra.backend.models.student.Guardian;
import com.orbitastra.backend.models.student.Student;
import com.orbitastra.backend.models.student.embedded.GuardianLink;

/**
 * What #11b hands back: <b>the link that was just made</b>, with both ends named.
 *
 * <h2>Both names, because neither page has both</h2>
 *
 * <p>The caller is on a guardian's page and chose a child by id. <b>Echoing the two ids back would
 * tell them nothing they did not send</b> — so the names come too, and a screen can say "ANKIT
 * KUMAR is now Ayan khan's father" without a second read.
 *
 * <h2>The flags are echoed from the stored link, not from the request</h2>
 *
 * <p>They look the same in the ordinary case and they are not the same thing. {@code
 * primaryContact} in particular is <b>settled during the write</b> — and reading the saved link
 * back is what makes this response a statement about the database rather than a repetition of what
 * was asked for.
 *
 * <h2>{@code demotedGuardianName} is the one field worth looking at</h2>
 *
 * <p>Asking for {@code primaryContact} <b>takes it from whoever held it</b>, in the same write.
 * That is a change to a guardian the caller never mentioned, so it is said out loud rather than
 * left to be discovered on the next read. <b>Null means nobody lost anything</b> — either the
 * caller did not ask for primary, or no other guardian held it.
 *
 * <h2>{@code version} is the child's, as it now stands</h2>
 *
 * <p>The write landed on the student document, so this is the number to send on the next write to
 * that child — including a second link made straight after this one.
 */
public record StudentLinkGuardianResponse(

        /** The child the link was written on. The document that actually changed. */
        String studentDocsId,

        String studentName,

        /** Null for a child admitted without one. */
        @JsonInclude(JsonInclude.Include.NON_NULL)
        String admissionNo,

        /** The guardian from the path. Unchanged by this write — the link lives on the child. */
        String guardianDocsId,

        String guardianName,

        /** What this guardian is to <b>this</b> child, read back off the saved link. */
        GuardianRelation relation,

        Boolean primaryContact,

        Boolean emergencyContact,

        Boolean pickupAuthorized,

        Boolean portalAccess,

        /**
         * How many guardians the child has now, this one included.
         *
         * <p>Ten is the cap, so this is also how close the next call is to
         * {@code 409 TOO_MANY_GUARDIANS}.
         */
        int guardianCount,

        /**
         * Who stopped being the primary contact so this one could take it.
         *
         * <p><b>Absent when nobody did</b>, which is the usual case.
         */
        @JsonInclude(JsonInclude.Include.NON_NULL)
        String demotedGuardianDocsId,

        /** The same person, named. Absent alongside the id above. */
        @JsonInclude(JsonInclude.Include.NON_NULL)
        String demotedGuardianName,

        /** <b>The child's</b> version after the write — what the next write to them must send. */
        Long version,

        /** What the caller should do next. */
        @JsonInclude(JsonInclude.Include.NON_NULL)
        String nextStep) {

    /**
     * Builds the answer from the saved child, the guardian, and the link as it was stored.
     *
     * <p><b>{@code link} is read out of {@code child.getGuardians()} after the save</b>, never
     * built from the request — see the class note on why that distinction is the point of this
     * response.
     */
    public static StudentLinkGuardianResponse of(Student child, Guardian person, GuardianLink link,
            Guardian demoted, String nextStep) {

        return new StudentLinkGuardianResponse(
                child.getId(),
                child.getFullName(),
                child.getAdmissionNo(),
                person.getId(),
                person.getFullName(),
                link.getRelation(),
                Boolean.TRUE.equals(link.getPrimaryContact()),
                Boolean.TRUE.equals(link.getEmergencyContact()),
                Boolean.TRUE.equals(link.getPickupAuthorized()),
                Boolean.TRUE.equals(link.getPortalAccess()),
                child.getGuardians() == null ? 0 : child.getGuardians().size(),
                demoted == null ? null : demoted.getId(),
                demoted == null ? null : demoted.getFullName(),
                child.getVersion(),
                nextStep);
    }
}
