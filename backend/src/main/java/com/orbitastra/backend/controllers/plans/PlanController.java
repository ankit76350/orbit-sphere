package com.orbitastra.backend.controllers.plans;

import java.net.URI;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.orbitastra.backend.dto.plans.catalogue.PlanCreateRequest;
import com.orbitastra.backend.dto.plans.catalogue.PlanResponse;
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
     * Endpoint #1 — makes a new plan.
     *
     * <p>It starts as a {@code DRAFT} at version 1, and not publicly available, so nobody can
     * buy it while the price is still being decided. {@code status}, {@code planVersion} and
     * {@code publiclyAvailable} are not on the request for that reason.
     *
     * <p>Created with an empty feature list; #3 sets the features.
     *
     * <p><b>{@code /drafts} is in the path deliberately.</b> The URL says what this makes, so a
     * caller cannot read {@code POST /platform/plans} and think they are putting a plan on sale.
     * Everything after this addresses the plan by code and version — {@code
     * /platform/plans/PREMIUM/versions/1} — because from then on the draft-ness is a status on a
     * plan that exists, not a different resource. This is the one endpoint where nothing exists
     * yet to name.
     */
    @PostMapping("/drafts")
    public ResponseEntity<PlanResponse> createDraft(@Valid @RequestBody PlanCreateRequest request) {
        PlanResponse response = planCatalogueService.createDraft(request);

        return ResponseEntity
                .created(URI.create("/platform/plans/" + response.planCode() + "/versions/"
                        + response.planVersion()))
                .body(response);
    }
}
