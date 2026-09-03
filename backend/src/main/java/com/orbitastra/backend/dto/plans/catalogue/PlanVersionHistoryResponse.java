package com.orbitastra.backend.dto.plans.catalogue;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import com.orbitastra.backend.models.plans.PlanDefinition;
import com.orbitastra.backend.models.plans.enums.BillingCycle;
import com.orbitastra.backend.models.plans.enums.PlanStatus;

/**
 * Every version of one plan, newest first. Endpoint #9.
 *
 * <p><b>Not paged</b>, unlike #8. A plan has a handful of versions — a price does not change
 * fifty times — so the whole history is one answer, and a caller reading how a price moved would
 * have to stitch pages back together to see it.
 *
 * <p>{@code name} is the newest version's name. A plan can be renamed between versions, and the
 * newest is the one somebody means when they say "Premium"; each row carries its own name too,
 * so a rename is visible rather than hidden.
 */
public record PlanVersionHistoryResponse(
        String planCode,
        String name,
        int versionCount,
        List<VersionRow> versions,
        String note) {

    /**
     * One version, and how it differs from the one before it.
     *
     * <p>{@code priceChangeFromPrevious} is the point of the endpoint: reading a column of prices
     * and doing the subtraction by eye is exactly the arithmetic a response should save somebody.
     * Null on the first version, and null across a currency change — 49999 INR to 699 USD is not
     * a difference of -49300, and pretending otherwise would be worse than saying nothing.
     *
     * <p>{@code schoolsOnThisVersion} is what makes the history actionable rather than
     * historical: it is the answer to "can this version be forgotten".
     */
    public record VersionRow(
            Integer planVersion,
            String name,
            PlanStatus status,
            BigDecimal listPrice,
            String currencyCode,
            BigDecimal priceChangeFromPrevious,
            BillingCycle billingCycle,
            Long maxStudents,
            Long maxUsers,
            Boolean publiclyAvailable,
            boolean sellable,
            int featureCount,
            long schoolsOnThisVersion,
            Instant effectiveFrom,
            Instant effectiveUntil,
            Instant createdAt) {
    }

    /**
     * @param newestFirst the versions in descending version order
     * @param schoolCounts how many subscriptions point at each version, by version number
     */
    public static PlanVersionHistoryResponse fromVersions(List<PlanDefinition> newestFirst,
            java.util.Map<Integer, Long> schoolCounts, String note) {

        List<VersionRow> rows = new java.util.ArrayList<>();

        for (int i = 0; i < newestFirst.size(); i++) {
            PlanDefinition plan = newestFirst.get(i);
            // The list runs newest first, so the version before this one is the NEXT element.
            PlanDefinition previous = i + 1 < newestFirst.size() ? newestFirst.get(i + 1) : null;

            BigDecimal change = null;
            if (previous != null
                    && plan.getListPrice() != null
                    && previous.getListPrice() != null
                    && plan.getCurrencyCode() != null
                    && plan.getCurrencyCode().equals(previous.getCurrencyCode())) {
                change = plan.getListPrice().subtract(previous.getListPrice());
            }

            rows.add(new VersionRow(
                    plan.getPlanVersion(),
                    plan.getName(),
                    plan.getStatus(),
                    plan.getListPrice(),
                    plan.getCurrencyCode(),
                    change,
                    plan.getBillingCycle(),
                    plan.getMaxStudents(),
                    plan.getMaxUsers(),
                    plan.getPubliclyAvailable(),
                    PlanResponse.isSellable(plan),
                    plan.getFeatures() == null ? 0 : plan.getFeatures().size(),
                    schoolCounts.getOrDefault(plan.getPlanVersion(), 0L),
                    plan.getEffectiveFrom(),
                    plan.getEffectiveUntil(),
                    plan.getCreatedAt()));
        }

        PlanDefinition newest = newestFirst.get(0);
        return new PlanVersionHistoryResponse(
                newest.getPlanCode(), newest.getName(), rows.size(), rows, note);
    }
}
