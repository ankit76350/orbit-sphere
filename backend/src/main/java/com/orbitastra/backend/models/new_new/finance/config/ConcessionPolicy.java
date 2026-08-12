package com.orbitastra.backend.models.new_new.finance.config;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

import com.orbitastra.backend.models.new_new.base.SchoolBase;
import com.orbitastra.backend.models.new_new.finance.enums.ConcessionType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * A named discount the school offers, such as a sibling waiver or a staff-child
 * discount.
 *
 * <p>This is only the rule. Giving it to a student is a separate
 * ConcessionRequest, so the discount always has a named approver behind it.
 *
 * <p>A concession is a discount the school decides on its own. A scholarship
 * that has a budget, an application form and a committee belongs in AidProgramme
 * instead. Both end up reducing an invoice line, but only one of them needs a
 * fund to be tracked.
 *
 * <p>{@code eligibleFeeHeadDocsIds} being empty means the discount may be
 * applied to any head that allows concessions. When the list has entries, only
 * those heads may be reduced, which stops a tuition waiver from also wiping out
 * a transport charge.
 */
@Document(collection = "concession_policies")
@CompoundIndexes({
        @CompoundIndex(
                name = "school_concession_policy_code_uniq",
                def = "{'schoolId': 1, 'policyCode': 1}",
                unique = true),
        @CompoundIndex(
                name = "school_concession_policy_active_idx",
                def = "{'schoolId': 1, 'active': 1, 'name': 1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class ConcessionPolicy extends SchoolBase {

    // Stable key used by concession requests. Example: "SIBLING_SECOND_CHILD"
    @NotBlank
    private String policyCode;

    // Example: "Second Child Sibling Waiver"
    @NotBlank
    private String name;

    // Example: "Given to the second child when two siblings study here together."
    private String description;

    // How the discount is worked out. Example: ConcessionType.PERCENT
    @NotNull
    private ConcessionType concessionType;

    // Share taken off when the type is PERCENT. Example: 25.00
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal percent;

    // Money taken off when the type is FIXED_AMOUNT. Example: 5000.00
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal fixedAmount;

    // Most that may be taken off in one year, whatever the rule works out to.
    // Example: 20000.00
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal maximumAmountPerYear;

    // Example: "INR"
    @NotBlank
    private String currencyCode;

    // Heads this discount may reduce. Empty means any head that allows it.
    @Builder.Default
    private List<String> eligibleFeeHeadDocsIds = new ArrayList<>();

    // Written rule the fee desk checks before raising a request.
    // Example: "Both children must be enrolled and active in the same year."
    private String eligibilityCriteria;

    // Whether a second person has to approve each request. Example: true
    @NotNull
    @Builder.Default
    private Boolean requiresApproval = true;

    // Proof the family has to give, listed for the fee desk.
    // Example: "Birth certificate of both children."
    private String requiredEvidence;

    // Whether new requests may still use this policy. Example: true
    @NotNull
    @Builder.Default
    private Boolean active = true;
}
