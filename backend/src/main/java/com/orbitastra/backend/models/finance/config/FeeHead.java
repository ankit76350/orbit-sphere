package com.orbitastra.backend.models.finance.config;

import java.math.BigDecimal;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

import com.orbitastra.backend.models.base.SchoolBase;
import com.orbitastra.backend.models.finance.enums.FeeCategory;
import com.orbitastra.backend.models.finance.enums.FeeFrequency;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * One thing the school charges money for.
 *
 * <p>One row for each: Tuition Fee, Transport Fee, Hostel Fee, Exam Fee, Library
 * Fine. A school sets these up once and then uses them for years.
 *
 * <p>This is only the setting. It is not the charge on anybody's bill. When a bill
 * is made, the name and the amount are copied onto a FeeInvoiceLine, so renaming a
 * head next year does not change a bill that has already gone out.
 *
 * <p>The amount here is only a starting value. A FeeStructure decides what each
 * class actually pays, because Class XII tuition is not Class I tuition.
 *
 * <p>Where a charge comes from:
 *
 * <pre>
 * FeeHead          "we charge for transport"        <- this file
 *   FeeStructure   "Class VI pays 2000 a month"
 *     FeeInvoice   "Arjun owes 2000 for August"
 * </pre>
 *
 * <p>Some heads are not set up by hand at all. A late library book and a broken
 * window both end up as a charge under a FINE head, raised by the library and the
 * conduct parts of the system.
 *
 * <p>{@code headCode} is the key everything else points at. Do not rename it once
 * bills exist.
 *
 * <p>{@code maximumConcessionPerYear} is the only yearly discount limit anywhere in
 * the fee system, and it sits here on purpose. A concession says what share to take
 * off. This says how much the school is willing to give away in total. Keeping it
 * here means one setting covers every discount a student has, instead of each
 * concession carrying its own limit that somebody then has to keep in step.
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

    // The key the school picks, which fee structures and bills point at. Never
    // rename it once bills exist, or the old bills stop making sense.
    //
    // One category holds many heads, and this is what tells them apart. A school
    // can have LIBRARY_LATE_FINE, LOST_ID_CARD and BREAKAGE_CHARGE, all three
    // under the FINE category.
    // Example: "LIBRARY_LATE_FINE"
    @NotBlank
    private String headCode;

    // The name staff see on screen and parents see on the bill. You can reword
    // this whenever you like, because every bill keeps its own copy of the name.
    // Example: "Library Late Return Fine"
    @NotBlank
    private String name;

    // A longer note for staff, saying when this charge is used.
    // Example: "Charged for each day a borrowed book is late."
    private String description;

    // Which group this charge belongs to, used for reports such as "how much did
    // we collect in tuition this year". The list of groups is fixed and schools
    // cannot add to it; they make new heads instead.
    // Example: FeeCategory.FINE
    @NotNull
    private FeeCategory category;

    // How often this is normally charged: every month, every term, once a year, or
    // only when something happens. A fee structure can change this for one class.
    // Example: FeeFrequency.MONTHLY
    @NotNull
    private FeeFrequency frequency;

    // A starting amount, so somebody setting up a class does not begin from
    // nothing. What a class actually pays is decided in the fee structure, and it
    // wins over this. Example: 2500.00
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal defaultAmount;

    // Which money this is in. Example: "INR"
    @NotBlank
    private String currencyCode;

    // Whether GST or another tax has to be added to this charge. Most school fees
    // do not have tax; things like a bus service sometimes do. Example: false
    @NotNull
    @Builder.Default
    private Boolean taxable = false;

    // How much tax to add, as a share. Only used when taxable is true.
    // Example: 18.00
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal taxRatePercent;

    // Whether money taken under this head can be given back. A hostel deposit can
    // be; a month's tuition already taught cannot. Example: false
    @NotNull
    @Builder.Default
    private Boolean refundable = false;

    // Whether a discount or a scholarship is allowed to reduce this charge. Turn
    // it off for things nobody should get a discount on, such as a fine.
    // Example: true
    @NotNull
    @Builder.Default
    private Boolean concessionAllowed = true;

    // The most discount one student can get on this charge in one academic year,
    // adding up every discount and scholarship they have. Null means no limit, and
    // that is the normal setting.
    //
    // This is the school limiting itself, not a promise to any family. It is the
    // school saying: whatever discounts a child has, we will not take more than
    // this much off this charge in one year. Example: 20000.00
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal maximumConcessionPerYear;

    // Whether a late-payment charge can be added when this is not paid on time.
    // Example: true
    @NotNull
    @Builder.Default
    private Boolean lateFeeApplicable = true;

    // Where this appears in the list on screens and on a printed bill. A smaller
    // number comes first, so tuition can sit above a small fine. Example: 10
    @Builder.Default
    private Integer sortOrder = 0;

    // Whether this can still be used in a new fee structure. Turning it off stops
    // new use but leaves every bill already raised under it alone. Example: true
    @NotNull
    @Builder.Default
    private Boolean active = true;
}
