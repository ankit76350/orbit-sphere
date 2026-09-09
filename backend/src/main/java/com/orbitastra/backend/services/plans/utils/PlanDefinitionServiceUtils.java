package com.orbitastra.backend.services.plans.utils;

import java.time.Instant;

import org.springframework.stereotype.Component;

import com.orbitastra.backend.common.error.exception.ApiException;
import com.orbitastra.backend.common.time.Dates;
import com.orbitastra.backend.models.plans.PlanDefinition;
import com.orbitastra.backend.models.plans.enums.PlanStatus;
import com.orbitastra.backend.repositories.plans.plandefinition.PlanDefinitionRepository;
import com.orbitastra.backend.services.plans.helper.PlansHelper;

import lombok.RequiredArgsConstructor;

/**
 * The shared bits every plan definition endpoint needs.
 *
 * <p>Moved out of {@code PlanDefinitionService} so that file holds the endpoints and nothing
 * else, the same as the two subscription services. It had three private helpers under its last
 * endpoint, and two of them are used by most of the nine.
 *
 * <p>A {@code @Component} rather than a static class because {@link #loadPlanVersion} needs the
 * repository.
 *
 * <p>Nothing here decides what an endpoint does. These find a plan version, check it is still a
 * draft, and say whether a school could buy it. Every method says which endpoints use it, in the
 * note above it.
 */
@Component
@RequiredArgsConstructor
public class PlanDefinitionServiceUtils {

    private final PlanDefinitionRepository plans;
    private final PlansHelper helper;

    /**
     * Why the plan can or cannot be bought right now, in a sentence.
     *
     * <p>Sellability is three facts — published, public, and inside the selling window — and
     * only one of them is what #7 changes. Without this, a caller who has just made a plan
     * public and still sees {@code sellable: false} has no way to tell which of the other two is
     * missing, and the obvious guess is that the call failed.
     *
     * Used by:
     * - setAvailability()
     */
    public String sellabilityNote(PlanDefinition plan) {
        if (plan.getStatus() == PlanStatus.RETIRED) {
            // Checked before the public-list line, because for a retired plan that flag is not
            // the reason it cannot be sold and saying so would send somebody to fix the wrong
            // thing.
            return "It is not sellable, and cannot become sellable: it is retired.";
        }
        if (!Boolean.TRUE.equals(plan.getPubliclyAvailable())) {
            return "It is not sellable: a plan has to be on the public list to be picked.";
        }
        if (plan.getStatus() != PlanStatus.ACTIVE) {
            return "It is NOT sellable yet — it is still a " + plan.getStatus()
                    + ". Publish it to put it on sale.";
        }

        Instant now = Instant.now();
        if (plan.getEffectiveFrom() != null && plan.getEffectiveFrom().isAfter(now)) {
            return "It is not sellable yet: it goes on sale on "
                    + Dates.readable(plan.getEffectiveFrom()) + ".";
        }
        if (plan.getEffectiveUntil() != null && !plan.getEffectiveUntil().isAfter(now)) {
            return "It is not sellable: it stopped being sold on "
                    + Dates.readable(plan.getEffectiveUntil()) + ".";
        }
        return "Schools can now pick it.";
    }

    /**
     * Refuses anything but a draft.
     *
     * <p>The rule every plan definition follows, in one place: a published plan may have
     * schools on it, so changing what it costs or what it includes would change what somebody
     * already agreed to without anybody agreeing to it. A retired plan is refused for the same
     * reason — schools may still be on it.
     *
     * <p>{@code what} completes the sentence, so each endpoint says which change was refused
     * rather than all of them sharing one vague message.
     *
     * Used by:
     * - publish()
     * - replaceFeatures()
     * - updateDraft()
     */
    public void requireDraft(PlanDefinition plan, String what) {
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
     *
     * Used by:
     * - getVersion()
     * - publish()
     * - replaceFeatures()
     * - retire()
     * - setAvailability()
     * - updateDraft()
     */
    public PlanDefinition loadPlanVersion(String code, Integer version) {
        String planCode = helper.normalizePlanCode(code);
        // TODO: read plan
        return plans.findByPlanCodeAndPlanVersion(planCode, version)
                .orElseThrow(() -> ApiException.notFound("PLAN_NOT_FOUND",
                        "No plan '" + planCode + "' version " + version + " exists."));
    }
}
