package com.orbitastra.backend.models.new_new.inventory.enums;

/**
 * Whether something lent out has come back.
 *
 * <p>Only used for NON_CONSUMABLE items. Issuing chalk is the end of the story and needs
 * no status; issuing a microscope is not.
 *
 * <p>NOT_RETURNED is the state that matters. A school that only had ISSUED and RETURNED
 * would leave everything unreturned sitting as ISSUED forever, so the list of things
 * nobody gave back would look identical to the list of things currently in use. The
 * difference is the whole point of keeping this.
 */
public enum StockIssueStatus {
    /** Out with somebody, and not yet due back. */
    ISSUED,

    /** Some came back and some did not. */
    PARTIALLY_RETURNED,

    /** All of it came back. */
    RETURNED,

    /** Past its due date and still out. */
    OVERDUE,

    /** Accepted as gone, and written off the books. */
    NOT_RETURNED,

    /** Came back broken and is being written off rather than shelved. */
    RETURNED_DAMAGED
}
