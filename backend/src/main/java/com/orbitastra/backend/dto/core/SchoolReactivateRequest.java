package com.orbitastra.backend.dto.core;

import jakarta.validation.constraints.Size;

/**
 * An optional note on why a school is being let back in.
 *
 * <p>Optional, unlike the reason on suspend, and the asymmetry is deliberate. Blocking a whole
 * school is the act that has to be justified — somebody will be asked why, possibly months
 * later. Letting one back in usually means the thing that caused the suspension was settled,
 * and forcing a sentence there produces "resolved" typed a hundred times, which is worse than
 * an empty field because it looks like a record and is not.
 *
 * <p>When given, it replaces {@code School.statusReason}. When omitted, the suspension reason
 * stays — so a reactivated school still carries the note explaining why it was suspended, which
 * is the useful default.
 *
 * <p>The whole body may be omitted too; the controller accepts a request with no body at all.
 */
public record SchoolReactivateRequest(

        /** Example: "Outstanding invoices cleared on 27 August." */
        @Size(max = 500) String note) {
}
