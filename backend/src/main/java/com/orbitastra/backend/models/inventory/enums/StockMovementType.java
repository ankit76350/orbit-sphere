package com.orbitastra.backend.models.inventory.enums;

/**
 * Why the quantity of something changed.
 *
 * <p>Every row in the stock ledger has one of these, and the type is what says whether
 * the quantity went up or down. There is deliberately no separate direction field: two
 * fields saying the same thing can disagree, and a row claiming to be a RECEIPT that
 * reduces stock would be impossible to explain.
 *
 * <p>Goes up: RECEIPT, RETURN, TRANSFER_IN, ADJUSTMENT_INCREASE.
 * Goes down: ISSUE, CONSUMPTION, WASTAGE, TRANSFER_OUT, ADJUSTMENT_DECREASE.
 *
 * <p>WASTAGE and ADJUSTMENT_DECREASE both reduce stock and are kept apart on purpose.
 * Wastage is stock the school knows it lost and why: milk that went off, a bat that
 * snapped. An adjustment is the count being wrong with nobody able to say where the
 * difference went. A store whose adjustments are large is a store with a problem, and
 * lumping the two together hides it.
 */
public enum StockMovementType {
    /** Stock arrived, whether bought, donated or returned from a repair. */
    RECEIPT,

    /** Given out to a person or a department. */
    ISSUE,

    /** Something issued has come back. */
    RETURN,

    /** Left one store on its way to another. */
    TRANSFER_OUT,

    /** Arrived in a store from another one. */
    TRANSFER_IN,

    /** Used up in the ordinary course of things, such as food cooked for a meal. */
    CONSUMPTION,

    /** Lost, spoiled or broken, with a known reason. */
    WASTAGE,

    /** A stock count found more than the records said. */
    ADJUSTMENT_INCREASE,

    /** A stock count found less than the records said, and nobody knows why. */
    ADJUSTMENT_DECREASE
}
