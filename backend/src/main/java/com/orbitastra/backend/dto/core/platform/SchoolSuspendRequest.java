package com.orbitastra.backend.dto.core.platform;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Why a school is being suspended.
 *
 * <p>The reason is <b>required</b>, and that is the only field here. Suspension blocks a whole
 * school — nobody logs in, nothing runs — and a suspension with no reason recorded gets switched
 * back on by the next person who is asked about it, because there is nothing to weigh against
 * the school's complaint.
 *
 * <p>It is stored on {@code School.statusReason} and deliberately kept after reactivation, so
 * "this was suspended in August for non-payment" survives being brought back.
 */
public record SchoolSuspendRequest(

        /** Example: "Non-payment. Third invoice unpaid past 60 days." */
        @NotBlank @Size(max = 500) String reason) {
}
