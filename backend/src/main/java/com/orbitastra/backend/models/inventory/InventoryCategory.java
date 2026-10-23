package com.orbitastra.backend.models.inventory;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.base.SchoolBase;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * One group of things the school stocks, as the school divides them up.
 *
 * <p>Stationery, Kitchen Provisions, Housekeeping, Sports, Laboratory, Electrical, Hostel
 * Linen. A collection rather than a fixed list because no two schools group their stores
 * the same way, and the grouping is what every stock report is built on.
 *
 * <p>{@code parentCategoryDocsId} lets a school go one level deeper where it helps:
 * Kitchen Provisions holding Grains, Vegetables and Dairy. It is optional, and a school
 * that wants one flat list leaves it null throughout.
 *
 * <p>It earns its place because a category and a store answer different questions. A
 * category says what a thing **is**; a store says where it **is kept**. Cleaning supplies
 * may sit in three different stores and still all be housekeeping spending, and a school
 * with a single main store gets no grouping from the store at all. Neither axis can stand
 * in for the other.
 *
 * <p>A separate sub-category model was considered instead and not taken. It would move the
 * problem onto InventoryItem, which would then have to point at either a category or a
 * sub-category: carrying both lets them disagree, carrying only the sub-category forces a
 * dummy one under every category, and a flag to choose between them is worse than either.
 * Items are used far more than categories, so ambiguity there costs more than one nullable
 * field here.
 *
 * <p>The tree is for grouping and reporting only. Stock is never counted against a
 * category; it is always against an item in a store, so a category with children still has
 * no quantity of its own.
 *
 * <p>The service checks that a category still used by an item is not deleted, and that a
 * parent chain does not loop back on itself. A category that is its own grandparent would
 * hang any report that walks the tree.
 */
@Document(collection = "inventory_categories")
@CompoundIndexes({
        @CompoundIndex(
                name = "school_inventory_category_name_uniq",
                def = "{'schoolId': 1, 'name': 1}",
                unique = true),
        @CompoundIndex(
                name = "school_inventory_category_tree_idx",
                def = "{'schoolId': 1, 'parentCategoryDocsId': 1, 'sortOrder': 1}"),
        @CompoundIndex(
                name = "school_inventory_category_active_idx",
                def = "{'schoolId': 1, 'active': 1, 'sortOrder': 1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryCategory extends SchoolBase {

    // Name staff pick from. Example: "Kitchen Provisions"
    @NotBlank
    private String name;

    // Links to another InventoryCategory.id when this sits under a broader one. It is
    // another row in this same collection, never this row itself. Null for a top-level
    // group. Example: "67bc1122dc3f7d0011223344"
    private String parentCategoryDocsId;

    // Example: "Rice, pulses, oil, spices and other dry provisions."
    private String description;

    // Order this group appears in on screens. Example: 20
    @Builder.Default
    private Integer sortOrder = 0;

    // Whether new items may still be filed here. Example: true
    @NotNull
    @Builder.Default
    private Boolean active = true;
}
