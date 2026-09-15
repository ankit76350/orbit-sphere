package com.orbitastra.backend.dto.people.staff.response;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * What #16 answers with: the new employment, and whatever it displaced.
 *
 * <p><b>Both halves, because a promotion is one event with two.</b> A caller that saw only the new
 * record would have to read again to learn that the previous one was closed and on what date —
 * and that date is computed here rather than sent, so it is the one thing they could not know.
 *
 * <p><b>{@code closed} is absent on a first hire</b>, which is how a caller tells a hire from a
 * promotion without comparing dates.
 */
public record EmploymentWriteResponse(

        /** The record now in force. */
        EmploymentResponse employment,

        /** The record this write ended, or absent when the person had none. */
        @JsonInclude(JsonInclude.Include.NON_NULL)
        EmploymentResponse closed,

        /**
         * A seat filled beyond its approved headcount, and anything else worth saying on a 201.
         *
         * <p><b>A warning, never a refusal.</b> A school hiring a twelfth teacher into eleven
         * approved seats is describing something that has already happened, and refusing it stops
         * the system recording the truth. The same call #14 makes about lowering a headcount.
         */
        @JsonInclude(JsonInclude.Include.NON_NULL)
        String warning,

        @JsonInclude(JsonInclude.Include.NON_NULL)
        String nextStep) {
}
