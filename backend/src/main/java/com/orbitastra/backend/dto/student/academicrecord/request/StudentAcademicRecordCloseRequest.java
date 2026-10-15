package com.orbitastra.backend.dto.student.academicrecord.request;

import com.orbitastra.backend.models.student.enums.AcademicRecordStatus;

import jakarta.validation.constraints.NotNull;

/**
 * What #16 needs to close a child's academic record.
 *
 * <p>The record is named in the path. Closing it ends the placement: the record keeps its class,
 * section and dates, gets an end date and a final status, and stops being the child's current
 * record.
 *
 * <p><b>The end date is not sent.</b> The server sets it to the day the record is closed.
 *
 * <p><b>The record is never deleted.</b> It is where that part of the year's attendance and marks
 * are attached, so it stays in the child's history (#20).
 */
public record StudentAcademicRecordCloseRequest(

        /**
         * How the placement ended. <b>One of {@code COMPLETED}, {@code TRANSFERRED} or
         * {@code CANCELLED}</b> — {@code ACTIVE} is refused, because that is not closing.
         *
         * <ul>
         *   <li><b>{@code COMPLETED}</b> — the child finished the year in this class.</li>
         *   <li><b>{@code TRANSFERRED}</b> — the placement ended because the child moved. A move
         *       to another class or section in this school is #17, which also opens the new
         *       record; this is for a child who leaves with nowhere in this school to go.</li>
         *   <li><b>{@code CANCELLED}</b> — the placement should never have been made.</li>
         * </ul>
         */
        @NotNull AcademicRecordStatus status,

        /**
         * <b>The RECORD'S version</b>, not the child's — the record is the thing being closed.
         * #20 returns it on every row.
         */
        @NotNull Long version) {
}
