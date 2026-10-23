package com.orbitastra.backend.dto.student.guardian.request;

import com.orbitastra.backend.models.common.enums.GuardianRelation;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * What #11b attaches a child to a guardian with.
 *
 * <h2>The guardian is the path; this names the child</h2>
 *
 * <p>{@code POST /schools/current/guardians/{guardianDocsId}} — so the person is already settled
 * before the body is read, and the body's job is to say <b>which child</b> and <b>what this
 * guardian is to them</b>.
 *
 * <p>It is #11 ({@code POST /students/{id}/guardians}) with the two ids swapped. <b>The document
 * written is the same either way</b> — a {@code GuardianLink} appended to the child's
 * {@code guardians} array — because the link lives on the <i>child</i>, never on the guardian.
 * Each endpoint has its own service method; neither calls the other.
 *
 * <h2>Link only. It makes neither side</h2>
 *
 * <p>There is nothing of the person here — no name, no number, no address — because the guardian
 * already exists. <b>Creating one is #7.</b> Nor can this admit the child: that is #1, which
 * decides an admission number, an admission date and a status, none of which belong on a contact's
 * page.
 *
 * <h2>The flags are this child's, and only this child's</h2>
 *
 * <p>The same man is "father, primary contact, may collect" to one child and only an emergency
 * number for their cousin. So they are <b>never read off the guardian and never written back to
 * them</b> — which is why a guardian document carries no relation at all.
 */
public record StudentLinkGuardianRequest(

        /**
         * The child to attach this guardian to.
         *
         * <p><b>Required.</b> The guardian comes from the path, so this is the only id the body
         * carries — and a missing one is a mistake rather than a request, because this endpoint
         * has no way to admit a child.
         */
        @NotBlank @Size(max = 60) String studentDocsId,

        /**
         * What this guardian is <b>to this child</b>.
         *
         * <p><b>Required</b> — the one thing a link cannot be without. {@code GUARDIAN} and
         * {@code GRANDPARENT} are not values; a picker offering them sends a 400.
         */
        @NotNull GuardianRelation relation,

        /**
         * The main person the school rings about this child.
         *
         * <p><b>Setting it true clears it on the child's other guardians</b>, in the same write.
         * Two primary contacts is not a state a school can act on — somebody has to be rung
         * first — so the endpoint picks rather than refusing or storing the contradiction. The
         * response names who was demoted.
         *
         * <p>Absent is false.
         */
        Boolean primaryContact,

        /** May be rung in an emergency. Several guardians can hold this at once. Absent is false. */
        Boolean emergencyContact,

        /** May collect the child from the gate. Absent is false. */
        Boolean pickupAuthorized,

        /** May be given guardian-portal access, once a portal exists. Absent is false. */
        Boolean portalAccess,

        /**
         * <b>The CHILD'S version, not the guardian's.</b>
         *
         * <p>The easiest mistake this endpoint allows, and worth saying twice: the caller is on a
         * <i>guardian's</i> page and the version on that screen is the guardian's. <b>The write
         * lands on the student document</b>, so that is what is checked — read the child first and
         * send theirs. The refusal names the child, which is the clue.
         *
         * <p>Required. Leaving it out is {@code 400 VALIDATION_FAILED} rather than a silent
         * last-write-wins, which is how one parent quietly replaces another.
         */
        @NotNull Long version) {
}
