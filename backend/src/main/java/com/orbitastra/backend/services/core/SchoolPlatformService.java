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

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import com.orbitastra.backend.common.web.PageResponse;
import com.orbitastra.backend.dto.core.platform.SchoolSearchRequest;
import com.orbitastra.backend.dto.core.platform.SchoolSummaryResponse;
import com.orbitastra.backend.common.error.exception.ApiException;
import com.orbitastra.backend.dto.core.platform.CompleteProvisioningResponse;
import com.orbitastra.backend.dto.core.platform.SchoolActivateResponse;
import com.orbitastra.backend.dto.core.platform.SchoolCreateRequest;
import com.orbitastra.backend.dto.core.platform.SchoolCreateResponse;
import com.orbitastra.backend.dto.core.platform.SchoolDetailResponse;
import com.orbitastra.backend.dto.core.platform.SchoolStatusResponse;
import com.orbitastra.backend.dto.core.platform.SchoolSubdomainRequest;
import com.orbitastra.backend.dto.core.platform.SchoolSubdomainResponse;
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
import com.orbitastra.backend.services.institution.NumberSequenceService;
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
public class SchoolPlatformService {

    /** Used when the caller does not say. Twenty rows is a screen without a scrollbar. */
    private static final int DEFAULT_PAGE_SIZE = 20;

    /** The most a caller may ask for at once, refused above rather than clamped. */
    private static final int MAX_PAGE_SIZE = 100;

    /** Newest first: an operator opening the console usually wants what just happened. */
    private static final Sort DEFAULT_SORT =
            Sort.by(Sort.Direction.DESC, "createdAt").and(Sort.by(Sort.Direction.ASC, "id"));

    /**
     * What G1 may sort on, and what each name means on the document.
     *
     * <p>An allow-list rather than a pass-through, and keyed on the API-facing name so
     * {@code sort=name} works without a caller knowing the field is {@code schoolName}.
     */
    private static final Map<String, String> SORTABLE_FIELDS = new LinkedHashMap<>(Map.of());

    static {
        // Keyed lowercase so sort=CreatedAt works, but the message below quotes the real
        // spelling — an error that answers in lowercase teaches the caller the wrong name.
        SORTABLE_FIELDS.put("name", "schoolName");
        SORTABLE_FIELDS.put("schoolname", "schoolName");
        SORTABLE_FIELDS.put("subdomain", "subdomain");
        SORTABLE_FIELDS.put("status", "status");
        SORTABLE_FIELDS.put("createdat", "createdAt");
        SORTABLE_FIELDS.put("updatedat", "updatedAt");
    }

    /** The same names as they should be typed, in a stable order, for the error message. */
    private static final String SORTABLE_FIELD_NAMES =
            "name, schoolName, subdomain, status, createdAt, updatedAt";

    private final SchoolRepository schools;
    private final NumberSequenceRepository numberSequences;
    private final RoleRepository roles;
    private final SchoolSubscriptionRepository subscriptions;
    private final CoreValidator coreValidator;

    //? endpoint 1 — create the tenant -------------------------------------------------

    @Transactional
    public SchoolCreateResponse createNewSchool(SchoolCreateRequest request) {
        //! validating subdomain
        String subdomain = coreValidator.validateSubdomain(request.subdomain());
        String timeZone = coreValidator.validateTimeZone(request.defaultTimeZone());
        String countryCode = TextHelper.uppercaseOrNull(request.countryCode());

        // Checked before writing so the caller gets a clear message. The unique index is still
        // the real guard: two simultaneous requests both pass this, and the loser surfaces as a
        // DuplicateKeyException, which GlobalExceptionHandler turns into the same 409.
        // TODO: check school subdomain exists
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
        // TODO: insert school
        School savedSchool = schools.save(school);

        return SchoolCreateResponse.fromSchool(savedSchool);
    }

    //? endpoint 2 — finish the setup --------------------------------------------------

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
        // TODO: read school
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

        //! step 3 - save the missing number sequences
        int sequencesCreated = seedMissingNumberSequences(schoolId);
        int sequencesPresent = NumberSequenceType.values().length - sequencesCreated;

