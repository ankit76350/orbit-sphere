package com.orbitastra.backend.models.new_new.inventory;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.new_new.base.SchoolBase;

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
 * that wants one flat list can leave it null throughout.
 *
 * <p>The tree is for reporting only. Stock is always counted against an item, never
 * against a category, so a category with children still has no quantity of its own.
 *
 * <p>The service checks that a category still used by an item is not deleted, and that a
 * parent chain does not loop back on itself.
 */
@Document(collection = "inventory_categories")
@CompoundIndexes({
        @CompoundIndex(
                name = "school_inventory_category_name_uniq",
                def = "{'schoolId': 1, 'name': 1}",
                unique = true),
        @CompoundIndex(
                name = "school_inventory_category_tree_idx",
                def = "{'schoolId': 1, 'parentCategoryDocsId': 1, 'sortOrder': 1}")
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

    // Links to another InventoryCategory.id when this sits under a broader one.
    // Null for a top-level group. Example: "67bc1122dc3f7d0011223344"
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
