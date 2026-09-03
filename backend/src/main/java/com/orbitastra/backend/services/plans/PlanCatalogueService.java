package com.orbitastra.backend.services.plans;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.orbitastra.backend.common.error.exception.ApiException;
import com.orbitastra.backend.dto.plans.catalogue.PlanCreateRequest;
import com.orbitastra.backend.dto.plans.catalogue.PlanDraftUpdateRequest;
import com.orbitastra.backend.dto.plans.catalogue.PlanFeatureListResponse;
import com.orbitastra.backend.dto.plans.catalogue.PlanFeatureRequest;
import com.orbitastra.backend.dto.plans.catalogue.PlanResponse;
import com.orbitastra.backend.models.plans.PlanDefinition;
import com.orbitastra.backend.models.plans.embedded.PlanFeature;
import com.orbitastra.backend.models.plans.enums.FeatureCode;
import com.orbitastra.backend.models.plans.enums.PlanStatus;
import com.orbitastra.backend.repositories.plans.PlanDefinitionRepository;
import com.orbitastra.backend.services.core.helper.TextHelper;
import com.orbitastra.backend.services.plans.helper.PlanValidator;

import lombok.RequiredArgsConstructor;

/**
 * The plan catalogue — what we sell, at what price, with what limits. Endpoints #1 to #7.
 *
 * <p><b>Platform surface only.</b> A {@code PlanDefinition} has no {@code schoolId}: it is
 * configuration shared by every tenant, and it is the one document in this module that is not
 * school-owned. No school may create, price or publish a plan, so none of these endpoints exists
 * on the school surface.
 *
 * <p><b>This is not the student fee module.</b> {@code models/finance} is money a parent pays a
 * school; this is money a school pays the platform. Nothing here may touch a {@code FeeInvoice}.
 *
 * <h2>The rule the whole group is shaped around</h2>
 *
 * <p>A plan version is <b>immutable once published</b>. A draft can be edited freely; the moment
 * it goes on sale a school can be on it, and changing the price of a plan somebody already
 * bought would change what they agreed to pay without anybody agreeing to it. So #2 refuses to
 * edit a published plan, and #5 copies it into a new draft version instead.
 *
 * <p>That is why a plan starts as {@code DRAFT} rather than being created ready to sell.
 */
@Service
@RequiredArgsConstructor
public class PlanCatalogueService {

    private final PlanDefinitionRepository plans;
    private final PlanValidator planValidator;

    //! endpoint 1 — create a draft plan -----------------------------------------------

        /**
         * Creates a new plan as a draft.
         *
         * <p>The plan starts as {@code DRAFT}, version 1, and cannot be purchased yet.
         *
         * <p>The plan code must be unique. The same code cannot be used for another plan because the
         * code is the plan's permanent identity.
         *
         * <p>The code is optional. If not provided, it is created from the name. For example,
         * {@code Premium Plus} becomes {@code PREMIUM_PLUS}.
         *
         * <p>The new plan starts with no features. Use #3 to add features.
         *
         * <p>Endpoint: {@code POST /platform/plans/drafts}
         */
    @Transactional
    public PlanResponse createDraft(PlanCreateRequest request) {
        //! step 1 - normalize and check everything the caller sent
        // Normally nothing was sent, and the code comes from the name.
        String planCode = planValidator.resolvePlanCode(request.planCode(), request.name());
        String currencyCode = planValidator.validateCurrencyCode(request.currencyCode());
        var listPrice = planValidator.validatePrice("listPrice", request.listPrice());
        planValidator.validateLimit("maxStudents", request.maxStudents());
        planValidator.validateLimit("maxUsers", request.maxUsers());
        planValidator.validateSellingWindow(request.effectiveFrom(), request.effectiveUntil());

        //! step 2 - the code has to be free
        if (plans.existsByPlanCode(planCode)) {
            throw ApiException.conflict("PLAN_CODE_TAKEN",
                    "A plan called '" + planCode + "' already exists. To change its price, make "
                            + "a new version of it instead of a new plan.");
        }

        //! step 3 - build the draft. Status, version and availability are ours to set, not the
        //! caller's: a plan that could be created ACTIVE would be on sale before it was priced.
        PlanDefinition plan = PlanDefinition.builder()
                .planCode(planCode)
                .planVersion(1)
                .name(request.name().trim())
                .description(TextHelper.blankToNull(request.description()))
                .status(PlanStatus.DRAFT)
                .billingCycle(request.billingCycle())
                .listPrice(listPrice)
                .currencyCode(currencyCode)
                .maxStudents(request.maxStudents())
                .maxUsers(request.maxUsers())
                .effectiveFrom(request.effectiveFrom())
                .effectiveUntil(request.effectiveUntil())
                .publiclyAvailable(false)
                .features(new ArrayList<>())
                .build();

        //! step 4 - save
        //TODO: save
        PlanDefinition savedPlan = plans.save(plan);

        return PlanResponse.fromPlan(savedPlan,
                "Draft created. Nobody can buy it yet: set its features, then publish it. "
                        + "While it is a DRAFT everything about it can still be changed.");
    }

