package com.orbitastra.backend.models.undone.inventory.enums;

public enum InventoryTransactionType {

    /**
     * New stock purchased.
     */
    PURCHASE,

    /**
     * Issued to student.
     */
    ISSUE_TO_STUDENT,

    /**
     * Issued to teacher.
     */
    ISSUE_TO_TEACHER,

    /**
     * Issued to department.
     */
    ISSUE_TO_DEPARTMENT,

    /**
     * Returned.
     */
    RETURN,

    /**
     * Broken/Damaged.
     */
    DAMAGE,

    /**
     * Lost.
     */
    LOSS,

    /**
     * Manual stock correction.
     */
    ADJUSTMENT
}
