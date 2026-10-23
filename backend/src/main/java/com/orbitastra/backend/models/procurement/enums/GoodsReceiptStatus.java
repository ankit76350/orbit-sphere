package com.orbitastra.backend.models.procurement.enums;

/**
 * What the school decided about a delivery.
 *
 * <p>DRAFT exists because unloading and checking are not the same moment. A lorry arrives,
 * the store keeper writes down what came off it, and only then does somebody look at
 * whether the vegetables are any good. Stock does not move until the receipt leaves DRAFT.
 *
 * <p>PARTIALLY_REJECTED is the ordinary case for food, not an exception. Forty kilograms
 * of tomatoes arrive and six are soft. The school takes thirty-four and sends six back,
 * and it must be able to bill the vendor for thirty-four.
 *
 * <p>Only the accepted quantity ever becomes stock. That is the rule this whole enum
 * exists to support: rejected goods were never in the school's possession in any sense
 * that a store balance should reflect.
 */
public enum GoodsReceiptStatus {
    /** Written down as it came off the lorry. Nothing has moved into stock yet. */
    DRAFT,

    /** Everything was taken, and stock has gone up. */
    ACCEPTED,

    /** Some was taken and some sent back. Only the accepted part became stock. */
    PARTIALLY_REJECTED,

    /** Nothing was taken. The whole delivery went back. */
    REJECTED,

    /** Entered by mistake. Any stock it created has been reversed. */
    CANCELLED
}
