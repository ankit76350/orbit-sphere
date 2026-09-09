package com.orbitastra.backend.services.core.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.orbitastra.backend.common.error.exception.ApiException;
import com.orbitastra.backend.models.identity.Role;
import com.orbitastra.backend.models.identity.embedded.RoleDefinition;
import com.orbitastra.backend.models.institution.NumberSequence;
import com.orbitastra.backend.models.institution.embedded.SequenceCounter;
import com.orbitastra.backend.models.institution.enums.NumberSequenceType;
import com.orbitastra.backend.models.institution.enums.SequenceResetPolicy;
import com.orbitastra.backend.models.plans.SchoolSubscription;
import com.orbitastra.backend.models.plans.enums.SubscriptionStatus;
import com.orbitastra.backend.repositories.identity.RoleRepository;
import com.orbitastra.backend.repositories.institution.NumberSequenceRepository;
import com.orbitastra.backend.services.institution.NumberSequenceService;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Component;

import com.orbitastra.backend.common.error.exception.ApiException;
import com.orbitastra.backend.models.identity.Role;
import com.orbitastra.backend.models.identity.embedded.RoleDefinition;
import com.orbitastra.backend.models.institution.NumberSequence;
import com.orbitastra.backend.models.institution.embedded.SequenceCounter;
import com.orbitastra.backend.models.institution.enums.NumberSequenceType;
import com.orbitastra.backend.models.institution.enums.SequenceResetPolicy;
import com.orbitastra.backend.models.plans.SchoolSubscription;
import com.orbitastra.backend.models.plans.enums.SubscriptionStatus;
import com.orbitastra.backend.services.institution.NumberSequenceService;

import lombok.RequiredArgsConstructor;

/**
 * The bits {@code SchoolPlatformService} needs and its endpoints do not read as.
 *
 * <p>Moved out so that file holds the endpoints and nothing else, the same as the plans module.
 * Two of these seed what a new school cannot work without — its number sequences and its roles —
 * and the third turns a subscription into a sentence for the activation response.
 *
 * <p>A {@code @Component} because they need repositories.
 *
 * <p>Every method says which endpoints use it, in the note above it.
 */
@Component
@RequiredArgsConstructor
public class SchoolPlatformServiceUtils {

    private final NumberSequenceRepository numberSequences;
    private final RoleRepository roles;

    /**
     * Adds a counter for every {@link NumberSequenceType} the school is short of.
     *
     * <p><b>Only the gaps, and only when there are gaps.</b> Nothing missing means no write at
     * all, which is what makes a repeat call free — and pushing the missing entries leaves every
     * existing counter's {@code nextValue} exactly where it was. Saving the whole document back
     * would be the way to reset a school's numbering by accident.
     *
     * <p>The school's first run is a {@code save} rather than an update, so the auditing hook
     * fills in {@code createdAt} and {@code createdByDocsId}; an update would leave both null.
     *
     * @return how many counters were written, which the response reports against how many were
     *         already there
          *
     * Used by:
     * - completeProvisioning()
     */
    public int seedMissingNumberSequences(String schoolId) {
        //! step 1 - read the school's one counters document, if it has one yet
        Optional<NumberSequence> document = numberSequences.findBySchoolId(schoolId);

        Set<NumberSequenceType> existing = document
                .map(NumberSequence::getCounters)
                .orElseGet(List::of)
                .stream()
                .map(SequenceCounter::getSequenceType)
                .collect(Collectors.toSet());

        //! step 2 - build a counter for every type that is missing
        List<SequenceCounter> missing = new ArrayList<>();
        for (NumberSequenceType type : NumberSequenceType.values()) {
            if (existing.contains(type)) {
                continue;
            }
            missing.add(SequenceCounter.builder()
                    .sequenceType(type)
                    .scopeKey(NumberSequenceService.GLOBAL_SCOPE)
                    .nextValue(1L)
                    .paddingWidth(6)
                    .resetPolicy(SequenceResetPolicy.NEVER)
                    .build());
        }

        //! step 3 - write them, and return how many were written
        // Nothing missing means no write at all, which is what makes a repeat call free.
        if (missing.isEmpty()) {
            return 0;
        }

        if (document.isEmpty()) {
            // First run for this school: a save, so the auditing hook fills in createdAt and
            // createdByDocsId. An update would leave both null.
            numberSequences.save(NumberSequence.builder()
                    .schoolId(schoolId)
                    .counters(missing)
                    .build());
            return missing.size();
        }

        // The document is already there, so add only what it is short of. Pushing the missing
        // entries leaves every existing counter's nextValue exactly where it was — saving the
        // whole document back would be the way to reset a school's numbering by accident.
        return numberSequences.addCounters(schoolId, missing);
    }

    /**
     * Adds missing default roles.
     *
     * <p>Matches roles by roleKey and keeps existing roles unchanged.
          *
     * Used by:
     * - completeProvisioning()
     */
    public int seedMissingRoles(String schoolId, List<RoleDefinition> wanted) {
        //! step 1 - read the school's one roles document, if it has one yet
        Optional<Role> document = roles.findBySchoolId(schoolId);

        Set<String> existingKeys = document
                .map(Role::getRoles)
                .orElseGet(List::of)
                .stream()
                .map(RoleDefinition::getRoleKey)
                .collect(Collectors.toSet());

        //! step 2 - keep only the defaults that are not there yet
        List<RoleDefinition> missing = wanted.stream()
                .filter(role -> !existingKeys.contains(role.getRoleKey()))
                .collect(Collectors.toList());

        //! step 3 - write them, and return how many were written
        // An existing role is never touched, only skipped. That matters more here than for the
        // counters: a school may have edited SCHOOL_ADMIN's permissions, and re-running
        // provisioning must not put our defaults back over the top of that.
        if (missing.isEmpty()) {
            return 0;
        }

        if (document.isEmpty()) {
            roles.save(Role.builder()
                    .schoolId(schoolId)
                    .roles(missing)
                    .build());
            return missing.size();
        }
        return roles.addRoles(schoolId, missing);
    }

    /**
     * Checks if the subscription allows activation.
     *
     * <p>A missing subscription is allowed for now because the system does not create
     * subscriptions yet. CANCELLED or EXPIRED subscriptions block activation.
     *
     * <p>The response shows the subscription status so this can be made required later.
          *
     * Used by:
     * - activateSchool()
     */
    public String describeSubscriptionForActivation(Optional<SchoolSubscription> subscription) {
        if (subscription.isEmpty()) {
            return "No subscription exists for this school. Activation was allowed anyway "
                    + "because nothing creates subscriptions yet — this check must become a "
                    + "hard requirement once it does.";
        }
        SubscriptionStatus status = subscription.get().getStatus();
        if (status == SubscriptionStatus.CANCELLED || status == SubscriptionStatus.EXPIRED) {
            throw ApiException.conflict("SUBSCRIPTION_NOT_ACTIVE",
                    "The school's subscription is " + status + ". It cannot be activated.");
        }
        return "Subscription is " + status + ".";
    }
}