    //! endpoint 2 — edit a draft ------------------------------------------------------
        /**
         * Updates the details of a draft plan.
         *
         * <p>Published plans cannot be changed. This prevents changing the price or details for schools
         * that are already using the plan.
         *
         * <p>Retired plans also cannot be changed because schools may still be using them.
         *
         * <p>Only the fields provided are updated. A {@code null} value keeps the existing value, while
         * {@code ""} clears the description.
         *
         * <p>The start and end dates are updated together.
         */
    @Transactional
    public PlanResponse updateDraft(String code, Integer version, PlanDraftUpdateRequest request) {
        //! step 1 - refuse a request that asks for nothing. Before the lookup, because a 404 for
        //! an empty PATCH would send the caller looking for the wrong problem.
        if (request.isEmpty()) {
            throw ApiException.badRequest("NOTHING_TO_UPDATE",
                    "Send at least one of name, description, billingCycle, listPrice, "
                            + "currencyCode, maxStudents, maxUsers or sellingWindow.");
        }

        //! step 2 - find the plan, or 404
        PlanDefinition plan = loadPlan(code, version);

        //! step 3 - only a draft may be edited
        requireDraft(plan, "cannot be edited");

        //! step 4 - apply only what was sent
        if (request.name() != null) {
            String name = request.name().trim();
            if (name.isEmpty()) {
                throw ApiException.badRequest("PLAN_NAME_REQUIRED",
                        "A plan name cannot be removed. Send a new one, or omit the field.");
            }
            plan.setName(name);
        }
        if (request.description() != null) {
            plan.setDescription(TextHelper.blankToNull(request.description()));
        }
        if (request.billingCycle() != null) {
            plan.setBillingCycle(request.billingCycle());
        }
        if (request.listPrice() != null) {
            plan.setListPrice(planValidator.validatePrice("listPrice", request.listPrice()));
        }
        if (request.currencyCode() != null) {
            plan.setCurrencyCode(planValidator.validateCurrencyCode(request.currencyCode()));
        }
        if (request.maxStudents() != null) {
            planValidator.validateLimit("maxStudents", request.maxStudents());
            plan.setMaxStudents(request.maxStudents());
        }
        if (request.maxUsers() != null) {
            planValidator.validateLimit("maxUsers", request.maxUsers());
            plan.setMaxUsers(request.maxUsers());
        }

        //! step 5 - the window, replaced whole when it was mentioned at all
        if (request.sellingWindow() != null) {
            Instant from = request.sellingWindow().effectiveFrom();
            Instant until = request.sellingWindow().effectiveUntil();
            planValidator.validateSellingWindow(from, until);
            plan.setEffectiveFrom(from);
            plan.setEffectiveUntil(until);
        }

        //! step 6 - save
        //TODO: save
        PlanDefinition savedPlan = plans.save(plan);

        return PlanResponse.fromPlan(savedPlan,
                "Draft updated. It is still a DRAFT, so nobody can buy it and everything about "
                        + "it can still be changed. Publish it when the price is settled.");
    }


