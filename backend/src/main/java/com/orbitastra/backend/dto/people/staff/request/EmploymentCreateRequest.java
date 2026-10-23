package com.orbitastra.backend.dto.people.staff.request;

import java.time.LocalDate;

import com.orbitastra.backend.models.people.staff.enums.EmploymentStatus;
import com.orbitastra.backend.models.people.staff.enums.EmploymentType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Hire, promote or transfer somebody. Endpoint #16.
 *
 * <h2>One endpoint, because it is one event</h2>
 *
 * <p>Hiring, promoting and transferring are the same write: close the record that was current and
 * open a new one. <b>Three endpoints doing this would be three chances to leave two records
 * current</b> — or none, which is worse, because the person then reads as unemployed.
 *
 * <h2>What is not here</h2>
 *
 * <p><b>No {@code current}.</b> The record this creates is the current one by definition; that is
 * what the endpoint means. A caller that could send {@code false} could create a historical record
 * out of nowhere, which is #18's job to correct and nobody's to invent.
 *
 * <p><b>No {@code effectiveUntil}.</b> A current record has not ended. The previous one's end is
 * computed — the day before this one starts — rather than taken from the caller, because two
 * people typing two dates is how a gap or an overlap gets in.
 *
 * <p><b>No {@code separationReason}.</b> Leaving is #17, and it is the one way somebody leaves.
 *
 * <h2>status is required, and it is not decoration</h2>
 *
 * <p>Defaulting it to {@code ACTIVE} would silently mark somebody as working here when the school
 * meant {@code PROBATION}, and the difference matters to anything reading a teacher picker.
 *
 * <p><b>A terminal status is refused</b> — {@code TERMINATED} and {@code RETIRED}. A record that is
 * {@code current} and terminal at the same time is the contradiction the module plan's open item 2
 * warns about; nothing in the model stops it, so this endpoint does. The check asks
 * {@link EmploymentStatus#isTerminal()} rather than naming values, which is why {@code RETIRED}
 * was covered the day it was added.
 *
 * <p><b>{@code OFFERED} was removed from the enum on 2026-09-16.</b> It meant "accepted an offer,
 * has not started"; a future {@code effectiveFrom} is how that is said now.
 */
public record EmploymentCreateRequest(

        /** The seat they are being put in. Must be a position of this school, and <b>active</b>. */
        @NotBlank @Size(max = 60) String positionDocsId,

        /** Whether this is an offer, a probation, an active job. {@code TERMINATED} is refused. */
        @NotNull EmploymentStatus status,

        /** Full-time, part-time, contract, temporary, substitute or intern. */
        @NotNull EmploymentType employmentType,

        /**
         * The day this employment begins.
         *
         * <p><b>It may be in the past or the future.</b> A school entering last April's hires is
         * the ordinary case, and an offer with a start date next term is the other one.
         */
        @NotNull LocalDate effectiveFrom,

        /**
         * Their supervisor's {@code Staff.id}, or null.
         *
         * <p>A person, not a seat — {@code Position.reportsToPositionDocsId} answers the
         * structural question and this answers "who do I actually report to", which is why both
         * exist. Validated to be this school's, and refused when it is the person themselves.
         */
        @Size(max = 60) String managerDocsId,

        /** When probation ends, if there is one. Cannot be before {@code effectiveFrom}. */
        LocalDate probationUntil) {
}
