package com.orbitastra.backend.models.inventory.enums;

/**
 * How an item behaves, which decides almost everything else about it.
 *
 * <p>This is the most important field on an item. A school's stock is not one kind of
 * thing: a sack of rice, a microscope and a bedsheet all sit in a store, and treating
 * them the same way gets all three wrong.
 *
 * <p>What each type changes:
 *
 * <ul>
 * <li>CONSUMABLE is used up and never comes back. Chalk, printer paper, cleaning
 * liquid. Issuing it is the end of the story.</li>
 * <li>NON_CONSUMABLE is expected back. A football, a microscope, a bedsheet. Issuing
 * it starts a StockIssue that somebody has to close.</li>
 * <li>PERISHABLE goes off. Milk, vegetables, eggs. It needs batches with expiry
 * dates, and the oldest batch has to be used first.</li>
 * </ul>
 */
public enum InventoryItemType {
    /** Used up and gone. Chalk, paper, detergent, rice. */
    CONSUMABLE,

    /** Expected back after use. Sports kit, lab apparatus, bedsheets, uniforms. */
    NON_CONSUMABLE,

    /** Goes off. Milk, vegetables, eggs, bread. Needs expiry dates. */
    PERISHABLE
}
