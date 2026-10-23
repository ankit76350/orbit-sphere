package com.orbitastra.backend.models.feedback.enums;


/**
 * What a piece of feedback is about.
 *
 * <p>An enum rather than free text, for the same reason every other type field in this
 * system is: "teacher", "Teacher" and "teaching staff" cannot be added up, and a report on
 * how staff are rated has to be able to find all of it.
 *
 * <p>SCHOOL carries no subject id. It is for feedback about the place rather than about
 * anybody in it — "the toilets on the second floor are never clean". That has to have
 * somewhere to go, or it gets filed against whichever member of staff the submitter
 * happened to think of.
 *
 * <p>STUDENT is here because it was asked for, and it is the value to be careful with. See
 * FeedbackTopic and the README: an accusation nobody can answer is a different thing when
 * the person accused is twelve years old.
 */
public enum FeedbackSubjectType {
    /** One member of staff. Points at Staff.id. Teaching quality, behaviour, helpfulness. */
    STAFF,

    /** One student. Points at Student.id. */
    STUDENT,

    /** The school itself. No subject id: cleanliness, safety, how the office answers. */
    SCHOOL,

    /** One department. Points at Department.id. */
    DEPARTMENT,

    /** A building, room or facility. Points at the relevant record where one exists. */
    FACILITY,

    /** A service the school runs: transport, the mess, the library, the clinic. */
    SERVICE
}
