package com.orbitastra.backend.models.new_new.payroll.enums;

/**
 * Which way a component moves money.
 *
 * <p>EMPLOYER_CONTRIBUTION is the one people forget. The school's half of provident fund
 * is a real cost to the school but is not deducted from the staff member and does not
 * reduce their take-home pay. Counting it as a deduction understates what somebody is
 * paid; leaving it out altogether understates what the school spends.
 *
 * <p>So: net pay is earnings minus deductions. Cost to the school is earnings plus
 * employer contributions. Two different questions, and this is what keeps them apart.
 */
public enum SalaryComponentType {
    /** Adds to pay. Basic, house rent allowance, transport allowance. */
    EARNING,

    /** Taken off pay. Provident fund, professional tax, income tax, an advance. */
    DEDUCTION,

    /** Paid by the school on top, never taken off the staff member. */
    EMPLOYER_CONTRIBUTION
}
