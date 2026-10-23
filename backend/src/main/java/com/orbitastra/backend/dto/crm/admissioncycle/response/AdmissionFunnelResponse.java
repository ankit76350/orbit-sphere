package com.orbitastra.backend.dto.crm.admissioncycle.response;

import java.util.List;
import java.util.Map;

/**
 * What #34 answers: one admission cycle as a funnel — leads, applications, offers and enrolments.
 *
 * <p>{@code stages} is the funnel itself, in order, each a single number. The three
 * {@code ...ByStatus} maps are the raw counts behind it, <b>every status listed</b>, zeros
 * included, so a stage nobody has reached reads as {@code 0} rather than being missing.
 */
public record AdmissionFunnelResponse(

        String admissionCycleDocsId,

        String cycleName,

        String academicYear,

        String cycleStatus,

        /** The funnel, top to bottom. */
        List<Stage> stages,

        /** Inquiries of the cycle's academic year, per status. */
        Map<String, Long> inquiriesByStatus,

        /** Applications of this cycle, per status. */
        Map<String, Long> applicationsByStatus,

        /** Offers on this cycle's applications, per status, every revision. */
        Map<String, Long> offersByStatus,

        /** What the numbers do and do not mean, in a sentence a person can read. */
        String note) {

    /**
     * One step of the funnel.
     *
     * @param key   a stable name for code — {@code leads}, {@code applications}, …
     * @param label a name for a screen
     * @param count how many reached this step
     */
    public record Stage(String key, String label, long count) {
    }
}
