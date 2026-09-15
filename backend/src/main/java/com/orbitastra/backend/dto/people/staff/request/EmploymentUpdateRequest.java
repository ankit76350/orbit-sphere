package com.orbitastra.backend.dto.people.staff.request;

import java.time.LocalDate;

import com.orbitastra.backend.models.people.staff.enums.EmploymentStatus;
import com.orbitastra.backend.models.people.staff.enums.EmploymentType;

import jakarta.validation.constraints.Size;

/**
 * Corrections to one employment record. Endpoint #18.
 *
 * <p><b>Every field is optional, and absent means "leave it alone".</b> An empty body is a
 * {@code 400 NOTHING_TO_UPDATE} rather than a no-op success.
 *
 * <h2>This corrects a record; it does not move anybody</h2>
 *
 * <p>The difference decides everything that is not here. A promotion, a transfer and a
 * resignation are <b>events</b> — they close one record and open another, or close one and set a
 * terminal status, and they are #16 and #17. A correction is for what was typed wrong on a record
 * that already describes the right thing.
 *
 * <h2>What this deliberately cannot change</h2>
 *
 * <p><b>Never {@code current}.</b> #16 and #17 move two records together; a PATCH that could set
 * this is exactly how two records end up current, or none do — and the second is worse, because
 * the person then reads as unemployed. The partial unique index would catch the first and nothing
 * catches the second.
 *
 * <p><b>Never {@code positionDocsId}.</b> Moving somebody to a different position is a transfer,
 * which is a new record, which is #16. Editing it in place would rewrite where they worked last
 * year — the same objection that keeps a position from changing department at #14.
 *
 * <p><b>Never {@code staffDocsId}.</b> A record belongs to the person it was written for.
 *
 * <p><b>Not {@code separationReason}</b>, which #17 sets as it ends an employment. Correcting one
 * is a real need and there is nothing to correct yet — #17 is not built.
 *
 * <h2>What can be cleared</h2>
 *
 * <pre>
 * "managerDocsId": ""   clears it — they report to nobody
 * "probationUntil": null  leaves it     (same as absent)
 * any field: null         leaves it
 * </pre>
 *
 * <p><b>{@code effectiveUntil} and {@code probationUntil} cannot be cleared</b>, for the reason
 * #2's enums cannot: {@code null} already means "leave it alone", and with no way to tell an
 * absent field from an explicit null there is nothing left to mean "remove it".
 */
public record EmploymentUpdateRequest(

        /**
         * A corrected start date.
         *
         * <p><b>Checked against the record's neighbours</b>, not just against itself: moving it
         * can push this record onto the one before it, and no index compares a start to a
         * previous end.
         */
        LocalDate effectiveFrom,

        /**
         * A corrected end date.
         *
         * <p><b>Refused on a record that is still current.</b> {@code EmploymentRecord} says this
         * is "null while this employment record remains current", and a record that is current
         * and ended at once is the same contradiction #16 refuses on {@code status}. Ending an
         * employment is #17.
         */
        LocalDate effectiveUntil,

        /** A corrected supervisor's {@code Staff.id}, or {@code ""} for nobody. */
        @Size(max = 60) String managerDocsId,

        /** A corrected probation end. Cannot be before the record's start. */
        LocalDate probationUntil,

        /**
         * A corrected status.
         *
         * <p><b>{@code TERMINATED} is refused on a current record</b> — current and finished at
         * once is what the module plan's open item 2 warns about, and #17 is what ends an
         * employment properly, setting {@code current} and a terminal status together.
         */
        EmploymentStatus status,

        /**
         * A corrected contract type.
         *
         * <p><b>Not in the plan's list, added 2026-09-15.</b> Full-time typed where part-time was
         * meant is a typo like any other, and there is no event endpoint that owns it — leaving it
         * out would mean the only fix was deleting the record, which this module has no way to do.
         */
        EmploymentType employmentType) {

    /** Whether the request asks for nothing at all. */
    public boolean isEmpty() {
        return effectiveFrom == null && effectiveUntil == null && managerDocsId == null
                && probationUntil == null && status == null && employmentType == null;
    }
}
