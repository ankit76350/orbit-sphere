package com.orbitastra.backend.dto.crm.admissioncycle.request;

import java.time.Instant;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * A new admission cycle. Endpoint #1.
 *
 * <p>An admission cycle is one round of admissions for one academic year — "Main intake", or
 * "Scholarship round". Everything else in this module hangs off it: an application has to name a
 * cycle, so this is the first call anybody makes.
 *
 * <p><b>The academic year IS in the body here</b>, unlike a class, where it comes from the URL.
 * The reason is in this module's README: for classes and timetables the year is the scope, but a
 * school opens next year's admissions while this year is still running, and often has two cycles
 * live at once. So the year is a normal field on the cycle, not a part of its address.
 *
 * <p><b>The year does not have to be the one the school is running.</b> That is the whole point of
 * this module. A school sets up its 2027-2028 cycle in the middle of 2026-2027.
 *
 * <p><b>The seat table is not accepted here.</b> Seats are set by #4, on their own. A cycle is
 * named and dated before anybody has worked out how many seats each class gets, and a create that
 * could fail on either a duplicate name or a bad seat row leaves the caller working out which.
 * Same shape as a class being created with no sections.
 *
 * <p><b>The status is not accepted either.</b> Every cycle starts as a DRAFT and is moved by #3.
 */
public record AdmissionCycleCreateRequest(

        /**
         * Which academic year this cycle admits students for. Example: "2027-2028"
         *
         * <p>This is the year's name, which is what every other collection stores. The year has to
         * already exist in this school. It does NOT have to be the year the school is running.
         */
        @NotBlank @Size(max = 40) String academicYear,

        /**
         * What the school calls this round. Example: "Main intake"
         *
         * <p>Has to be different from the other cycles in the same year. A school can run more
         * than one round for a year — a general intake and a scholarship round — so the name is
         * how staff tell them apart on screen.
         */
        @NotBlank @Size(max = 120) String name,

        /**
         * When the school starts taking enquiries. Example: "2026-10-01T00:00:00Z"
         *
         * <p>All four dates are optional, because a school often creates the cycle before it has
         * settled its calendar. When they are given, they have to be in this order:
         * enquiries open, applications open, applications close, enrollment deadline.
         */
        Instant inquiryOpenAt,

        /** When families can start applying. Example: "2026-11-01T00:00:00Z" */
        Instant applicationOpenAt,

        /** The last moment an application is taken. Example: "2027-01-31T18:29:59Z" */
        Instant applicationCloseAt,

        /**
         * The last moment an accepted family can enroll. Example: "2027-03-15T18:29:59Z"
         *
         * <p>After this the school gives the seat to somebody else. Nothing enforces that yet —
         * it is stored so the offer screens can show it.
         */
        Instant enrollmentDeadlineAt,

        /** Anything the school wants to remember about this round. Optional. */
        @Size(max = 2000) String notes) {
}
