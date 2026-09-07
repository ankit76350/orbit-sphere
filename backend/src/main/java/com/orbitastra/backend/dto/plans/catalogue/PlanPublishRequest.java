package com.orbitastra.backend.dto.plans.catalogue;

import java.time.Instant;

/**
 * The selling window, decided at the moment of publishing. Endpoint #4.
 *
 * <p><b>Publish is where the window is settled, because it is the last chance.</b> Publishing is
 * a one-way door — #2 refuses a plan that is not a draft, and there is no unpublish — so a
 * version that goes out with the wrong dates is wrong for the life of that version, and the only
 * fix is a whole new one. Asking here costs two fields; not asking costs a version.
 *
 * <p><b>Both fields have a defined absence, and neither means "leave the draft's alone".</b>
 *
 * <ul>
 *   <li>{@code effectiveFrom} not sent → <b>now</b>. A plan being published is a plan going on
 *       sale, so today is the only sensible default.</li>
 *   <li>{@code effectiveUntil} not sent → <b>null</b>, which means it sells until somebody
 *       retires it. That is the ordinary case: most plans have no end date.</li>
 * </ul>
 *
 * <p>So publishing decides the window outright rather than inheriting it. A date set on the draft
 * earlier and then not sent here is dropped — which is the point: one call, one place, one
 * answer, and no way to publish a window nobody looked at.
 */
public record PlanPublishRequest(

        /** When it goes on sale. Not sent means now. */
        Instant effectiveFrom,

        /** When it stops being sold. Not sent means never — it sells until retired. */
        Instant effectiveUntil) {

    /** A request that sends nothing, for a caller that posted no body. Both defaults apply. */
    public static PlanPublishRequest empty() {
        return new PlanPublishRequest(null, null);
    }
}
