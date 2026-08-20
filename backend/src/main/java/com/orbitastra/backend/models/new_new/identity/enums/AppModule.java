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
 *
 * <p>Buying is split in two for the same reason, and it is the more important of the two
 * splits. A store keeper raises requests and signs for deliveries; releasing money to a
 * vendor is somebody else's job. One PROCUREMENT module would let the person who orders the
 * goods also pay for them, which is the arrangement every audit is looking for.
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

    /** Books, copies, issues, reservations and the borrowing rules. */
    LIBRARY,

    /**
     * Buildings, rooms, beds, who sleeps where, leave and the nightly roll call.
     * A warden needs this and a class teacher does not.
     */
    HOSTEL,

    /** Halls, meal times, menus and who ate. */
    MESS,

    /**
     * Every store in the school and everything in them: stationery, food, linen,
     * sports kit, lab apparatus, cleaning supplies and maintenance materials.
     */
    INVENTORY,

    /**
     * Vendors, requests to buy, purchase orders and goods received. Everything up to the
     * point where the school owes somebody money.
     */
    PROCUREMENT,

    /**
     * Supplier bills and the payments that settle them. Split from PROCUREMENT for the same
     * reason the fee modules are split: a store keeper should be able to raise a request and
     * sign for a delivery, and must not be able to release money to the vendor who made it.
     */
    PROCUREMENT_PAYMENTS,

    /**
     * Salary structures, monthly payroll and payslips. Held well apart from STAFF: a head
     * of department needs a colleague's timetable and must never see their pay.
     */
    PAYROLL,

    /**
     * Government identity numbers, parental consents, and what the school owes to boards
     * and authorities. Separate from STUDENTS because a class teacher has no business
     * reading a child's Aadhaar.
     */
    COMPLIANCE,

    /**
     * Event albums and photographs. Publishing is separate from uploading on purpose:
     * pictures of children should not go up because one person had a camera.
     */
    GALLERY,

    /**
     * Extra help with learning: identified needs, the plan, the accommodations, and whether
     * the sessions actually happened. Held apart from STUDENTS because a child's learning
     * difficulty is theirs, not something every screen should announce. Safeguarding is not
     * in this module and needs narrower access than a role can give.
     */
    SUPPORT,

    /**
     * Feedback topics, drives and what came in. Reading raw submissions is a narrower
     * thing than reading the summaries, and revealing who wrote a confidential one is
     * narrower still: those are separate PermissionAction levels on this module, not
     * separate modules.
     */
    FEEDBACK,

    /** Messages sent to families and staff. */
    NOTIFICATIONS,

    /** Reports and data exports. */
    REPORTS,

    /**
     * The audit trail. Reading it is its own permission and a narrow one: somebody who can
     * see who did what can see a great deal about everybody, and the people who most need
     * watching are often the ones with the widest access.
     */
    AUDIT,

    /** User accounts, roles and who is allowed to do what. */
    USER_ACCESS,

    /** School details, academic years, holidays and number sequences. */
    SCHOOL_SETTINGS,

    /** The school's own subscription to this platform. */
    SUBSCRIPTION
}
