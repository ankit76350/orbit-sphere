package com.orbitastra.backend.models.academics.enums;

/**
 * What a grading scheme reads to decide a grade — the <b>input</b>, never the output.
 *
 * <p>This is the field that says what a {@code GradeBand}'s bounds are measured in, so it has to
 * name the thing a teacher enters. It does not name what comes back out: the awarded grade is
 * {@code GradeBand.gradeCode}, and what that grade is worth in an aggregate is
 * {@code GradeBand.gradePoint}. Both are mapped <i>from</i> a value on one of these scales.
 *
 * <p><b>{@code POINT} was renamed to {@code MARKS} on 2026-09-14</b>, because "point" read as a
 * grade point and a grade point is an output. A scheme whose input was a grade point would be
 * mapping a grade point onto a grade point, which is circular — and the IB fixture written against
 * that reading was wrong in exactly that way: it treated IB's 1–7, which is an <i>awarded grade</i>,
 * as though it were the number a teacher enters.
 */
public enum GradingScaleType {

    /**
     * A value already normalised to a percentage — {@code maximumValue} is 100.
     *
     * <p>What CBSE and ICSE internal reporting use, and what an IB scheme looks like once its
     * boundaries are expressed as percentages of the raw total.
     */
    PERCENTAGE,

    /**
     * A raw score out of a total that is not 100 — marks out of 50, out of 25, out of 80.
     *
     * <p>Mechanically the same walk as {@code PERCENTAGE}: both find the band whose bounds contain
     * the value. They are separate because the <i>reported</i> figure differs — a card showing
     * "43 / 50" is not a card showing "86%", and {@code ReportCardSubjectResult} carries
     * {@code maximumMarks} and {@code percentage} as two distinct fields for the same reason.
     */
    MARKS,

    /**
     * No numeric input at all — a teacher picks "Developing" directly.
     *
     * <p>CBSE's co-scholastic areas work this way. A band here carries a {@code gradeCode} and a
     * {@code description} and no bounds, and #8 cannot resolve such a scheme by value.
     */
    DESCRIPTOR
}
