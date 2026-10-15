package com.orbitastra.backend.dto.student.student.request;

import com.orbitastra.backend.models.common.enums.GuardianRelation;

import jakarta.validation.constraints.NotNull;

/**
 * What #12 changes on one guardian's link to one child.
 *
 * <p>The guardian and the child are both in the path. These fields are <b>this child's</b> view
 * of the guardian — the same person can be "father, primary" to one child and only an emergency
 * number for another — so the guardian's own record is never touched.
 *
 * <p><b>Absent leaves a field alone.</b> A body that changes nothing is refused.
 */
public record StudentGuardianUpdateRequest(

        /** What this guardian is to this child. Absent leaves it alone. */
        GuardianRelation relation,

        /**
         * The main person the school contacts. <b>{@code true} moves it here</b> — the child's
         * other primary guardian stops being primary in the same write.
         *
         * <p><b>{@code false} on the current primary is refused</b> ({@code LAST_PRIMARY_CONTACT}),
         * because the child would be left with none. Make someone else primary instead.
         */
        Boolean primaryContact,

        /** May be contacted in an emergency. */
        Boolean emergencyContact,

        /** Allowed to collect the child. */
        Boolean pickupAuthorized,

        /** May have guardian-portal access. */
        Boolean portalAccess,

        /** <b>The STUDENT'S version</b> — the link lives inside the student's document. */
        @NotNull Long version) {
}
