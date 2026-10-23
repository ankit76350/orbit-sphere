package com.orbitastra.backend.services.plans;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.orbitastra.backend.common.error.exception.ApiException;
import com.orbitastra.backend.dto.plans.subscription.response.FeatureAccessResponse;
import com.orbitastra.backend.dto.plans.subscription.response.FeatureAccessResponse.Feature;
import com.orbitastra.backend.dto.plans.subscription.response.MySubscriptionResponse;
import com.orbitastra.backend.models.core.School;
import com.orbitastra.backend.models.plans.PlanDefinition;
import com.orbitastra.backend.models.plans.SchoolSubscription;
import com.orbitastra.backend.models.plans.embedded.PlanFeature;
import com.orbitastra.backend.models.plans.enums.OveragePolicy;
import com.orbitastra.backend.repositories.plans.plandefinition.PlanDefinitionRepository;
import com.orbitastra.backend.services.plans.utils.SchoolSubscriptionServiceUtils;
import com.orbitastra.backend.repositories.plans.schoolsubscription.SchoolSubscriptionRepository;

import lombok.RequiredArgsConstructor;

/**
 * Works out what a school is allowed to use. Endpoint #34, and the only place this is decided.
 *
 * <p><b>Every module that gates a feature asks this.</b> Nothing else may read
 * {@code plan_definitions.features} and decide for itself, because the moment two places work
 * feature access out they disagree — and they disagree quietly, in the direction of letting a
 * school use what it has not paid for. Transport checking "is TRANSPORT in the features list"
 * looks right and misses that the subscription was cancelled last month.
 *
 * <p>It is a class of its own rather than another method on
 * {@link PlatformSubscriptionService} so that the one place is easy to find. A module needing a gate
 * calls {@link #featureAccessFor(School)} directly; #34 is only the HTTP face of the same method.
 *
 * <h2>Two rules, and they are both here rather than at the call sites</h2>
 *
 * <p><b>A subscription has to be granting anything before a feature can be allowed.</b> TRIAL and
 * ACTIVE grant; PAST_DUE grants too, deliberately — an unpaid invoice is a conversation to have,
 * not a reason to lock a school out of its own attendance register mid-morning. SUSPENDED,
 * CANCELLED and EXPIRED grant nothing.
 *
 * <p><b>A period that has run out grants nothing either</b>, whatever the status says. Nothing
 * renews a subscription or marks one expired yet — #21 and #26 are not built — so a period
 * lapses while the status still reads ACTIVE. Trusting the status alone would keep a school on a
 * plan it stopped paying for, for as long as nobody noticed.
 */
@Service
@RequiredArgsConstructor
public class SchoolSubscriptionService {

    private final SchoolSubscriptionRepository schoolSubscription;
    private final PlanDefinitionRepository planDefinition;
    private final SchoolSubscriptionServiceUtils utils;






   //! Endpoint 33 — the school's own billing screen ----------------------------------
    public MySubscriptionResponse getMySubscription(School school) {

        //! step 1 - the school's one current subscription
        // TODO: read subscription
        SchoolSubscription subscription = schoolSubscription
                .findBySchoolIdAndCurrentIsTrue(school.getId())
                .orElseThrow(() -> ApiException.notFound("SUBSCRIPTION_NOT_FOUND",
                        "This school has no subscription."));

        //! step 2 - the plan it is on, for the name and the description
        // TODO: read plan
        PlanDefinition plan = planDefinition.findById(subscription.getPlanDefinitionDocsId())
                .orElseThrow(() -> ApiException.notFound("PLAN_NOT_FOUND",
                        "The plan this subscription points at no longer exists."));

        return MySubscriptionResponse.fromSubscription(subscription, plan);
    }

    //! Endpoint 34 — what this school may use right now -------------------------------

    /** #34 — Returns what the school can use right now. */
    public FeatureAccessResponse featureAccessFor(School school) {

        //! step 1 - the school's one current subscription
        // TODO: read subscription
        SchoolSubscription subscription = schoolSubscription
                .findBySchoolIdAndCurrentIsTrue(school.getId())
                .orElseThrow(() -> ApiException.notFound("SUBSCRIPTION_NOT_FOUND",
                        "This school has no subscription, so it is not entitled to anything."));

        //! step 2 - the plan it points at, for the features and the ceilings
        // TODO: read plan
        PlanDefinition plan = planDefinition.findById(subscription.getPlanDefinitionDocsId())
                .orElseThrow(() -> ApiException.notFound("PLAN_NOT_FOUND",
                        "The plan this subscription points at no longer exists."));

        //! step 3 - is the subscription granting anything at all
        String blocked = utils.whyNotActive(subscription, school.getDefaultTimeZone());
        boolean active = blocked == null;

        //! step 4 - each feature, with the answer already worked out
        List<Feature> features = new ArrayList<>();
        for (PlanFeature feature : plan.getFeatures() == null ? List.<PlanFeature>of()
                : plan.getFeatures()) {

            boolean includedInPlan = Boolean.TRUE.equals(feature.getEnabled());

            features.add(new Feature(
                    feature.getFeatureCode(),
                    feature.getFeatureCode().getLabel(),
                    includedInPlan,
                    // The subscription's state is folded in here, so a caller that reads only
                    // this field still gets the right answer.
                    includedInPlan && active,
                    feature.getUsageLimit(),
                    feature.getUsageMetric(),
                    feature.getOveragePolicy() == null
                            ? OveragePolicy.BLOCK
                            : feature.getOveragePolicy()));
        }

        //! step 5 - the ceilings in force: the school's negotiated limit where it has one
        Long maxStudents = subscription.getMaxStudentsOverride() != null
                ? subscription.getMaxStudentsOverride()
                : plan.getMaxStudents();
        Long maxUsers = subscription.getMaxUsersOverride() != null
                ? subscription.getMaxUsersOverride()
                : plan.getMaxUsers();

        return new FeatureAccessResponse(
                active,
                blocked,
                subscription.getSubscriptionNo(),
                subscription.getStatus(),
                plan.getName(),
                subscription.getPlanVersion(),
                subscription.getCurrentPeriodEnd(),
                maxStudents,
                maxUsers,
                features.size(),
                features);
    }
}
