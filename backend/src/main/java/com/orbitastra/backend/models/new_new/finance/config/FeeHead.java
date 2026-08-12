package com.orbitastra.backend.models.new_new.finance.config;

import java.math.BigDecimal;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

import com.orbitastra.backend.models.new_new.base.SchoolBase;
import com.orbitastra.backend.models.new_new.finance.enums.FeeCategory;
import com.orbitastra.backend.models.new_new.finance.enums.FeeFrequency;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * One thing a school can charge for, such as Tuition, Lab or Exam.
 *
 * <p>This is the reusable setting that invoice lines are built from. It is not
 * the charge itself; the amount actually billed to a student is copied onto
 * FeeInvoiceLine when the invoice is made, so changing a head later never
 * rewrites bills that have already gone out.
 *
 * <p>{@code headCode} is the stable key other records point at and must not be
 * renamed once invoices exist. {@code defaultAmount} is only a starting value;
 * a FeeStructure line may override it for a particular class.
 *
 * <p>{@code revenueLedgerAccountDocsId} is what connects fees to the books. When
 * it is set, issuing an invoice for this head can post to the right income
 * account by itself. When it is null the accounts team has to place the amount
 * by hand.
 */
@Document(collection = "fee_heads")
@CompoundIndexes({
        @CompoundIndex(
                name = "school_fee_head_code_uniq",
                def = "{'schoolId': 1, 'headCode': 1}",
                unique = true),
        @CompoundIndex(
                name = "school_fee_head_category_active_idx",
                def = "{'schoolId': 1, 'category': 1, 'active': 1, 'sortOrder': 1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class FeeHead extends SchoolBase {

    // Stable key used by fee structures and invoice lines. Example: "TUITION"
    @NotBlank
    private String headCode;

    // Name shown to staff and printed on the bill. Example: "Tuition Fee"
    @NotBlank
    private String name;

    // Example: "Regular monthly teaching fee for classes I to V."
    private String description;

    // Grouping used for reports. Example: FeeCategory.TUITION
    @NotNull
    private FeeCategory category;

    // How often this head is normally charged. Example: FeeFrequency.MONTHLY
    @NotNull
    private FeeFrequency frequency;

    // Starting amount, which a fee structure line may override. Example: 2500.00
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal defaultAmount;

    // Example: "INR"
    @NotBlank
    private String currencyCode;

    // Whether GST or another tax applies to this head. Example: false
    @NotNull
    @Builder.Default
    private Boolean taxable = false;

    // Tax rate to use when taxable is true. Example: 18.00
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal taxRatePercent;

    // Whether money collected under this head can be given back. Example: false
    @NotNull
    @Builder.Default
    private Boolean refundable = false;

    // Whether a concession or scholarship may reduce this head. Example: true
    @NotNull
    @Builder.Default
    private Boolean concessionAllowed = true;

    // Whether a late-payment charge may be added for this head. Example: true
    @NotNull
    @Builder.Default
    private Boolean lateFeeApplicable = true;

    // Income account this head is posted to. Example: "67ac20a1dc3f7d0066554433"
    private String revenueLedgerAccountDocsId;

    // Order this head appears in on screens and printed bills. Example: 10
    @Builder.Default
    private Integer sortOrder = 0;

    // Whether new fee structures may still use this head. Example: true
    @NotNull
    @Builder.Default
    private Boolean active = true;
}
