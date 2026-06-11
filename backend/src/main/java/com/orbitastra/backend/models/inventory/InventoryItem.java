package com.orbitastra.backend.models.inventory;

import java.math.BigDecimal;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Document(collection = "inventory_items")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryItem {

    @Id
    private String id;

    private String schoolId;

    private String name;

    private String category;

    private Integer stockQty;

    private BigDecimal unitPrice;

    private Integer lowStockThreshold;
}
