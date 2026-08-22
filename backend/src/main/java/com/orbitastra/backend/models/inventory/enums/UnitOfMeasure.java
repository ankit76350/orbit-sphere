package com.orbitastra.backend.models.new_new.inventory.enums;

/**
 * What an item is counted in.
 *
 * <p>Fixed by the platform rather than typed by each school, because a store where one
 * clerk writes "kg" and another writes "Kg" cannot be added up. A school that needs
 * something not here should say so; adding a value is a smaller problem than free text.
 *
 * <p>An item is stored in exactly one of these forever. Rice bought in 50 kg sacks and
 * issued by the kilogram is a KILOGRAM item that happens to arrive 50 at a time; the
 * sack is a packaging detail, not a second unit. Two units on one item is how stock
 * counts quietly go wrong.
 */
public enum UnitOfMeasure {
    /** Counted one by one. */
    PIECE,

    /** Weighed in kilograms. */
    KILOGRAM,

    /** Weighed in grams, for small quantities such as spices. */
    GRAM,

    /** Measured in litres. */
    LITRE,

    /** Measured in millilitres. */
    MILLILITRE,

    /** Measured in metres, such as cloth or cable. */
    METRE,

    /** Twelve of something. */
    DOZEN,

    /** A sealed packet treated as one unit. */
    PACKET,

    /** A box treated as one unit. */
    BOX,

    /** A bottle treated as one unit. */
    BOTTLE,

    /** A tied bundle, such as firewood or brooms. */
    BUNDLE,

    /** Two that belong together, such as shoes or gloves. */
    PAIR,

    /** Several that belong together, such as a geometry set. */
    SET
}
