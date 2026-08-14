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

    /** Uploaded and generated documents. */
    DOCUMENTS,

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
