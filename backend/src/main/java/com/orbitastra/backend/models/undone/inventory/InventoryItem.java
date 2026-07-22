package com.orbitastra.backend.models.undone.inventory;

import com.orbitastra.backend.models.BaseDocument;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Document(collection = "inventory_items")
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryItem extends BaseDocument {

    private String name;

    private String category;

    private Integer stockQty;

    private BigDecimal unitPrice;

    private Integer lowStockThreshold;
}