        //! step 4 - save the missing roles
        List<Role> wanted = DefaultRoles.forSchool(schoolId);
        int rolesCreated = seedMissingRoles(schoolId, wanted);
        int rolesPresent = wanted.size() - rolesCreated;

        // step 5 - read back what the school ended up with
        // Read from the database, not from `wanted`: a school may hold roles nobody here
        // created, and readyToActivate is about what exists rather than what we just added.
        // TODO: read roles
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
        // TODO: read number sequences
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
                    .scopeKey(NumberSequenceService.GLOBAL_SCOPE)
                    .nextValue(1L)
                    .paddingWidth(6)
                    .resetPolicy(SequenceResetPolicy.NEVER)
                    .build());
        }

        //! step 3 - save them, and return how many were saved
        // Nothing missing means no write at all, which is what makes a repeat call free.
        if (missing.isEmpty()) {
            return 0;
        }

        // TODO: insert number sequences
        return numberSequences.saveAll(missing).size();
    }

        /**
         ** Adds missing default roles.
         *
         ** <p>Matches roles by roleKey and keeps existing roles unchanged.
         */
    private int seedMissingRoles(String schoolId, List<Role> wanted) {
        //! step 1 - read the role keys the school already has
        // TODO: read roles
        Set<String> existingKeys = roles.findBySchoolId(schoolId).stream()
                .map(Role::getRoleKey)
                .collect(Collectors.toSet());

        //! step 2 - keep only the defaults that are not there yet
        List<Role> missing = wanted.stream()
                .filter(role -> !existingKeys.contains(role.getRoleKey()))
                .collect(Collectors.toList());

        //! step 3 - save them, and return how many were saved
        // An existing role is never touched, only skipped.
        if (missing.isEmpty()) {
            return 0;
        }

        // TODO: insert roles
        return roles.saveAll(missing).size();
    }



    //? endpoint 3 — activate the school -----------------------------------------------
        /**
         * Activates a school from TRIAL or PROVISIONING.
         *
         * <p>Rejects other statuses and records the activation date.
         */
    @Transactional
    public SchoolActivateResponse activateSchool(String schoolId) {
        //! step 1 - find the school, or 404
        // TODO: read school
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
        // TODO: check role exists
        if (!roles.existsBySchoolIdAndRoleKey(schoolId, "SCHOOL_ADMIN")) {
            throw ApiException.conflict("SETUP_INCOMPLETE",
                    "This school has no SCHOOL_ADMIN role. Run complete-provisioning first.");
        }
        // TODO: count number sequences
        long sequenceCount = numberSequences.countBySchoolId(schoolId);
        if (sequenceCount < NumberSequenceType.values().length) {
            throw ApiException.conflict("SETUP_INCOMPLETE",
                    "This school has " + sequenceCount + " of "
                            + NumberSequenceType.values().length + " number sequences. Run "
                            + "complete-provisioning first.");
        }

        //! step 4 - check the subscription, where there is one to check
        // TODO: read subscription
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
        
        // TODO: update school
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

    //? endpoint 4 — suspend -----------------------------------------------------------

    // Suspends an ACTIVE school by changing its status to SUSPENDED and storing the reason and time.
    @Transactional
    public SchoolStatusResponse suspendSchool(String schoolId, String reason) {
        //! step 1 - find the school, or 404
        // TODO: read school
        School school = schools.findById(schoolId)
                .orElseThrow(() -> ApiException.notFound("SCHOOL_NOT_FOUND",
                        "No school found with id '" + schoolId + "'."));

        //! step 2 - only a live school can be suspended
        if (school.getStatus() != SchoolStatus.ACTIVE) {
            throw ApiException.conflict("SCHOOL_NOT_SUSPENDABLE",
                    "A school at status " + school.getStatus() + " cannot be suspended. Only "
                            + "ACTIVE can.");
        }

        //! step 3 - block it, recording when and why
        school.setStatus(SchoolStatus.SUSPENDED);
        school.setSuspendedAt(Instant.now());
        school.setStatusReason(reason.trim());

        // TODO: update school
        School savedSchool = schools.save(school);

        // NOT DONE HERE, and it matters: nothing kills the school's live AuthSessions or stops
        // its scheduled jobs. Those services do not exist yet, so a suspended school's users
        // stay logged in until their tokens expire. Suspension is currently a flag, not a lock.
        // Wire both in when sessions are built.
        return SchoolStatusResponse.fromSchool(savedSchool,
                "The school is blocked. Existing sessions are NOT yet revoked — see the service "
                        + "comment. Use reactivate to restore access.");
    }

    //? endpoint 5 — reactivate --------------------------------------------------------

   /**
     * Reactivates a suspended school by changing its status from SUSPENDED to ACTIVE.
     * Keeps the original activation and suspension history, and updates the status reason only when a new note is provided.
    */
    @Transactional
    public SchoolStatusResponse reactivateSchool(String schoolId, String note) {
        //! step 1 - find the school, or 404
        // TODO: read school
        School school = schools.findById(schoolId)
                .orElseThrow(() -> ApiException.notFound("SCHOOL_NOT_FOUND",
                        "No school found with id '" + schoolId + "'."));

        //! step 2 - only a suspended school can be reactivated
        if (school.getStatus() != SchoolStatus.SUSPENDED) {
            throw ApiException.conflict("SCHOOL_NOT_REACTIVATABLE",
                    "A school at status " + school.getStatus() + " cannot be reactivated. Only "
                            + "SUSPENDED can. A school that has never gone live is activated, "
                            + "not reactivated.");
        }

        //! step 3 - restore access, keeping the suspension history
        school.setStatus(SchoolStatus.ACTIVE);
        if (note != null && !note.isBlank()) {
            school.setStatusReason(note.trim());
        }

        // TODO: update school
        School savedSchool = schools.save(school);

        return SchoolStatusResponse.fromSchool(savedSchool,
                "The school is live again. suspendedAt and the reason are kept on purpose, as "
                        + "the record of the last suspension.");
    }

    //? endpoint 10 — change the subdomain ---------------------------------------------
    /**
     * #10 — changes the tenant label a school answers to.
     *
     * <p><b>Platform surface only.</b> This is not a profile edit; it is the key that resolves
     * every request to this tenant. Endpoint #6 deliberately has no field for it.
     *
     * <p><b>The old label is released, not held.</b> The moment this returns, the previous
     * subdomain is free and the next school to ask can have it. Nothing reserves it.
     *
     * <p><b>What this does not do.</b> Nothing invalidates a routing cache, rewrites a stored
     * link, or tells anybody at the school that their address changed — none of that exists yet.
     * Until it does, the response says plainly that every old link is now dead, because a caller
     * who does not know that will find out from the school.
     */
    @Transactional
    public SchoolSubdomainResponse changeSubdomain(String schoolId, SchoolSubdomainRequest request) {
        //! step 1 - find the school, or 404
        // TODO: read school
        School school = schools.findById(schoolId)
                .orElseThrow(() -> ApiException.notFound("SCHOOL_NOT_FOUND",
                        "No school found with id '" + schoolId + "'."));

        //! step 2 - a school on its way out does not get a new address
        if (school.getStatus() == SchoolStatus.DELETED
                || school.getStatus() == SchoolStatus.DELETION_PENDING) {
            throw ApiException.conflict("SCHOOL_NOT_EDITABLE",
                    "A school at status " + school.getStatus() + " cannot change its subdomain.");
        }

        //! step 3 - the caller must name the current subdomain correctly. This is the whole
        //! guard against doing it to the wrong tenant, so it runs before anything is touched.
        String confirmed = TextHelper.lowercaseOrNull(request.currentSubdomain());
        if (!school.getSubdomain().equals(confirmed)) {
            throw ApiException.conflict("SUBDOMAIN_CONFIRMATION_MISMATCH",
                    "This school answers to '" + school.getSubdomain() + "', not '" + confirmed
                            + "'. Send the current subdomain to confirm which school you mean.");
        }

        //! step 4 - shape, reserved words, normalization
        String newSubdomain = coreValidator.validateSubdomain(request.newSubdomain());

        String oldSubdomain = school.getSubdomain();
        if (newSubdomain.equals(oldSubdomain)) {
            throw ApiException.conflict("SUBDOMAIN_UNCHANGED",
                    "'" + newSubdomain + "' is already this school's subdomain.");
        }

        //! step 5 - nobody else may be using it
        // TODO: check school subdomain exists
        if (schools.existsBySubdomain(newSubdomain)) {
            throw ApiException.conflict("SUBDOMAIN_TAKEN",
                    "The subdomain '" + newSubdomain + "' is already in use.");
        }

        //! step 6 - move
        school.setSubdomain(newSubdomain);

        // TODO: update school
        School savedSchool = schools.save(school);

        return SchoolSubdomainResponse.fromSchool(savedSchool, oldSubdomain,
                "Every link, bookmark and saved login using '" + oldSubdomain + "' is now dead — "
                        + "nothing redirects, and the school has NOT been told. '" + oldSubdomain
                        + "' is now free for any school to claim.");
    }

    //? endpoint G1 — list the schools -------------------------------------------------
        /**
         * Lists schools with search, filters, sorting, and pagination.
         * Returns the latest schools by default.
        */
        public PageResponse<SchoolSummaryResponse> listSchools(SchoolSearchRequest request) {

        // Step 1: Get page and size
        int page = request.page() == null ? 0 : request.page();
        int size = request.size() == null ? DEFAULT_PAGE_SIZE : request.size();

        if (page < 0) {
                throw ApiException.badRequest(
                        "INVALID_PAGE",
                        "page cannot be negative. Received: " + page);
        }

        if (size < 1 || size > MAX_PAGE_SIZE) {
                throw ApiException.badRequest(
                        "INVALID_PAGE_SIZE",
                        "size must be between 1 and " + MAX_PAGE_SIZE + ". Received: " + size);
        }

        // Step 2: Build sorting
        Sort sort = DEFAULT_SORT;
        String rawSort = request.sort();

        if (rawSort != null && !rawSort.isBlank()) {

                String[] parts = rawSort.split(",");
                String requestedField = parts[0].trim();

                String field = SORTABLE_FIELDS.get(requestedField.toLowerCase());

                if (field == null) {
                throw ApiException.badRequest(
                        "INVALID_SORT_FIELD",
                        "'" + requestedField + "' cannot be sorted on. Allowed: "
                                + SORTABLE_FIELD_NAMES + ".");
                }

                Sort.Direction direction = Sort.Direction.ASC;

                if (parts.length > 1 && !parts[1].isBlank()) {
                String requestedDirection = parts[1].trim();

                if (requestedDirection.equalsIgnoreCase("desc")) {
                        direction = Sort.Direction.DESC;
                } else if (!requestedDirection.equalsIgnoreCase("asc")) {
                        throw ApiException.badRequest(
                                "INVALID_SORT_DIRECTION",
                                "'" + requestedDirection
                                        + "' is not a direction. Use asc or desc.");
                }
                }

                // Add id as a second sort so results stay consistent
                sort = Sort.by(direction, field)
                        .and(Sort.by(Sort.Direction.ASC, "id"));
        }

        // Step 3: Create pageable
        Pageable pageable = PageRequest.of(page, size, sort);

        // Step 4: Get schools from database
        // TODO: search schools
        Page<School> schoolPage = schools.search(request, pageable);

        // Step 5: Convert schools to response objects
        return PageResponse.from(
                schoolPage,
                SchoolSummaryResponse::fromSchool);
        }

    /**
     * G2 — reads one school in full for the operator.
     *
     * <p>Returns the school whatever its status, including a closed or deleted one. The
     * operator's console is exactly where somebody needs to look at a school that is no longer
     * running and see why, so hiding it here would only send them to the database instead.
     *
     * <p>This only reads, so there is no need for @Transactional.
     */
    public SchoolDetailResponse getSchool(String schoolId) {
        // TODO: read school
        School school = schools.findById(schoolId)
                .orElseThrow(() -> ApiException.notFound("SCHOOL_NOT_FOUND",
                        "No school found with id '" + schoolId + "'."));

        return SchoolDetailResponse.fromSchool(school);
    }
}
