package com.orbitastra.backend.services.core;


import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.orbitastra.backend.common.error.exception.ApiException;
import com.orbitastra.backend.dto.core.CompleteProvisioningResponse;
import com.orbitastra.backend.dto.core.SchoolActivateResponse;
import com.orbitastra.backend.dto.core.SchoolCreateRequest;
import com.orbitastra.backend.dto.core.SchoolCreateResponse;
import com.orbitastra.backend.models.core.School;
import com.orbitastra.backend.models.core.enums.SchoolStatus;
import com.orbitastra.backend.models.identity.Role;
import com.orbitastra.backend.models.institution.NumberSequence;
import com.orbitastra.backend.models.institution.enums.NumberSequenceType;
import com.orbitastra.backend.models.institution.enums.SequenceResetPolicy;
import com.orbitastra.backend.models.plans.SchoolSubscription;
import com.orbitastra.backend.models.plans.enums.SubscriptionStatus;
import com.orbitastra.backend.repositories.core.SchoolRepository;
import com.orbitastra.backend.repositories.identity.RoleRepository;
import com.orbitastra.backend.repositories.institution.NumberSequenceRepository;
import com.orbitastra.backend.repositories.plans.SchoolSubscriptionRepository;
import com.orbitastra.backend.services.core.helper.CoreValidator;
import com.orbitastra.backend.services.core.helper.DefaultRoles;
import com.orbitastra.backend.services.core.helper.TextHelper;

import lombok.RequiredArgsConstructor;

/**
 * Creates a new tenant.
 *
 * <p><b>Right now this writes the School row and nothing else.</b> It originally also seeded a
 * NumberSequence for every type and a starting set of Roles; both were removed on 2026-08-21 to
 * be decided separately.
 *
 * <p>That leaves a real gap to close before the tenant is usable, and it is worth knowing which
 * way it fails. Almost every business document takes its human-readable number from a
 * NumberSequence row — an admission number, an invoice number, a receipt. With no row for a
 * type, the first attempt to admit a student has no counter to increment. And a UserAccount
 * points at Role ids, so with no roles there is nothing to attach the first administrator to.
 *
 * <p>Neither failure appears here. Both appear later, to somebody trying to use the school —
 * which is exactly what PROVISIONING as a starting status is for. Whatever creates the first
 * administrator will have to deal with both, or seeding comes back.
 *
 * <p>{@code @Transactional} is kept even though a single document write is already atomic in
 * MongoDB. When seeding returns, the boundary needs to be here and not remembered later.
 *
 * <p><b>Why no staff record and no user account, when the controller README said there would
 * be.</b> The plan had provisioning create the account holder's Staff row and first UserAccount
 * too, so a tenant arrived ready to log into. Writing it showed that cannot work honestly:
 * {@code Staff} requires {@code dateOfBirth} and {@code gender}, both non-null. A platform
 * operator provisioning a school for a client does not know the principal's date of birth, and
 * inventing one puts a false date into a staff record that payroll and government reporting will
 * later treat as fact. There is no safe placeholder for a real person's birthday.
 *
 * <p>The account holder on the contract and the school's first administrator are also not
 * necessarily the same person — a trustee may sign while an IT contractor does the setup. So
 * {@code School.accountHolderName} stays a plain name, and creating the first administrator is
 * its own endpoint with its own request that asks for what Staff actually requires.
 *
 * <p>That keeps this method's promise intact rather than weakening it: **School plus sequences
 * plus roles is a coherent unit** — a skeleton that can accept its first administrator — and
 * PROVISIONING is precisely the state for "exists, not usable yet".
 *
 * <p>All three writes are needed together. Without the sequences nothing else in the system can
 * ever be created, because almost every business document takes its number from one. Without the
 * roles there is nothing to attach the first account to. A School row on its own is not a tenant.
 */
@Service
@RequiredArgsConstructor
public class SchoolService {

    private final SchoolRepository schools;
    private final NumberSequenceRepository numberSequences;
    private final RoleRepository roles;
    private final SchoolSubscriptionRepository subscriptions;
    private final CoreValidator coreValidator;

