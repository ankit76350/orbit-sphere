package com.orbitastra.backend.models.undone.feeengine;

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
 * A named discount scheme (sibling waiver, scholarship, staff-child, RTE, ...).
 * The existing {@code finance.FeeInvoice.discount} is just a per-invoice amount;
 * this master defines the reusable rule that a {@link ConcessionRequest} applies.
 */
@Document(collection = "concession_policies")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConcessionPolicy {

    @CreatedDate
    private java.time.LocalDateTime createdAt;

    @LastModifiedDate
    private java.time.LocalDateTime updatedAt;

    @Id
    private String id;

    @Indexed
    private String schoolId;

    private String name;

    // Percentage discount granted (0-100). A null percent implies a custom/amount-based concession.
    private BigDecimal percent;

    private String criteria;

    @Builder.Default
    private Boolean active = true;
}
