package com.orbitastra.backend.services.plans;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.orbitastra.backend.common.web.PageResponse;
import com.orbitastra.backend.common.error.exception.ApiException;
import com.orbitastra.backend.dto.plans.catalogue.PlanCreateRequest;
import com.orbitastra.backend.dto.plans.catalogue.PlanDetailResponse;
import com.orbitastra.backend.dto.plans.catalogue.PlanDraftUpdateRequest;
import com.orbitastra.backend.dto.plans.catalogue.PlanAvailabilityRequest;
import com.orbitastra.backend.dto.plans.catalogue.PlanFeatureListResponse;
import com.orbitastra.backend.dto.plans.catalogue.PlanFeatureRequest;
import com.orbitastra.backend.dto.plans.catalogue.PlanResponse;
import com.orbitastra.backend.dto.plans.catalogue.PlanSearchRequest;
import com.orbitastra.backend.dto.plans.catalogue.PlanSummaryResponse;
import com.orbitastra.backend.dto.plans.catalogue.PlanVersionHistoryResponse;
import com.orbitastra.backend.models.plans.PlanDefinition;
import com.orbitastra.backend.models.plans.embedded.PlanFeature;
import com.orbitastra.backend.models.plans.enums.FeatureCode;
import com.orbitastra.backend.models.plans.enums.PlanStatus;
import com.orbitastra.backend.repositories.plans.PlanDefinitionRepository;
import com.orbitastra.backend.repositories.plans.SchoolSubscriptionRepository;
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

    /** Used when the caller does not say. Twenty rows is a screen without a scrollbar. */
    private static final int DEFAULT_PAGE_SIZE = 20;

    /** The most a caller may ask for at once, refused above rather than clamped. */
    private static final int MAX_PAGE_SIZE = 100;

    /**
     * What #8 may sort on, and what each name means on the document.
     *
     * <p>Keyed lowercase so {@code sort=CreatedAt} works, and an allow-list rather than a
     * pass-through so a caller cannot order by an unindexed field.
     */
    private static final Map<String, String> SORTABLE_PLAN_FIELDS = new LinkedHashMap<>();

    static {
        SORTABLE_PLAN_FIELDS.put("name", "name");
        SORTABLE_PLAN_FIELDS.put("plancode", "planCode");
        SORTABLE_PLAN_FIELDS.put("planversion", "planVersion");
        SORTABLE_PLAN_FIELDS.put("status", "status");
        SORTABLE_PLAN_FIELDS.put("listprice", "listPrice");
        SORTABLE_PLAN_FIELDS.put("createdat", "createdAt");
        SORTABLE_PLAN_FIELDS.put("updatedat", "updatedAt");
    }

    /** The same names spelled as they should be typed, for the error message. */
    private static final String SORTABLE_PLAN_FIELD_NAMES =
            "name, planCode, planVersion, status, listPrice, createdAt, updatedAt";

    private final PlanDefinitionRepository plans;
    private final SchoolSubscriptionRepository subscriptions;
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


    //! endpoint 7 — list it publicly, or not ------------------------------------------
    /**
     * #7 — says whether a plan shows on the public list.
     *
     * <p>The difference between a plan a school can find and pick for itself, and one that only
     * exists in a quote somebody sends them. A bespoke price for one large trust is a real plan —
     * published, sellable, and deliberately not on the pricing page.
     *
     * <p><b>It is a switch, so it is idempotent.</b> Setting it to what it already is comes back
     * {@code 200} saying so. That is the opposite of #4 and #6, and the reason is that those two
     * are one-way doors: nothing undoes a publish or a retire, so "it was already done" is a fact
     * the caller needs. This can be turned off again in one call, so a repeat costs nothing and
     * refusing it would only invite the caller to read first and then race.
     *
     * <p><b>On its own it makes nothing buyable.</b> A plan is sellable when it is {@code ACTIVE}
     * <i>and</i> public <i>and</i> inside its selling window — three separate facts. This
     * endpoint owns one of them, so the response says which of the others are still missing
     * rather than leaving somebody to wonder why a public plan is not on sale.
     *
     * <p><b>A retired plan cannot be listed.</b> Nobody can buy it, so advertising it would put
     * something on the pricing page that every attempt to purchase would refuse. Only that
     * direction is refused — taking a retired plan <i>off</i> the list is tidying up, and there
     * is no reason to stop somebody doing it.
     */
    @Transactional
    public PlanResponse setAvailability(String code, Integer version,
            PlanAvailabilityRequest request) {

        //! step 1 - find the plan, or 404
        PlanDefinition plan = loadPlan(code, version);

        boolean wanted = request.publiclyAvailable();

        //! step 2 - a retired plan is not something to advertise. Only the "on" direction is
        //! refused: taking a retired plan off the list is tidying up, and never wrong.
        if (wanted && plan.getStatus() == PlanStatus.RETIRED) {
            throw ApiException.conflict("PLAN_RETIRED",
                    "'" + plan.getPlanCode() + "' version " + plan.getPlanVersion() + " is "
                            + "retired, so nobody can buy it. Listing it publicly would "
                            + "advertise a plan every purchase would refuse.");
        }

        //! step 3 - nothing to do if it is already that way. A switch, not a door.
        if (Boolean.valueOf(wanted).equals(plan.getPubliclyAvailable())) {
            return PlanResponse.fromPlan(plan, (wanted
                    ? "It was already on the public list. "
                    : "It was already off the public list. ") + sellabilityNote(plan));
        }

        //! step 4 - flip it and save
        plan.setPubliclyAvailable(wanted);
        //TODO: save
        PlanDefinition savedPlan = plans.save(plan);

        // "still offered privately" is true of a live plan and false of a retired one, so it is
        // only said where it holds.
        String off = savedPlan.getStatus() == PlanStatus.RETIRED
                ? "Taken off the public list. "
                : "Taken off the public list. It can still be offered privately in a quote. ";

        return PlanResponse.fromPlan(savedPlan,
                (wanted ? "Now on the public list. " : off) + sellabilityNote(savedPlan));
    }


    //! endpoint 8 — list the catalogue ------------------------------------------------

        /**
         ** 8 — lists plans with optional filters, sorting, and pagination.
        */
        public PageResponse<PlanSummaryResponse> listPlans(PlanSearchRequest request) {

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

        Sort sort = Sort.by(
                Sort.Order.asc("planCode"),
                Sort.Order.desc("planVersion"));

        String rawSort = request.sort();

        if (rawSort != null && !rawSort.isBlank()) {
                String[] parts = rawSort.split(",");
                String requested = parts[0].trim();

                String field = SORTABLE_PLAN_FIELDS.get(requested.toLowerCase());

                if (field == null) {
                throw ApiException.badRequest(
                        "INVALID_SORT_FIELD",
                        "'" + requested + "' cannot be sorted on. Allowed: "
                                + SORTABLE_PLAN_FIELD_NAMES + ".");
                }

                Sort.Direction direction = Sort.Direction.ASC;

                if (parts.length > 1 && !parts[1].isBlank()) {
                String requestedDirection = parts[1].trim();

                if (requestedDirection.equalsIgnoreCase("desc")) {
                        direction = Sort.Direction.DESC;
                } else if (!requestedDirection.equalsIgnoreCase("asc")) {
                        throw ApiException.badRequest(
                                "INVALID_SORT_DIRECTION",
                                "'" + requestedDirection + "' is not a direction. Use asc or desc.");
                }
                }

                sort = Sort.by(direction, field).and(sort);
        }

        Pageable pageable = PageRequest.of(page, size, sort);

        //TODO: query
        Page<PlanDefinition> plansPage = plans.search(request, pageable);

        return PageResponse.from(plansPage, PlanSummaryResponse::fromPlan);
        }


    //! endpoint 9 — one plan's version history ----------------------------------------
    /**
     * #9 — every version of one plan, newest first.
     *
     * <p>Two questions, and they are the reason this is not just a filtered #8: <b>how did the
     * price move</b>, and <b>can the old versions be forgotten</b>. The first needs the versions
     * next to each other in order; the second needs to know who is still on each one.
     *
     * <p>So each row carries {@code priceChangeFromPrevious} — the subtraction done for the
     * reader rather than by them — and {@code schoolsOnThisVersion}.
     *
     * <p><b>Not paged.</b> A price does not change fifty times, and a caller reading a history
     * would have to stitch pages together to see the shape of it.
     */
    public PlanVersionHistoryResponse listVersions(String code) {
        //! step 1 - every version of that code, newest first
        String planCode = planValidator.normalizePlanCode(code);
        List<PlanDefinition> versions = plans.findByPlanCodeOrderByPlanVersionDesc(planCode);

        if (versions.isEmpty()) {
            throw ApiException.notFound("PLAN_NOT_FOUND",
                    "No plan '" + planCode + "' exists.");
        }

        //! step 2 - who is on each one. One indexed count per version rather than one query for
        //! all of them: a plan has a handful of versions, and a count that never loads a
        //! document is cheaper than fetching every subscription to group them here.
        Map<Integer, Long> schoolCounts = new LinkedHashMap<>();
        long onAnyVersion = 0;
        for (PlanDefinition version : versions) {
            long count = subscriptions.countByPlanDefinitionDocsId(version.getId());
            schoolCounts.put(version.getPlanVersion(), count);
            onAnyVersion += count;
        }

        //! step 3 - say plainly that the counts cannot be trusted yet, rather than letting a
        //! column of zeroes read as "this plan has no customers"
        String note = onAnyVersion == 0
                ? "Every schoolsOnThisVersion is 0 because nothing creates subscriptions yet — "
                        + "that is endpoint #13, which is not built. Read the zeroes as "
                        + "\"unknown\", not as \"nobody\"."
                : onAnyVersion + " subscription(s) point at this plan across all its versions.";

        return PlanVersionHistoryResponse.fromVersions(versions, schoolCounts, note);
    }


    //! endpoint 10 — one version in full ----------------------------------------------
    /**
     * #10 — one plan version, everything about it, features included.
     *
     * <p>What somebody sees after picking a row out of #8 or #9. The list endpoints report a
     * feature <i>count</i> so a page of rows stays readable; this is where the features
     * themselves are.
     *
     * <p>Each feature comes back with the label and description from {@code FeatureCode}, so a
     * screen showing "what this plan includes" does not have to keep its own copy of the wording
     * for twenty-four features.
     *
     * <p>Also carries {@code schoolsOnThisVersion}, which is not in the endpoint's field list.
     * It is the question somebody looking at one version has — can this be retired, or is
     * somebody still on it — and answering it costs one indexed count.
     */
    public PlanDetailResponse getVersion(String code, Integer version) {
        //! step 1 - find the plan, or 404
        PlanDefinition plan = loadPlan(code, version);

        //! step 2 - who is on it
        long schoolsOnThisVersion = subscriptions.countByPlanDefinitionDocsId(plan.getId());

        //! step 3 - a zero that cannot yet be trusted has to say so
        String note = schoolsOnThisVersion == 0
                ? "schoolsOnThisVersion is 0 because nothing creates subscriptions yet — that is "
                        + "endpoint #13, which is not built. Read it as \"unknown\", not as "
                        + "\"nobody\"."
                : null;

        return PlanDetailResponse.fromPlan(plan, schoolsOnThisVersion, note);
    }

    //* ---------------------------------------------------------------------------------

    /**
     * Why the plan can or cannot be bought right now, in a sentence.
     *
     * <p>Sellability is three facts — published, public, and inside the selling window — and
     * only one of them is what #7 changes. Without this, a caller who has just made a plan
     * public and still sees {@code sellable: false} has no way to tell which of the other two is
     * missing, and the obvious guess is that the call failed.
     */
    private String sellabilityNote(PlanDefinition plan) {
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
            return "It is not sellable yet: it goes on sale on " + plan.getEffectiveFrom() + ".";
        }
        if (plan.getEffectiveUntil() != null && !plan.getEffectiveUntil().isAfter(now)) {
            return "It is not sellable: it stopped being sold on " + plan.getEffectiveUntil()
                    + ".";
        }
        return "Schools can now pick it.";
    }

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
