package com.orbitastra.backend.dto.student.academicrecord.request;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * What #17 needs to transfer a child to another class or section.
 *
 * <h2>Why this is not a {@code PATCH} of the class</h2>
 *
 * <p><b>Two reasons, and the second is the one that matters.</b>
 *
 * <p>The first is mechanical: {@code school_year_student_active_academic_record_uniq} forbids two
 * {@code ACTIVE} records for one child in one year, so a close and an open cannot be two requests
 * — for an instant between them the child is either in two places or in none.
 *
 * <p>The second is that <b>editing the class in place erases where the child sat for the first
 * half of the year</b>, which is exactly what that half's attendance and marks are attached to. A
 * transfer is a new fact, not a correction of an old one. Correcting a placement that was simply typed
 * wrongly is #15.
 *
 * <h2>It takes the child, not the record</h2>
 *
 * <p>The plan puts the record id in the path. This takes {@code studentDocsId} and finds the open
 * record itself, because that is the question a caller actually has — <i>"transfer this child to 7B"</i>
 * — and because the record id is a thing they would have to look up first in order to say it.
 *
 * <p><b>A child with no open record is not refused.</b> There is nothing to close, so the transfer is
 * a first placement, and the response says which of the two happened.
 */
public record StudentAcademicRecordTransferRequest(

        /**
         * The child being transferred.
         *
         * <p>The year is the path, and the record is found from the two — so this is the only id
         * that has to be sent.
         */
        @NotBlank @Size(max = 60) String studentDocsId,

        /**
         * The class to transfer into. <b>Optional: absent keeps the current one</b>, which makes this
         * a section transfer within a class — the common case, and the one a school does most often.
         *
         * <p>When the child has no open record there is no current class to default to, and this
         * becomes required.
         */
        @Size(max = 60) String classDocsId,

        /**
         * The section to transfer into. <b>Required</b>, because a transfer with no destination is not a
         * transfer.
         */
        @NotBlank @Size(max = 20) String sectionNo,

        /**
         * The roll number in the new section. <b>Optional, and not generated</b> — the same
         * limitation #14 has, for the same reason.
         *
         * <p><b>It does not carry over.</b> Roll numbers are scoped to a section, so the number a
         * child held in 7A says nothing about what is free in 7B — carrying it silently would
         * either collide or claim a number nobody assigned.
         */
        @Size(max = 20) String rollNo,

        /**
         * The day the new placement starts. <b>Defaults to today.</b>
         *
         * <p><b>It is also the old record's {@code effectiveUntil}</b>, so the two meet rather
         * than leaving a gap. The boundary day belongs to both records, which is the plan's
         * reading: a child who changes section on the 14th was in the old one that morning.
         */
        LocalDate effectiveFrom,

        /**
         * <b>The CHILD'S version</b>, as the caller last read it.
         *
         * <p><b>Required, and checked before anything is written.</b> A transfer touches two
         * documents and cannot be half-done — so the one check that can refuse it has to happen
         * before the first save, not between the two.
         *
         * <p><b>Why the child and not the record.</b> The record being closed is found by this
         * endpoint rather than named by the caller, so a version for it would be a number they
         * never saw. The child is what they were looking at, what carries
         * {@code currentAcademicRecordDocsId}, and the document this write repoints.
         *
         * <p><b>What it actually catches</b> is two people acting on one child at once: a second
         * transfer, or a placement by #14, between the caller's read and their write. Either would
         * leave the first caller transferring a child out of a section they are no longer in.
         *
         * <p>Leaving it out is {@code 400 VALIDATION_FAILED} rather than a silent
         * last-write-wins — which here would mean a closed record nobody can account for.
         */
        @NotNull Long version) {
}