    //! endpoint 3 — set a draft's features --------------------------------------------
    /**
     * #3 — replaces the whole feature list of a draft.
     *
     * <p><b>The whole list, not one feature at a time.</b> A feature list is priced as a set:
     * "2000 students and the AI reports for a given figure" is one offer, and there is no moment
     * at which half of it is a plan. Endpoints that added and removed a single entitlement would
     * make that half-state reachable and ordinary, and the plan would sit there priced for a set
     * of features it no longer has.
     *
     * <p>Sending {@code []} empties the list. That is the honest way to clear it, and it is why
     * there is no separate delete.
     *
     * <p><b>Only a draft.</b> Features are what the plan entitles a school to; changing them on
     * a published plan would silently change what somebody already bought. Same refusal as #2.
     */
    @Transactional
    public PlanFeatureListResponse replaceFeatures(String code, Integer version,
            List<PlanFeatureRequest> requests) {

        //! step 1 - find the plan, or 404
        PlanDefinition plan = loadPlan(code, version);

        //! step 2 - only a draft may be changed
        requireDraft(plan, "its features cannot be changed");

        //! step 3 - check every feature, and refuse the same code twice
        List<PlanFeatureRequest> incoming = requests == null ? List.of() : requests;
        Set<FeatureCode> seen = new LinkedHashSet<>();
        List<PlanFeature> replacement = new ArrayList<>();

        for (PlanFeatureRequest one : incoming) {
            if (!seen.add(one.featureCode())) {
                // Two rows for one code is not a bigger entitlement, it is a question: which of
                // the two limits applies? Nothing downstream could answer it.
                throw ApiException.badRequest("DUPLICATE_FEATURE",
                        "'" + one.featureCode() + "' appears more than once. Each feature can "
                                + "only be listed once, with one limit.");
            }

            planValidator.validateFeature(one.featureCode(), one.enabled(), one.usageLimit());
            replacement.add(one.toFeature());
        }

        //! step 4 - swap the whole list and save
        int before = plan.getFeatures() == null ? 0 : plan.getFeatures().size();
        plan.setFeatures(replacement);
        //TODO: save
        PlanDefinition savedPlan = plans.save(plan);

        return PlanFeatureListResponse.fromPlan(savedPlan,
                "Replaced the feature list: " + before + " out, " + replacement.size()
                        + " in. Still a DRAFT, so it can be replaced again before publishing.");
    }


    //! endpoint 4 — publish a draft ---------------------------------------------------
    /**
     * #4 — turns a draft into a plan schools can buy.
     *
     * <p><b>A one-way door.</b> From here the version can never be edited again: #2 and #3 both
     * refuse anything that is not a draft, and there is no unpublish. A school can be on it from
     * the moment it goes live, and changing what they bought after they bought it is the thing
     * this whole group is arranged to prevent. A new price means #5 — a new version — and the
     * schools on this one stay exactly where they are.
     *
     * <p>Because it cannot be undone, it is <b>checked rather than trusted</b>: a plan with no
     * features would take a school's money and grant nothing, and a plan whose selling window
     * has already closed could never be bought at all. Both are refused here rather than
     * discovered by a school.
     *
     * <p><b>Publishing does not put it on the public list.</b> That is #7. A published plan is
     * real and sellable in a quote; whether it appears on the pricing page is a separate
     * decision, so the two are separate endpoints.
     */
    @Transactional
    public PlanResponse publish(String code, Integer version) {
        //! step 1 - find the plan, or 404
        PlanDefinition plan = loadPlan(code, version);

        //! step 2 - only a draft can be published. Refused rather than answered 200, because
        //! "it was already published" and "you just published it" are different facts and a
        //! caller who cannot tell them apart will assume the wrong one.
        if (plan.getStatus() == PlanStatus.ACTIVE) {
            throw ApiException.conflict("PLAN_ALREADY_PUBLISHED",
                    "'" + plan.getPlanCode() + "' version " + plan.getPlanVersion()
                            + " is already published. To change it, make a new version.");
        }

        requireDraft(plan, "cannot be published");

        //! step 3 - a plan with nothing in it is not a plan
        if (plan.getFeatures() == null || plan.getFeatures().isEmpty()) {
            throw ApiException.conflict("PLAN_HAS_NO_FEATURES",
                    "'" + plan.getPlanCode() + "' version " + plan.getPlanVersion() + " has no "
                            + "features, so a school buying it would get nothing. Set its "
                            + "features first.");
        }

        //! step 4 - and neither is one that can never be sold
        Instant now = Instant.now();
        if (plan.getEffectiveUntil() != null && !plan.getEffectiveUntil().isAfter(now)) {
            throw ApiException.conflict("PLAN_WINDOW_ALREADY_CLOSED",
                    "'" + plan.getPlanCode() + "' version " + plan.getPlanVersion() + " stops "
                            + "being sold on " + plan.getEffectiveUntil() + ", which has passed. "
                            + "Change or clear the selling window before publishing.");
        }

        //! step 5 - go live. An effectiveFrom already set is kept, so a launch date chosen while
        //! it was a draft still stands; an empty one means "from now".
        boolean scheduled = plan.getEffectiveFrom() != null && plan.getEffectiveFrom().isAfter(now);
        if (plan.getEffectiveFrom() == null) {
            plan.setEffectiveFrom(now);
        }
        plan.setStatus(PlanStatus.ACTIVE);

        //TODO: save
        PlanDefinition savedPlan = plans.save(plan);

        //! step 6 - say plainly what just became true, and what has not
        String nextStep = "Published, and now permanent: this version can never be edited again. "
                + (scheduled
                        ? "It goes on sale on " + savedPlan.getEffectiveFrom() + ". "
                        : "")
                + (Boolean.TRUE.equals(savedPlan.getPubliclyAvailable())
                        ? "It is on the public list."
                        : "It is NOT on the public list yet — it can only be offered privately "
                                + "in a quote until that is turned on.")
                + " To change the price, make a new version.";

        return PlanResponse.fromPlan(savedPlan, nextStep);
    }


