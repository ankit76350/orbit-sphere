package com.orbitastra.backend.models.new_new.payroll.enums;

/**
 * How far one person's payslip for one month has got.
 *
 * <p>It usually follows the run it belongs to, but not always, and WITHHELD is why this is
 * a separate field. One member of staff's pay can be held back while everybody else is
 * paid: a bank account that does not verify, a dispute, somebody who left mid-month with
 * an unsettled advance. Without this, holding one person back would mean holding the whole
 * school's payroll.
 */
public enum PayslipStatus {
    /** Worked out and still changeable. */
    DRAFT,

    /** Part of an approved run. The figures are fixed. */
    APPROVED,

    /** Paid. */
    PAID,

    /** Deliberately held back while the rest of the run was paid. */
    WITHHELD,

    /** Cancelled, with a reason recorded. */
    CANCELLED
}
