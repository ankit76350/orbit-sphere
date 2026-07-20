package com.orbitastra.backend.models.undone.mess;

import java.math.BigDecimal;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A raw-provisions stock line held by the mess kitchen (rice, flour, oil, ...),
 * separate from the general store {@code inventory.InventoryItem}.
 */
@Document(collection = "mess_kitchen_stock")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessKitchenStock {

    @CreatedDate
    private java.time.LocalDateTime createdAt;

    @LastModifiedDate
    private java.time.LocalDateTime updatedAt;

    @Id
    private String id;

    @Indexed
    private String schoolId;

    private String item;

    private BigDecimal quantity;

    // Unit of measure for the quantity, e.g. "kg", "litre", "packs".
    private String unit;

    // Reorder threshold; at or below this a restock alert is raised.
    private BigDecimal minAlertQuantity;
}
