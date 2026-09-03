package com.orbitastra.backend.controllers.plans;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import com.orbitastra.backend.dto.plans.catalogue.PlanDraftUpdateRequest;
import com.orbitastra.backend.dto.plans.catalogue.PlanFeatureListRequest;
import com.orbitastra.backend.dto.plans.catalogue.PlanFeatureListResponse;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.orbitastra.backend.common.web.PageResponse;
import com.orbitastra.backend.dto.plans.catalogue.PlanAvailabilityRequest;
import com.orbitastra.backend.dto.plans.catalogue.PlanCreateRequest;
import com.orbitastra.backend.dto.plans.catalogue.PlanResponse;
import com.orbitastra.backend.dto.plans.catalogue.PlanSearchRequest;
import com.orbitastra.backend.dto.plans.catalogue.PlanSummaryResponse;
import com.orbitastra.backend.models.plans.enums.PlanStatus;
import com.orbitastra.backend.services.plans.PlanCatalogueService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * The plan catalogue — endpoints #1 to #7 of the plan in this package's README.
 *
 * <p><b>Platform surface only, and that is structural.</b> A {@code PlanDefinition} has no
 * {@code schoolId}; it is configuration shared by every tenant. A school may look at the plans
 * it could move to (#11) but may not create one, price one, or publish one — so those endpoints
 * do not exist on the school surface at all, and there is no request a school can send that
 * reaches them.
 *
 * <p><b>A plan version is addressed by code and version</b>, not by id — {@code
 * /platform/plans/PREMIUM/versions/2}. That pair is the plan's business identity: it is what
 * {@code SchoolSubscription} stores, and it is what somebody means when they say "the school is
 * on Premium v2".
 *
 * <p>There is no {@code DELETE}. A plan that was ever published may be on a school's
 * subscription, and removing it would leave that subscription pointing at nothing. #6 retires a
 * plan instead: it stops being sold, and every school already on it keeps working.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/platform/plans")
public class PlanController {

    private final PlanCatalogueService planCatalogueService;

    /**
     * Creates a new plan.
     *
     * <p>The plan starts as {@code DRAFT}, version 1, and is not publicly available.
     * The request does not include status, version, or public availability because these
     * values are set automatically.
     *
     * <p>The plan starts with no features. Use #3 to add features.
     *
     * <p>The {@code /drafts} path makes it clear that this endpoint creates a draft,
     * not a plan that is ready for sale.
     *
     * <p>After creation, the plan is identified by its code and version.
     */
    @PostMapping("/drafts")
    public ResponseEntity<PlanResponse> createDraft(@Valid @RequestBody PlanCreateRequest request) {
        PlanResponse response = planCatalogueService.createDraft(request);

        return ResponseEntity
                .created(URI.create("/platform/plans/" + response.planCode() + "/versions/"
                        + response.planVersion()))
                .body(response);
    }

    /**
     * Updates the details of a draft plan.
     *
     * <p>Published plans cannot be changed. If changes are needed, #5 creates a new draft version.
     *
     * <p>Only the provided fields are updated. A {@code null} value keeps the current value, while
     * {@code ""} clears the description.
     *
     * <p>The start and end dates are updated together.
     *
     * <p>The plan code, version, status, public availability, and features cannot be changed here.
     * Use #3, #4, and #7 for those changes.
     */
    @PatchMapping("/{code}/versions/{version}")
    public ResponseEntity<PlanResponse> updateDraft(
            @PathVariable String code,
            @PathVariable Integer version,
            @Valid @RequestBody PlanDraftUpdateRequest request) {

        return ResponseEntity.ok(planCatalogueService.updateDraft(code, version, request));
    }

    /**
     * Endpoint #3 — sets the whole feature list of a draft.
     *
     * <p>A {@code PUT} because the list is replaced, not edited row by row. A feature list is
     * priced as a set, so there is no moment at which half of it is a plan — and endpoints that
     * added or removed one entitlement would make that half-state ordinary.
     *
     * <p>The rows arrive under a {@code features} key rather than as a bare array, so a bad row
     * is reported with the same {@code fieldErrors} shape as every other endpoint. Send
     * {@code &#123;"features": []&#125;} to empty the list; there is no separate delete.
     *
     * <p>Refused unless the plan is a {@code DRAFT}: features are what a school is buying.
     */
    @PutMapping("/{code}/versions/{version}/features")
    public ResponseEntity<PlanFeatureListResponse> replaceFeatures(
            @PathVariable String code,
            @PathVariable Integer version,
            @Valid @RequestBody PlanFeatureListRequest request) {

        return ResponseEntity.ok(
                planCatalogueService.replaceFeatures(code, version, request.features()));
    }

    /**
     * Endpoint #4 — turns a draft into a plan schools can buy.
     *
     * <p><b>A one-way door.</b> From here the version can never be edited: #2 and #3 refuse
     * anything that is not a draft, and there is no unpublish. To change the price, make a new
     * version with #5 — the schools on this one keep what they bought.
     *
     * <p>Takes no body. Refused if the plan has no features, or if its selling window has
     * already closed, because neither could ever be bought.
     *
     * <p>Publishing does not put the plan on the public list; that is #7.
     */
    @PostMapping("/{code}/versions/{version}/publish")
    public ResponseEntity<PlanResponse> publish(
            @PathVariable String code,
            @PathVariable Integer version) {

        return ResponseEntity.ok(planCatalogueService.publish(code, version));
    }

    /**
     * Endpoint #6 — retires a plan from the catalogue.
     *
     * <p>Existing schools keep their plan, price, and features.
     * This does not cancel subscriptions.
     *
     * <p>Draft plans can also be retired.
     *
     * <p>No body required. A retired plan cannot be restored.
     */
    @PostMapping("/{code}/versions/{version}/retire")
    public ResponseEntity<PlanResponse> retire(
            @PathVariable String code,
            @PathVariable Integer version) {

        return ResponseEntity.ok(planCatalogueService.retire(code, version));
    }

    /**
     * Endpoint #7 — says whether a plan shows on the public list.
     *
     * <p>The difference between a plan a school can find and pick, and one that only exists in a
     * quote somebody sends them. A bespoke price for one large trust is published, sellable, and
     * deliberately not on the pricing page.
     *
     * <p><b>Idempotent</b>, unlike #4 and #6: this is a switch that can be flipped back, so
     * setting it to what it already is comes back {@code 200} saying so.
     *
     * <p>On its own it makes nothing buyable — a plan must also be {@code ACTIVE} and inside its
     * selling window. The response says which of those is still missing.
     */
    @PatchMapping("/{code}/versions/{version}/availability")
    public ResponseEntity<PlanResponse> setAvailability(
            @PathVariable String code,
            @PathVariable Integer version,
            @Valid @RequestBody PlanAvailabilityRequest request) {

        return ResponseEntity.ok(
                planCatalogueService.setAvailability(code, version, request));
    }

    /**
     * Endpoint #8 — the operator's list of every plan.
     *
     * <p>Every parameter is optional: a bare {@code GET /platform/plans} returns the first page
     * of the whole catalogue. Filters combine with AND, except {@code status}, which may be
     * repeated and means "any of these".
     *
     * <p><code>?status=ACTIVE&amp;publiclyAvailable=true&amp;search=prem&amp;page=0&amp;size=20&amp;sort=name,asc</code>
     *
     * <p>One row per plan <b>version</b>. The default order groups them: by code, newest version
     * of each first — the catalogue read as a menu.
     *
     * <p>Read-only, so no {@code @Transactional}.
     */
    @GetMapping
    public ResponseEntity<PageResponse<PlanSummaryResponse>> list(
            @RequestParam(required = false) List<PlanStatus> status,
            @RequestParam(required = false) String planCode,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Boolean publiclyAvailable,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sort) {

        // Bound one at a time rather than through @ModelAttribute, so a misspelled status comes
        // back through the type-mismatch handler naming the accepted values.
        PlanSearchRequest request = new PlanSearchRequest(
                status, planCode, name, publiclyAvailable, search, page, size, sort);

        return ResponseEntity.ok(planCatalogueService.listPlans(request));
    }
}
