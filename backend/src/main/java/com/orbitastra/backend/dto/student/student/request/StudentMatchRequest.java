package com.orbitastra.backend.dto.student.student.request;

import jakarta.validation.constraints.Size;

/**
 * What #6 asks about before a school admits a child.
 *
 * <p><b>The call made before every admission</b>, and the equivalent of {@code crm} #15 for leads.
 * It exists for the same reason: the alternative is a second record for a child who is already on
 * the roll, and nothing downstream — attendance, fees, marks — can tell afterwards that the two
 * are one person.
 *
 * <p><b>One of the three is required</b>, and sending more than one matches <i>any</i> of them. A
 * family that gives a phone number this time and an admission number next time is the same family.
 */
public record StudentMatchRequest(

        /**
         * Any shape. <b>Matched on its digits</b>, and across the child's own number <i>and their
         * guardians'</i> — a seven year old has no phone, and the number the school holds is their
         * mother's.
         *
         * <p>Ten digits or more is compared on the last ten, so a country code or a trunk 0 stops
         * mattering. Fewer has to match the whole number.
         */
        @Size(max = 40) String phone,

        /** Matched whole and case-insensitively. This is a question about identity. */
        @Size(max = 60) String admissionNo,

        /**
         * Matched <b>anywhere</b>, unlike the other two.
         *
         * <p>A name is not an identifier: somebody typing "aarav" wants every Aarav on the roll to
         * look at, and anchoring it would answer "no" to a question that was really "show me who
         * it might be".
         */
        @Size(max = 160) String name) {
}
