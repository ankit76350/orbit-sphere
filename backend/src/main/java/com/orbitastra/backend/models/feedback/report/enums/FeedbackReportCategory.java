package com.orbitastra.backend.models.feedback.report.enums;

/**
 * What a report sent straight to the head is about.
 *
 * <p>The channel is deliberately open — anybody may report anything at any time — but the
 * category is a fixed list, because it is what decides **who receives it and how fast.** A
 * report about a child being bullied and a suggestion about the canteen menu should not land
 * in the same queue at the same speed, and free text cannot be routed.
 *
 * <p>OTHER exists, and it breaks the usual rule against catch-all values. It is right here
 * because the promise being made to the reporter is "tell us anything", and a person with
 * something to say who cannot find a category that fits will either force it into the wrong
 * one or say nothing. The subject line carries what the category could not. A channel with
 * many OTHER reports is telling the school its category list is wrong.
 *
 * <p>HARASSMENT_OR_BULLYING and SAFETY_CONCERN are in the list even though this module does
 * **not** investigate either of them. They are here precisely so that they are recognised at
 * the moment they arrive, routed to the right person within the hour rather than the week, and
 * escalated out of this module deliberately instead of sitting in OTHER until somebody reads
 * down the queue. Receiving something properly and handling it are two different jobs; this
 * package does the first.
 */
public enum FeedbackReportCategory {
    /** An idea for making something better. Nothing is wrong. */
    SUGGESTION,

    /** Something the school did well, and who did it. */
    APPRECIATION,

    /** A concern about how a member of staff behaved or taught. */
    STAFF_CONCERN,

    /** A concern about a student's behaviour towards others. */
    STUDENT_CONCERN,

    /**
     * Somebody is being bullied, harassed or targeted. Routed fastest, and escalated out
     * of this module. Nothing here investigates it.
     */
    HARASSMENT_OR_BULLYING,

    /**
     * Something is unsafe: a broken railing, a gate left open, a driver on the phone.
     * Routed fastest, because the point of reporting it is to stop it before it happens.
     */
    SAFETY_CONCERN,

    /** Teaching, syllabus, homework load, exams, marking. */
    ACADEMIC_CONCERN,

    /** Buildings, classrooms, toilets, water, electricity, cleanliness. */
    FACILITY_CONCERN,

    /** Buses, routes, timings, drivers, attendants. */
    TRANSPORT_CONCERN,

    /** Food quality, quantity, hygiene, the kitchen, the dining hall. */
    FOOD_CONCERN,

    /** Fees, receipts, refunds, a charge nobody can explain. */
    FEE_CONCERN,

    /** Something the school holds about a person, or who can see it. */
    PRIVACY_CONCERN,

    /** Money or property being misused. */
    FINANCIAL_MISCONDUCT,

    /** Anything the list above does not fit. The subject line says what it is. */
    OTHER
}
