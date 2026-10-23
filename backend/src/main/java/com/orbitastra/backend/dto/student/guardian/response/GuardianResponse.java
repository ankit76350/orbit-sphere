package com.orbitastra.backend.dto.student.guardian.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.orbitastra.backend.models.common.enums.GuardianRelation;
import com.orbitastra.backend.models.common.enums.SchoolLocale;
import com.orbitastra.backend.models.student.Guardian;
import com.orbitastra.backend.models.student.embedded.GuardianLink;

/**
 * One contact, as a child's page shows them.
 *
 * <p><b>Two documents in one object, and that is deliberate.</b> The person — their name, their
 * number, where they live — is a {@code Guardian}, shared by every child they belong to. The
 * <i>role</i> — father, primary contact, allowed to collect — is a {@code GuardianLink} embedded
 * in the student, because the same person can be "father, primary, portal" to one child and only
 * an emergency contact to another.
 *
 * <p>Anybody reading a child's page wants both at once, and asking them to join two shapes in
 * their own code would be handing them this module's internal split.
 *
 * <p><b>{@code matched} is only on #1's answer.</b> It says whether this guardian was already in
 * the school or was written by that request, and it is null everywhere else — see
 * {@code StudentCreatedResponse} for why it matters.
 */
public record GuardianResponse(

        String guardianDocsId,
        String fullName,
        GuardianRelation relation,

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

        Boolean primaryContact,
        Boolean emergencyContact,
        Boolean pickupAuthorized,
        Boolean portalAccess,

        /**
         * <b>Only on #1's answer.</b> True when this person was already in the school and was
         * linked, false when the request created them. Absent on every other endpoint, where the
         * question does not apply.
         */
        @JsonInclude(JsonInclude.Include.NON_NULL)
        Boolean matched) {

    /** For a read — a child's page. No {@code matched}, because nothing was just written. */
    public static GuardianResponse of(Guardian person, GuardianLink link) {
        return build(person, link, null);
    }

    /** For #1, which has to say whether each contact was found or created. */
    public static GuardianResponse of(Guardian person, GuardianLink link, boolean matched) {
        return build(person, link, matched);
    }

    private static GuardianResponse build(Guardian person, GuardianLink link, Boolean matched) {
        return new GuardianResponse(
                person.getId(),
                person.getFullName(),
                link.getRelation(),
                person.getPhoneNumber(),
                person.getAlternatePhoneNumber(),
                person.getEmailAddress(),
                person.getAddress(),
                person.getOccupation(),
                person.getPreferredLanguage(),
                link.getPrimaryContact(),
                link.getEmergencyContact(),
                link.getPickupAuthorized(),
                link.getPortalAccess(),
                matched);
    }
}
