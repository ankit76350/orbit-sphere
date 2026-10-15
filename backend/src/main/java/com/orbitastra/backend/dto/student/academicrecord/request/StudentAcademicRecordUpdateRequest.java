package com.orbitastra.backend.dto.student.academicrecord.request;

import java.time.LocalDate;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * What #15 corrects on a child's <b>open</b> academic record.
 *
 * <h2>A correction, not an event</h2>
 *
 * <p>This is the typo endpoint: a roll number entered wrongly, a start date a day out. <b>It
 * changes what the record always should have said</b>, which is why it is a {@code PATCH} and why
 * it writes no history.
 *
 * <p><b>Everything that is an event has its own endpoint</b>, because each writes a second
 * document or a second record:
 *
 * <ul>
 *   <li><b>The class is #17</b>, which closes this record and opens another. Moving between
 *       classes is never a typo — it is a thing that happened on a day.</li>
 *   <li><b>The status is #16</b>, which also clears the child's pointer.</li>
 *   <li><b>{@code effectiveUntil} belongs to a closed record</b>, and this one is open by
 *       definition.</li>
 * </ul>
 *
 * <h2>The section is here <i>and</i> on #17, and the difference is what happened</h2>
 *
 * <p>Both can change it, and choosing between them is not a style question:
 *
 * <ul>
 *   <li><b>#15 — somebody typed B when the child has always been in C.</b> The record is corrected
 *       in place and <b>no history is written</b>, because there was never a day the child sat in
 *       B.</li>
 *   <li><b>#17 — the child was in B and is now in C.</b> The old record closes as
 *       {@code TRANSFERRED} with an end date, and a new one opens. <b>That boundary is what the
 *       first half of the year's attendance hangs off.</b></li>
 * </ul>
 *
 * <p><b>Using this one for a real move silently erases where the child was.</b> It is the one way
 * to misuse this endpoint, and nothing can detect it — only the caller knows which happened.
 *
 * <p>The class is deliberately not offered here, which keeps the mistake small: a correction can
 * put a child in the wrong section of the right class, never in the wrong class.
 *
 * <h2>Only the current record can be edited</h2>
 *
 * <p>The caller sends the child <b>and</b> the {@code academicRecordDocsId} they are editing. It
 * has to be the record {@code Student.currentAcademicRecordDocsId} points to — anything else is
 * {@code 409 RECORD_NOT_CURRENT} — and that record must be in the path's year and {@code ACTIVE}.
 *
 * <p><b>A closed record cannot be corrected through here at all.</b> A typo in last term's roll
 * number is unreachable, which is a real limitation rather than an oversight.
 */
public record StudentAcademicRecordUpdateRequest(

        /** The child whose open record is being corrected. */
        @NotBlank @Size(max = 60) String studentDocsId,

        /**
         * The academic record being edited. <b>Required.</b>
         *
         * <p><b>It must be the child's current record</b> — the one
         * {@code Student.currentAcademicRecordDocsId} points to. Any other id, a closed
         * {@code TRANSFERRED} row included, is refused with {@code 409 RECORD_NOT_CURRENT}, so an
         * Edit pressed on an old row never quietly corrects a different record.
         *
         * <p>The record is then read by this id and must be in the year from the path and still
         * {@code ACTIVE}.
         */
        @NotBlank @Size(max = 60) String academicRecordDocsId,

        /**
         * The section it should have said. <b>Absent leaves it alone.</b>
         *
         * <p><b>It has to exist on the class the record already names</b>, and be active — the
         * same two checks #14 and #17 make, because a section lives inside a class document and a
         * letter that is valid in 7 need not be valid in 8.
         *
         * <p><b>The class itself cannot be changed here</b>, so this moves a child within their
         * class and no further. Across classes is #17.
         *
         * <p><b>Use it only for a typo.</b> A child who actually changed section is #17 — see the
         * class note above for why that distinction is the caller's to make and nobody else's.
         */
        @Size(max = 20) String sectionNo,

        /**
         * The roll number it should have said. <b>Absent leaves it alone.</b>
         *
         * <p><b>{@code ""} clears it</b>, the convention every optional string in this project
         * follows — and a record with no roll number is a real state, so clearing one is a real
         * correction rather than a way of saying "I do not know".
         *
         * <p>Still scoped to the section — and to the <b>new</b> one when {@code sectionNo} is
         * being corrected in the same request, because that is where the number will live.
         * <b>Their own record never blocks them</b>, which matters when the correction is a change
         * of case or spacing.
         */
        @Size(max = 20) String rollNo,

        /**
         * The day the placement should have started. <b>Absent leaves it alone.</b>
         *
         * <p><b>It cannot be in the past</b>, the same rule #14 and #17 carry — which makes this
         * field narrower than it looks: a start date typed as last Tuesday cannot be corrected to
         * the right day last Monday, only forward. Said here because it is the one thing about
         * this endpoint that will surprise somebody.
         */
        @FutureOrPresent LocalDate effectiveFrom,

        /**
         * <b>The RECORD'S version</b>, not the child's.
         *
         * <p>That differs from #17 on purpose, and the difference follows the write: a transfer
         * repoints the child, so the child is what it guards; <b>a correction touches the record
         * and nothing else</b>.
         *
         * <p>#20 returns it on every row, which is where a caller gets it.
         */
        @NotNull Long version) {
}
