package com.orbitastra.backend.models.undone.feeengine;

import com.orbitastra.backend.models.BaseDocument;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

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
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class FeeHead extends BaseDocument {

    private String name;

    private BigDecimal amount;

    private FeeFrequency frequency;

    // GST applicability. When taxable, taxRatePercent carries the rate (e.g. 18).
    @Builder.Default
    private Boolean taxable = false;

    private BigDecimal taxRatePercent;
}
