package com.orbitastra.backend.models.inventory;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.base.SchoolBase;
import com.orbitastra.backend.models.inventory.enums.StoreType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * One place where stock is physically kept.
 *
 * <p>A school does not have one store, it has several: a main store, the kitchen store,
 * each hostel's own cupboard, the lab, the sports room. This model is what makes the
 * difference between "we have 70 kg of rice" and the far more useful "50 kg in the
 * kitchen store and 20 kg in the main store".
 *
 * <p>That distinction is the whole reason this collection exists. The reference sketch
 * kept a single {@code stockQuantity} on the item itself, which cannot answer where
 * anything actually is, and therefore cannot tell a kitchen it is about to run out while
 * a sack sits in another building.
 *
 * <p>{@code keeperStaffDocsId} is who answers for what is in it. A store where a count
 * comes out short and nobody is answerable is a store that keeps coming out short.
 *
 * <p>{@code active} being false closes a store without deleting it, so the movements
 * already recorded against it still read correctly. A store still holding stock cannot be
 * closed; the stock has to be transferred out first.
 */
@Document(collection = "inventory_stores")
@CompoundIndexes({
        @CompoundIndex(
                name = "school_inventory_store_name_uniq",
                def = "{'schoolId': 1, 'name': 1}",
                unique = true),
        @CompoundIndex(
                name = "school_inventory_store_type_idx",
                def = "{'schoolId': 1, 'storeType': 1, 'active': 1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryStore extends SchoolBase {

    // Name everybody uses. Example: "Kitchen Store"
    @NotBlank
    private String name;

    // What it is mainly for, used for grouping. Example: StoreType.KITCHEN
    @NotNull
    private StoreType storeType;

    // Where to find it. Example: "Behind the dining hall, ground floor."
    private String location;

    // Links to Staff.id for whoever answers for what is in this store.
    // Example: "67aa15d9dc3f7d0044444444"
    private String keeperStaffDocsId;

    // Links to HostelBuilding.id when this store belongs to one hostel.
    // Example: "67ba1122dc3f7d0011223344"
    private String hostelBuildingDocsId;

    // Whether stock may still be moved into this store. Example: true
    @NotNull
    @Builder.Default
    private Boolean active = true;

    // Example: "Kept locked; keys with the head cook and the bursar."
    private String remarks;
}
