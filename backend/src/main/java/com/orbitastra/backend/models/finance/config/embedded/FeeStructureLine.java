package com.orbitastra.backend.models.finance.config.embedded;

import java.math.BigDecimal;

import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

import com.orbitastra.backend.models.finance.enums.FeeFrequency;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * One charge inside a FeeStructure version.
 *
 * <p>It has no collection identity of its own. The list of lines in a structure
 * is small and fixed, which is why the lines live inside the structure instead
 * of in their own collection.
 *
 * <p>{@code amount} is what this class actually pays, and it wins over the
 * head's default amount. {@code frequency} may also override the head, which is
 * how one head such as Transport can be monthly for day scholars and one-time
 * for others.
 *
 * <p>Only the head's id is stored. Nothing about the head is copied here, because
 * a structure line is a setting, not a bill. Tax comes from the head alone, since
 * a tax rate follows what the service is and not which class the student is in.
 * FeeInvoiceLine does copy the head's code, name and tax rate, because a bill has
 * to keep showing what the parent was charged even after the head is renamed or
 * the tax rate changes.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeeStructureLine {

    // Order this line appears in. Example: 1
    @NotNull
    private Integer lineNo;

    // Which fee head is charged. Links to FeeHead.id, and the head is loaded to
    // get its code and name for display.
    // Example: "67ac1188dc3f7d0011aa22bb"
    @NotBlank
    private String feeHeadDocsId;

    // Amount for this class, which overrides the head's default. Example: 2500.00
    @NotNull
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal amount;

    // Overrides the head's frequency when set. Example: FeeFrequency.MONTHLY
    private FeeFrequency frequency;

    // False when a family may choose not to take this charge, such as transport.
    // Example: true
    @NotNull
    @Builder.Default
    private Boolean mandatory = true;

    // Whether a concession or scholarship may reduce this line. Example: true
    @NotNull
    @Builder.Default
    private Boolean concessionAllowed = true;

    // Why this class pays a different amount from the head default.
    // Example: "Lab charge reduced for classes below VI."
    private String remarks;
}