    @Transactional
    public SchoolCreateResponse createNewSchool(SchoolCreateRequest request) {
        //! validating subdomain
        String subdomain = coreValidator.validateSubdomain(request.subdomain());
        String timeZone = coreValidator.validateTimeZone(request.defaultTimeZone());
        String countryCode = TextHelper.uppercaseOrNull(request.countryCode());

        // Checked before writing so the caller gets a clear message. The unique index is still
        // the real guard: two simultaneous requests both pass this, and the loser surfaces as a
        // DuplicateKeyException, which GlobalExceptionHandler turns into the same 409.
        if (schools.existsBySubdomain(subdomain)) {
            throw ApiException.conflict("SUBDOMAIN_TAKEN",
                    "The subdomain '" + subdomain + "' is already in use.");
        }

        //! step 1 - build the school info document
        School school = School.builder()
                .schoolName(request.schoolName().trim())
                .accountHolderName(request.accountHolderName().trim())
                .subdomain(subdomain)
                .phoneNumber(TextHelper.blankToNull(request.phoneNumber()))
                .emailAddress(TextHelper.lowercaseOrNull(request.emailAddress()))
                .defaultLocale(request.defaultLocale().trim())
                .defaultTimeZone(timeZone)
                .addressLine(TextHelper.blankToNull(request.addressLine()))
                .city(TextHelper.blankToNull(request.city()))
                .stateOrProvince(TextHelper.blankToNull(request.stateOrProvince()))
                .postalCode(TextHelper.blankToNull(request.postalCode()))
                .countryCode(countryCode)
                .status(request.initialStatus())
                .build();

        //! step 2 - save it with the new school
        School savedSchool = schools.save(school);

        return SchoolCreateResponse.fromSchool(savedSchool);
    }

    //! endpoint 2 — finish the setup -------------------------------------------------

        /**
         * Completes tenant setup with missing sequences and roles.
         *
         * <p>Idempotent: creates only missing data.
         *
         * <p>Works for all statuses except end-of-life statuses.
         */
    @Transactional
    public CompleteProvisioningResponse completeProvisioning(String schoolId) {
        // step 1 - find the school, or 404
        School school = schools.findById(schoolId)
                .orElseThrow(() -> ApiException.notFound("SCHOOL_NOT_FOUND",
                        "No school found with id '" + schoolId + "'."));

        // step 2 - refuse a shut-down tenant
        // Seeding one would quietly bring rows back to a school somebody deliberately closed.
        if (EnumSet.of(SchoolStatus.OFFBOARDING, SchoolStatus.CLOSED,
                SchoolStatus.DELETION_PENDING, SchoolStatus.DELETED).contains(school.getStatus())) {
            throw ApiException.conflict("SCHOOL_NOT_PROVISIONABLE",
                    "A school at status " + school.getStatus() + " cannot be provisioned.");
        }

        //!TODO: step 3 - save the missing number sequences
        int sequencesCreated = seedMissingNumberSequences(schoolId);
        int sequencesPresent = NumberSequenceType.values().length - sequencesCreated;

        //!TODO: step 4 - save the missing roles
        List<Role> wanted = DefaultRoles.forSchool(schoolId);
        int rolesCreated = seedMissingRoles(schoolId, wanted);
        int rolesPresent = wanted.size() - rolesCreated;

        // step 5 - read back what the school ended up with
        // Read from the database, not from `wanted`: a school may hold roles nobody here
        // created, and readyToActivate is about what exists rather than what we just added.
        List<String> roleKeys = roles.findBySchoolId(schoolId).stream()
                .map(Role::getRoleKey)
                .sorted()
                .collect(Collectors.toList());

        return CompleteProvisioningResponse.fromSchool(
                school, sequencesCreated, sequencesPresent, rolesCreated, rolesPresent, roleKeys);
    }

        /**
         ** Creates missing number sequences for all types.
         *
         ** <p>Uses GLOBAL scope and skips existing sequences.
         */
    private int seedMissingNumberSequences(String schoolId) {
        //! step 1 - read what the school already has
        Set<NumberSequenceType> existing = numberSequences.findBySchoolId(schoolId).stream()
                .map(NumberSequence::getSequenceType)
                .collect(Collectors.toSet());

        //! step 2 - build a document for every type that is missing
        List<NumberSequence> missing = new ArrayList<>();
        for (NumberSequenceType type : NumberSequenceType.values()) {
            if (existing.contains(type)) {
                continue;
            }
            missing.add(NumberSequence.builder()
                    .schoolId(schoolId)
                    .sequenceType(type)
                    .scopeKey("GLOBAL")
                    .nextValue(1L)
                    .paddingWidth(6)
                    .resetPolicy(SequenceResetPolicy.NEVER)
                    .build());
        }

        //! step 3 - save them, and return how many were saved
        // Nothing missing means no write at all, which is what makes a repeat call free.
        return missing.isEmpty() ? 0 : numberSequences.saveAll(missing).size();
    }

