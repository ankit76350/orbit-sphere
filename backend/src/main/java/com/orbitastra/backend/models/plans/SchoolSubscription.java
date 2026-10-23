package com.orbitastra.backend.models.plans;

import java.math.BigDecimal;
import java.time.Instant;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

import com.orbitastra.backend.models.base.SchoolBase;
import com.orbitastra.backend.models.plans.enums.BillingCycle;
import com.orbitastra.backend.models.plans.enums.SubscriptionStatus;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * Current and historical contracted subscription periods for one school.
 *
 * <p>The inherited {@code schoolId} links to School.id.
 * {@code planDefinitionDocsId + planVersion} identify the selected immutable
 * PlanDefinition version. Contract price, billing cycle, and optional capacity
 * overrides are stored here because one school can negotiate terms different
 * from the public plan defaults.
 *
 * <p><b>One document per billing period, not per school.</b> Exactly one may
 * have {@code current = true}, and that is the row the school is on; the rest
 * are the periods it has been through. Two endpoints make a second row, and
 * they differ in what happens to the one being closed:
 *
 * <ul>
 *   <li><b>#16, a plan change</b> — closes the current row with its period
 *       <i>trimmed</i> to the day of the change, because that is the period it
 *       actually served, and inserts a row on the new plan's terms.</li>
 *   <li><b>#17, a renewal</b> — closes the current row with its dates left
 *       alone, because it ran its full course, and inserts a row on
 *       <i>identical</i> terms whose period starts where that one ended.</li>
 * </ul>
 *
 * <p>Either way the new row gets its own {@code subscriptionNo} from the
 * sequence, and the closed row keeps its status: it was superseded or renewed,
 * not cancelled or expired.
 *
 * <p>So every read of "the school's subscription" goes through
 * {@code schoolId + current}, never through "the one row for this school".
 * Subscription fields are deliberately not duplicated in School.
 *
 * <p>A closed row keeps its {@code status}. It did not expire and it was not
 * cancelled — it was superseded, and writing either of those words would put
 * something false in the record. {@code current} is the field that says which
 * row is live.
 *
 * <p>The document holds what is true now. What happened to get here — every
 * status move, every edit, with its own reason and timestamp — is
 * {@code subscription_history}, one row per event.
 */
@Document(collection = "school_subscriptions")
@CompoundIndexes({
                @CompoundIndex(name = "school_subscription_no_uniq", def = "{'schoolId': 1, 'subscriptionNo': 1}", unique = true),
                @CompoundIndex(name = "school_current_subscription_uniq", def = "{'schoolId': 1, 'current': 1}", unique = true, partialFilter = "{'current': true}"),
                @CompoundIndex(name = "school_subscription_status_period_idx", def = "{'schoolId': 1, 'status': 1, 'currentPeriodEnd': 1}"),
                // Answers "who is on this plan version". Added 2026-09-03 for the plan version
                // history (#9), which asks it once per version — and for anything later that has
                // to know whether a version can be retired without stranding somebody. Without
                // it that question is a scan of every subscription in the platform, on a
                // collection that eventually holds one row per school.
                //
                // NOT school-scoped, unlike every other index here: the question is about a
                // plan, and a plan belongs to no school.
                @CompoundIndex(name = "subscription_plan_version_idx", def = "{'planDefinitionDocsId': 1, 'planVersion': 1}"),
                // Answers "every school's subscription, soonest to end first" — #30's default
                // order, and the order #31 and #32 will want too. Added 2026-09-09.
                //
                // NOT school-scoped either, and that is the point: #30 takes no school, so every
                // other index here is useless to it — each one is prefixed on schoolId. Without
                // this, the operator's main screen is a collection scan plus an in-memory sort of
                // every subscription on the platform.
                //
                // WHY THE `_id` IS IN IT. The sort ends in _id because subscriptionNo is only
                // unique WITHIN a school, so across the platform it cannot break a tie. An index
                // without _id still scans, but Mongo then has to sort the matches in memory:
                // measured on 3148 rows, `{status, currentPeriodEnd}` read 2357 documents to
                // return 20, and this one reads 20.
                //
                // WHY THE PERIOD END LEADS rather than status, even though status is the headline
                // filter. Equality-first would be the textbook choice, but it only serves queries
                // that send a status — measured, a status-prefixed index left the bare list on a
                // collection scan, while this one serves the bare list AND every filter
                // combination in index order. One index instead of two.
                @CompoundIndex(name = "subscription_period_end_idx", def = "{'currentPeriodEnd': 1, '_id': 1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class SchoolSubscription extends SchoolBase {

        // Example: "SUB/2026/000001"
        @NotBlank
        private String subscriptionNo;

        // Links to PlanDefinition.id. Example: "67aa1202dc3f7d0012345678"
        @NotBlank
        private String planDefinitionDocsId;

        // Example: 1
        @NotNull
        private Integer planVersion;

        // Example: SubscriptionStatus.ACTIVE
        @NotNull
        private SubscriptionStatus status;

        // Example: BillingCycle.YEARLY
        @NotNull
        private BillingCycle billingCycle;

        // Example: 2026-04-01T00:00:00Z
        @NotNull
        private Instant currentPeriodStart;

        // Example: 2027-03-31T23:59:59Z
        @NotNull
        private Instant currentPeriodEnd;

        // Example: true
        @NotNull
        @Builder.Default
        private Boolean autoRenew = false;

        // Example: 45000.00
        @NotNull
        @Field(targetType = FieldType.DECIMAL128)
        private BigDecimal contractedPrice;

        // Example: "INR"
        @NotBlank
        private String currencyCode;

        /**
         * This school's student ceiling. Example: 2500
         *
         * <p><b>#13 always writes it</b>, copying {@code PlanDefinition.maxStudents} when the
         * sale named no figure of its own — so the subscription says what the school may use
         * without anybody reading the plan behind it, and a school already sold keeps what it
         * bought when the plan's next version moves its ceiling.
         *
         * <p>Null still means "fall back to the plan", which is what #14 stores when an override
         * is removed. It is no longer the ordinary state of a new subscription.
         */
        private Long maxStudentsOverride;

        /**
         * This school's user ceiling. Example: 300
         *
         * <p>Same as {@code maxStudentsOverride}: written on every sale from
         * {@code PlanDefinition.maxUsers} unless the caller named one, and null only when #14 has
         * removed it.
         */
        private Long maxUsersOverride;

        // Example: true
        @NotNull
        @Builder.Default
        private Boolean current = true;

        //!
        // Example: "customer_Qx7B2mR9"
        private String billingCustomerReference;

        //collect during update
        private String reasonForChanges;
}
