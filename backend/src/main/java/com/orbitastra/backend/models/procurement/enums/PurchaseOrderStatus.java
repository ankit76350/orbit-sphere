package com.orbitastra.backend.models.new_new.procurement.enums;

/**
 * Where an order placed with a vendor has got to.
 *
 * <p>DRAFT is editable. ISSUED is not: once the order has gone to the vendor it is a
 * commitment, and changing what it says afterwards would mean the school and the vendor
 * are holding different pieces of paper. A change after issue means cancelling and raising
 * a new order, so the change has a date and a reason on it.
 *
 * <p>SHORT_CLOSED is the state that matters most here, and it is the one a simpler design
 * would leave out. Two hundred kilograms were ordered, one hundred and eighty-five
 * arrived, and the school has decided that is the end of it. Without this state the order
 * sits at PARTIALLY_RECEIVED forever, and the fifteen kilograms nobody is chasing look
 * exactly like fifteen kilograms still on their way.
 *
 * <p>Goods received but not yet arrived is not a status here. It is the difference between
 * the ordered and received quantities on each line.
 */
public enum PurchaseOrderStatus {
    /** Being prepared. Still editable. */
    DRAFT,

    /** Approved internally, not yet sent to the vendor. */
    APPROVED,

    /** Sent to the vendor. Now a commitment, and no longer editable. */
    ISSUED,

    /** Some of the goods have arrived. */
    PARTIALLY_RECEIVED,

    /** Everything ordered has arrived. */
    RECEIVED,

    /** Less arrived than was ordered, and the school has decided to accept that. */
    SHORT_CLOSED,

    /** Called off. Nothing will arrive. */
    CANCELLED
}
