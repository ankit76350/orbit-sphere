package com.orbitastra.backend.models.new_new.feedback.enums;

/**
 * Who is allowed to read the feedback once it has been given.
 *
 * <p>Set on the topic, so it is a decision the school makes once for a kind of feedback
 * rather than something argued about every time somebody asks to see their results.
 *
 * <p>SUBJECT_AGGREGATE and SUBJECT_FULL are the pair worth thinking about. A teacher seeing
 * "your average was 4.2 from thirty-one responses" learns something useful and cannot go
 * looking for anybody. **A teacher reading thirty-one individual anonymous comments will
 * try to work out who wrote the unkind one**, and in a class they teach every day they will
 * often be right. That is not a hypothetical: it is the ordinary human response to being
 * criticised anonymously by people you can name.
 *
 * <p>So SUBJECT_AGGREGATE is the safe default for anything a student or parent says about a
 * member of staff, and SUBJECT_FULL is a deliberate decision for feedback that was never
 * anonymous in the first place.
 *
 * <p>Whichever is chosen, nothing reaches the subject until the response count passes
 * FeedbackTopic.minimumResponsesToReveal. An average of three responses in a class of five
 * is not anonymous arithmetic.
 */
public enum FeedbackVisibility {
    /** Only the coordinator and whoever holds the reviewing permission. */
    REVIEWER_ONLY,

    /** The reviewer, plus the subject's own head of department or line manager. */
    SUBJECT_MANAGER,

    /** The subject sees the numbers, and no individual comments. */
    SUBJECT_AGGREGATE,

    /** The subject sees the numbers and the comments as written. */
    SUBJECT_FULL
}
