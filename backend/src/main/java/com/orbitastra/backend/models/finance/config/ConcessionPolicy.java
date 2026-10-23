package com.orbitastra.backend.models.finance.config;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

import com.orbitastra.backend.models.base.SchoolBase;
import com.orbitastra.backend.models.finance.enums.ConcessionType;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
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
 * <p>A concession is a discount the school decides to give, and there is no money
 * set aside for it. A 25 percent tuition discount takes 25 percent off tuition on
 * every bill it is allowed to touch, for as long as the request is valid. A
 * scholarship is a different thing: it has a fund that can run out, a form to fill
 * in and a committee to decide. That belongs in AidProgramme. Both end up
 * reducing a bill, but only the scholarship has money that runs out.
 *
 * <p>No limit is saved here. If the school wants to cap what it gives away, it
 * sets {@code maximumConcessionPerYear} on the fee head, and that one setting
 * covers every discount a student has on that head.
 *
 * <p>{@code eligibleFeeHeadDocsIds} must name at least one fee head. An empty
 * list is not allowed and does not mean "all of them". This is on purpose: if we
 * got it wrong, a tuition discount would also come off transport, hostel and exam
 * charges. A school that really wants every head has to list every head.
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
//! Basicaaly this is defifnication of the discount policy from the school

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

    // Share taken off when the type is PERCENT. A full waiver is 100.00 here.
    // Example: 25.00
    @DecimalMin("0.00")
    @DecimalMax("100.00")
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal percent;

    // Money taken off one bill when the type is FIXED_AMOUNT. This is per
    // invoice, not per year. Example: 5000.00
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal fixedAmount;

    // Example: "INR"
    @NotBlank
    private String currencyCode;

    // Heads this discount may reduce. At least one, and nothing outside this list
    // is ever touched. Example: the id of the TUITION head
    @NotEmpty
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
