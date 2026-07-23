package com.orbitastra.backend.models.undone.mess;

import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.base.SchoolBase;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A raw-provisions stock line held by the mess kitchen (rice, flour, oil, ...),
 * separate from the general store {@code inventory.InventoryItem}.
 */
@Document(collection = "mess_kitchen_stock")
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class MessKitchenStock extends SchoolBase {

    private String item;

    private BigDecimal quantity;

    // Unit of measure for the quantity, e.g. "kg", "litre", "packs".
    private String unit;

    // Reorder threshold; at or below this a restock alert is raised.
    private BigDecimal minAlertQuantity;
}
