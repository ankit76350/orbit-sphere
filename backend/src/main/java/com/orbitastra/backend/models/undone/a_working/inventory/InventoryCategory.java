package com.orbitastra.backend.models.undone.a_working.inventory;

import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.new_new.base.SchoolBase;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Document(collection = "inventory_categories")
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryCategory extends SchoolBase {

    /**
     * Stationery
     * Sports
     * Science Lab
     * Computer Lab
     */
    private String name;

    /**
     * Category description.
     */
    private String description;

    /**
     * Inventory icon.
     */
    private String iconUrl;

    /**
     * Display order.
     */
    @Builder.Default
    private Integer sortOrder = 0;

    /**
     * Active / Inactive
     */
    @Builder.Default
    private Boolean active = true;
}