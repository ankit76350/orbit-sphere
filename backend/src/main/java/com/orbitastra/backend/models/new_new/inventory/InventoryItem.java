package com.orbitastra.backend.models.new_new.inventory;

import java.math.BigDecimal;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

import com.orbitastra.backend.models.new_new.base.SchoolBase;
import com.orbitastra.backend.models.new_new.inventory.enums.InventoryItemType;
import com.orbitastra.backend.models.new_new.inventory.enums.UnitOfMeasure;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * One thing the school keeps in stock.
 *
 * <p>Rice, A4 paper, chalk, floor cleaner, bedsheets, footballs, test tubes, light bulbs.
 * One row each, set up once and used for years.
 *
 * <p>**This model holds no quantity.** That is the single most important thing about it,
 * and it is where the reference sketch went wrong: it kept a {@code stockQuantity} here,
 * which cannot say that 50 kg of rice is in the kitchen and 20 kg is in the main store.
 * Quantities live on StockBalance, one row per item per store, so a kitchen can be told
 * it is running out while a sack sits in another building.
 *
 * <p>{@code itemType} decides how the item behaves everywhere else:
 *
 * <ul>
 * <li>CONSUMABLE is issued and gone. Chalk, rice, detergent.</li>
 * <li>NON_CONSUMABLE is expected back, so issuing it opens a StockIssue somebody has to
 * close. Footballs, microscopes, bedsheets.</li>
 * <li>PERISHABLE needs batches with expiry dates, and the oldest batch goes first.
 * Milk, vegetables, eggs.</li>
 * </ul>
 *
 * <p>{@code unitOfMeasure} is fixed for the life of the item. Rice bought in 50 kg sacks
 * and issued by the kilogram is a KILOGRAM item that happens to arrive fifty at a time;
 * the sack is packaging, not a second unit. Two units on one item is how stock counts
 * quietly stop adding up.
 *
 * <p>{@code reorderLevel} is what turns a store into something that warns instead of
 * simply running out. It is a total across all stores, because the question it answers is
 * whether the school needs to buy more.
 *
 * <p>{@code lastPurchaseRate} is only the most recent price paid, kept so somebody
 * raising a purchase has a starting figure. It is not what the stock on hand is worth:
 * each receipt records its own rate, so valuation comes from the movements.
 *
 * <p>The service checks that {@code requiresBatchTracking} is true for every PERISHABLE
 * item, that an item with stock anywhere is never deleted, and that the unit is never
 * changed once movements exist.
 */
@Document(collection = "inventory_items")
@CompoundIndexes({
        @CompoundIndex(
                name = "school_inventory_item_code_uniq",
                def = "{'schoolId': 1, 'itemCode': 1}",
                unique = true),
        @CompoundIndex(
                name = "school_inventory_item_name_idx",
                def = "{'schoolId': 1, 'name': 1}"),
        @CompoundIndex(
                name = "school_inventory_item_category_idx",
                def = "{'schoolId': 1, 'inventoryCategoryDocsId': 1, 'active': 1}"),
        @CompoundIndex(
                name = "school_inventory_item_type_idx",
                def = "{'schoolId': 1, 'itemType': 1, 'active': 1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryItem extends SchoolBase {

    // The school's own code for this item, quoted on a purchase order and written on a
    // store card. Do not rename it once movements exist. Example: "PROV-RICE-01"
    @NotBlank
    private String itemCode;

    // Name staff see. Example: "Rice, Sona Masoori"
    @NotBlank
    private String name;

    // Links to InventoryCategory.id. Example: "67bc1122dc3f7d0011223344"
    @NotBlank
    private String inventoryCategoryDocsId;

    // How this item behaves. Decides whether issuing it expects a return, and whether
    // it needs expiry dates. Example: InventoryItemType.CONSUMABLE
    @NotNull
    private InventoryItemType itemType;

    // What it is counted in, for the life of the item. Example: UnitOfMeasure.KILOGRAM
    @NotNull
    private UnitOfMeasure unitOfMeasure;

    // Example: "Medium grain, used for the daily lunch."
    private String description;

    // Whether stock of this item has to be tracked in batches with expiry dates.
    // Always true for PERISHABLE, and useful for anything that can be recalled.
    // Example: false
    @NotNull
    @Builder.Default
    private Boolean requiresBatchTracking = false;

    // Total across all stores below which somebody should buy more. Null means the
    // school does not want a warning for this item. Example: 100.00
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal reorderLevel;

    // How much to buy when it runs low, so the person ordering does not have to guess.
    // Example: 500.00
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal reorderQuantity;

    // The most recent price paid per unit, kept as a starting figure for whoever raises
    // the next purchase. Not used to value stock on hand. Example: 62.50
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal lastPurchaseRate;

    // Example: "INR"
    @NotBlank
    private String currencyCode;

    // How many days after issue a NON_CONSUMABLE item is normally due back. Null for
    // items nobody expects back on a schedule, such as a bedsheet. Example: 7
    private Integer defaultReturnDays;

    // Links to DocumentRecord.id for a photograph, which helps a store keeper match a
    // name to a thing on a shelf. Example: "67bc1123dc3f7d0022334455"
    private String imageDocumentDocsId;

    // Who the school usually buys this from, as plain text. Proper vendor records
    // belong to a procurement module that is not built.
    // Example: "Shree Traders, Dadar"
    private String usualSupplierName;

    // Where it normally sits, written for a store keeper to find it.
    // Example: "Rack 3, bottom shelf"
    private String storageNote;

    // Whether this item may still be received or issued. Turning it off leaves the
    // existing stock and its history alone. Example: true
    @NotNull
    @Builder.Default
    private Boolean active = true;
}
