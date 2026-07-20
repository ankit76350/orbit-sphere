package com.orbitastra.backend.models.undone.feeengine;

import java.math.BigDecimal;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.undone.feeengine.enums.FeeFrequency;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A reusable, configurable chargeable line (e.g. "Tuition", "Lab", "Exam") from
 * which invoices are assembled. This is the fee CONFIGURATION master — the coarse
 * {@code finance.enums.FeeType} on an invoice does not carry frequency or GST, so
 * this is not a duplicate of the invoice.
 */
@Document(collection = "fee_heads")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeeHead {

    @CreatedDate
    private java.time.LocalDateTime createdAt;

    @LastModifiedDate
    private java.time.LocalDateTime updatedAt;

    @Id
    private String id;

    @Indexed
    private String schoolId;

    private String name;

    private BigDecimal amount;

    private FeeFrequency frequency;

    // GST applicability. When taxable, taxRatePercent carries the rate (e.g. 18).
    @Builder.Default
    private Boolean taxable = false;

    private BigDecimal taxRatePercent;
}
