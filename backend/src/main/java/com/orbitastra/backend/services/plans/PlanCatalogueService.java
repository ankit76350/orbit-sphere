package com.orbitastra.backend.services.plans;

import java.time.Instant;
import java.util.ArrayList;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.orbitastra.backend.common.error.exception.ApiException;
import com.orbitastra.backend.dto.plans.catalogue.PlanCreateRequest;
import com.orbitastra.backend.dto.plans.catalogue.PlanDraftUpdateRequest;
import com.orbitastra.backend.dto.plans.catalogue.PlanResponse;
import com.orbitastra.backend.models.plans.PlanDefinition;
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
        if (plan.getStatus() != PlanStatus.DRAFT) {
            throw ApiException.conflict("PLAN_NOT_EDITABLE",
                    "'" + plan.getPlanCode() + "' version " + plan.getPlanVersion() + " is "
                            + plan.getStatus() + " and cannot be edited. Schools may already be "
                            + "on it. Make a new version of it instead.");
        }

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
        PlanDefinition savedPlan = plans.save(plan);

        return PlanResponse.fromPlan(savedPlan,
                "Draft updated. It is still a DRAFT, so nobody can buy it and everything about "
                        + "it can still be changed. Publish it when the price is settled.");
    }

    //* ---------------------------------------------------------------------------------

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