        /**
         ** Adds missing default roles.
         *
         ** <p>Matches roles by roleKey and keeps existing roles unchanged.
         */
    private int seedMissingRoles(String schoolId, List<Role> wanted) {
        //! step 1 - read the role keys the school already has
        Set<String> existingKeys = roles.findBySchoolId(schoolId).stream()
                .map(Role::getRoleKey)
                .collect(Collectors.toSet());

        //! step 2 - keep only the defaults that are not there yet
        List<Role> missing = wanted.stream()
                .filter(role -> !existingKeys.contains(role.getRoleKey()))
                .collect(Collectors.toList());

        //! step 3 - save them, and return how many were saved
        // An existing role is never touched, only skipped.
        return missing.isEmpty() ? 0 : roles.saveAll(missing).size();
    }



    //! endpoint 3 — activate the school ----------------------------------------------
        /**
         * Activates a school from TRIAL or PROVISIONING.
         *
         * <p>Rejects other statuses and records the activation date.
         */
    @Transactional
    public SchoolActivateResponse activateSchool(String schoolId) {
        //! step 1 - find the school, or 404
        School school = schools.findById(schoolId)
                .orElseThrow(() -> ApiException.notFound("SCHOOL_NOT_FOUND",
                        "No school found with id '" + schoolId + "'."));

        //! step 2 - only TRIAL and PROVISIONING may go live
        if (school.getStatus() != SchoolStatus.PROVISIONING
                && school.getStatus() != SchoolStatus.TRIAL) {
            throw ApiException.conflict("SCHOOL_NOT_ACTIVATABLE",
                    "A school at status " + school.getStatus() + " cannot be activated. Only "
                            + "PROVISIONING and TRIAL can. A suspended school is reactivated, "
                            + "not activated.");
        }

        //! step 3 - refuse a school nobody could log into
        // Without a SCHOOL_ADMIN role the first administrator account has nothing to hold, and
        // without the number sequences the first admission has no number to take. Activating
        // either way produces a live school that fails on first use.
        if (!roles.existsBySchoolIdAndRoleKey(schoolId, "SCHOOL_ADMIN")) {
            throw ApiException.conflict("SETUP_INCOMPLETE",
                    "This school has no SCHOOL_ADMIN role. Run complete-provisioning first.");
        }
        long sequenceCount = numberSequences.countBySchoolId(schoolId);
        if (sequenceCount < NumberSequenceType.values().length) {
            throw ApiException.conflict("SETUP_INCOMPLETE",
                    "This school has " + sequenceCount + " of "
                            + NumberSequenceType.values().length + " number sequences. Run "
                            + "complete-provisioning first.");
        }

        //! step 4 - check the subscription, where there is one to check
        Optional<SchoolSubscription> subscription =
                subscriptions.findBySchoolIdAndCurrentIsTrue(schoolId);
        String subscriptionStatus = subscription
                .map(s -> s.getStatus().name())
                .orElse("NONE");
        String subscriptionNote = subscriptionCheck(subscription);

        //! step 5 - go live, stamping activatedAt only the first time
        boolean firstActivation = school.getActivatedAt() == null;
        school.setStatus(SchoolStatus.ACTIVE);  //? ← changed, status
        if (firstActivation) {
            school.setActivatedAt(Instant.now()); //? ← changed, first time only
        }
        
        //TODO: save (status or activatedAt)
        School savedSchool = schools.save(school);

        return SchoolActivateResponse.fromSchool(
                savedSchool, firstActivation, subscriptionStatus, subscriptionNote);
    }

        /**
         ** Checks if the subscription allows activation.
         *
         ** <p>A missing subscription is allowed for now because the system does not create
         ** subscriptions yet. CANCELLED or EXPIRED subscriptions block activation.
         *
         ** <p>The response shows the subscription status so this can be made required later.
         */
    private String subscriptionCheck(Optional<SchoolSubscription> subscription) {
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