    //! endpoint 6 — retire a plan -----------------------------------------------------
        /**
         * #6 — retires a plan from the catalogue.
         *
         * <p>Existing schools keep their plan, price, and features.
         * This does not cancel subscriptions.
         *
         * <p>Draft plans can also be retired. No data is deleted.
         *
         * <p>{@code publiclyAvailable} is managed by #7.
         */
    @Transactional
    public PlanResponse retire(String code, Integer version) {
        //! step 1 - find the plan, or 404
        PlanDefinition plan = loadPlan(code, version);

        //! step 2 - retiring is terminal, so saying "already retired" matters. There is no way
        //! back: no endpoint returns a plan to DRAFT or ACTIVE.
        if (plan.getStatus() == PlanStatus.RETIRED) {
            throw ApiException.conflict("PLAN_ALREADY_RETIRED",
                    "'" + plan.getPlanCode() + "' version " + plan.getPlanVersion()
                            + " is already retired.");
        }

        //! step 3 - remember what it was, because the two cases read differently
        boolean wasNeverSold = plan.getStatus() == PlanStatus.DRAFT;

        //! step 4 - record when it stopped being sold. A date already in the past is kept: it
        //! stopped then, and moving it forward would rewrite that.
        Instant now = Instant.now();
        if (plan.getEffectiveUntil() == null || plan.getEffectiveUntil().isAfter(now)) {
            plan.setEffectiveUntil(now);
        }
        plan.setStatus(PlanStatus.RETIRED);

        //TODO: save
        PlanDefinition savedPlan = plans.save(plan);

        return PlanResponse.fromPlan(savedPlan, wasNeverSold
                ? "Withdrawn. It was still a draft, so it was never sold to anybody and nothing "
                        + "else is affected. Its plan code stays taken."
                : "Retired, and no longer on the menu: no school can pick it from here. Schools "
                        + "ALREADY on it keep it, at the price and features they were sold, and "
                        + "nothing about their subscription has changed.");
    }

    //* ---------------------------------------------------------------------------------

    /**
     * Refuses anything but a draft.
     *
     * <p>The rule the whole catalogue is built on, in one place: a published plan may have
     * schools on it, so changing what it costs or what it includes would change what somebody
     * already agreed to without anybody agreeing to it. A retired plan is refused for the same
     * reason — schools may still be on it.
     *
     * <p>{@code what} completes the sentence, so each endpoint says which change was refused
     * rather than all of them sharing one vague message.
     */
    private void requireDraft(PlanDefinition plan, String what) {
        if (plan.getStatus() != PlanStatus.DRAFT) {
            throw ApiException.conflict("PLAN_NOT_EDITABLE",
                    "'" + plan.getPlanCode() + "' version " + plan.getPlanVersion() + " is "
                            + plan.getStatus() + " and " + what + ". Schools may already be on "
                            + "it. Make a new version of it instead.");
        }
    }

    /**
     * One plan version, by the code and version in the URL, or a 404.
     *
     * <p>The code is normalized the same way it was when the plan was created, so a link typed
     * as {@code /plans/premium-plus/versions/1} finds {@code PREMIUM_PLUS} rather than nothing.
     */
    private PlanDefinition loadPlan(String code, Integer version) {
        String planCode = planValidator.normalizePlanCode(code);
        return plans.findByPlanCodeAndPlanVersion(planCode, version)
                .orElseThrow(() -> ApiException.notFound("PLAN_NOT_FOUND",
                        "No plan '" + planCode + "' version " + version + " exists."));
    }
}
