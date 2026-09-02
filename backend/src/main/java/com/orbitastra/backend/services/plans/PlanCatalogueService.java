package com.orbitastra.backend.services.plans;

import java.util.ArrayList;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.orbitastra.backend.common.error.exception.ApiException;
import com.orbitastra.backend.dto.plans.catalogue.PlanCreateRequest;
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
     * #1 — makes a new plan, as a draft.
     *
     * <p>It starts at {@code DRAFT}, version 1, and not publicly available. Nobody can buy it
     * while the price is still being decided, which is the reason the endpoint does not simply
     * create a live plan.
     *
     * <p><b>The code must be new.</b> A second {@code PREMIUM} is refused even though the unique
     * index is on the code <i>and</i> version, so version 1 of a second PREMIUM would technically
     * fit. It is refused because {@code planCode} is the plan's permanent identity and two plans
     * sharing it can never be told apart afterwards — a school subscription stores the code and
     * a version, and "which PREMIUM" would have no answer. A genuinely new price for an existing
     * plan is #5, not this.
     *
     * <p>The plan is created with an <b>empty feature list</b>; #3 sets the whole list in one go.
     */
    @Transactional
    public PlanResponse createPlan(PlanCreateRequest request) {
        //! step 1 - normalize and check everything the caller sent
        String planCode = planValidator.validatePlanCode(request.planCode());
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
}
