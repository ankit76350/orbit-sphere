package com.orbitastra.backend.models.new_new.identity.enums;

/**
 * One part of the application that permissions can be given for.
 *
 * <p>This list is fixed by the platform. Schools cannot add to it; they make new
 * roles instead. Each value lines up with a package of models, so a permission
 * check has an obvious home.
 *
 * <p>Fees are split into four rather than one, and that is on purpose. A school
 * usually wants the fee desk to raise bills and take money, but only the head to
 * allow a discount. One FINANCE module could not tell those two apart.
 */
public enum AppModule {
    /** Inquiries, applications, offers and admission reviews. */
    ADMISSIONS,

    /** Student records, guardians and academic records. */
    STUDENTS,

    /** Staff records, employment, leave and reviews. */
    STAFF,

    /** Classes, sections, subjects and curriculum documents. */
    ACADEMICS,

    /** Daily timetables and period allocation. */
    TIMETABLE,

    /** Attendance registers and daily attendance records. */
    ATTENDANCE,

    /** Homework and homework submissions. */
    HOMEWORK,

    /** Exams, datesheets, marks and report cards. */
    EXAMINATIONS,

    /** Fee heads, fee structures and invoices. */
    FEES_BILLING,

    /** Payments, allocations, refunds and wallets. */
    FEES_PAYMENTS,

    /** Concession policies and concession requests. */
    FEES_CONCESSIONS,

    /** Scholarship programmes, applications and awards. */
    FEES_AID,

    /** Payment gateways, mandates, settlements and bank accounts. */
    FEES_SETUP,

    /** Uploaded and generated documents, certificates and ID cards. */
    DOCUMENTS,

    /** Routes, vehicles, drivers, student allocations and boarding. */
    TRANSPORT,

    /** Visitors, gate movements and letting a child out during school hours. */
    GATE,

    /**
     * Health profiles, clinic visits, medicines given and vaccinations. Held apart
     * from STUDENTS on purpose: a class teacher needs a child's timetable but not
     * their medical notes, and only the alerts marked for it reach other screens.
     */
    HEALTH,

    /**
     * Incidents, discipline cases, the actions decided, and recognitions. Also held
     * apart from STUDENTS: what a child did wrong in Class VI should not be on
     * every screen for the rest of their time at the school.
     */
    CONDUCT,

    /** Books, copies, loans, reservations and the borrowing rules. */
    LIBRARY,

    /** Messages sent to families and staff. */
    NOTIFICATIONS,

    /** Reports and data exports. */
    REPORTS,

    /** User accounts, roles and who is allowed to do what. */
    USER_ACCESS,

    /** School details, academic years, holidays and number sequences. */
    SCHOOL_SETTINGS,

    /** The school's own subscription to this platform. */
    SUBSCRIPTION
}
