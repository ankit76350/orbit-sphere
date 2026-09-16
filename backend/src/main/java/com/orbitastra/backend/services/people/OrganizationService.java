package com.orbitastra.backend.services.people;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.orbitastra.backend.common.current.CurrentSchoolResolver;
import com.orbitastra.backend.common.error.exception.ApiException;
import com.orbitastra.backend.common.text.TextHelper;
import com.orbitastra.backend.common.web.PageResponse;
import com.orbitastra.backend.dto.people.organization.request.DepartmentCreateRequest;
import com.orbitastra.backend.dto.people.organization.request.DepartmentSearchRequest;
import com.orbitastra.backend.dto.people.organization.request.DepartmentUpdateRequest;
import com.orbitastra.backend.dto.people.organization.request.PositionCreateRequest;
import com.orbitastra.backend.dto.people.organization.request.PositionSearchRequest;
import com.orbitastra.backend.dto.people.organization.request.PositionUpdateRequest;
import com.orbitastra.backend.dto.people.organization.response.DepartmentDetailResponse;
import com.orbitastra.backend.dto.people.organization.response.DepartmentNodeResponse;
import com.orbitastra.backend.dto.people.organization.response.DepartmentResponse;
import com.orbitastra.backend.dto.people.organization.response.DepartmentSummaryResponse;
import com.orbitastra.backend.dto.people.organization.response.DepartmentTreeResponse;
import com.orbitastra.backend.dto.people.organization.response.StaffSummaryResponse;
import com.orbitastra.backend.dto.people.organization.response.PositionDetailResponse;
import com.orbitastra.backend.dto.people.organization.response.PositionHolderResponse;
import com.orbitastra.backend.dto.people.organization.response.PositionResponse;
import com.orbitastra.backend.dto.people.organization.response.PositionRowResponse;
import com.orbitastra.backend.models.core.School;
import com.orbitastra.backend.models.people.organization.Department;
import com.orbitastra.backend.models.people.organization.Position;
import com.orbitastra.backend.models.people.staff.EmploymentRecord;
import com.orbitastra.backend.models.people.staff.Staff;
import com.orbitastra.backend.repositories.people.organization.DepartmentRepository;
import com.orbitastra.backend.repositories.people.organization.PositionRepository;
import com.orbitastra.backend.repositories.people.staff.EmploymentRecordRepository;
import com.orbitastra.backend.repositories.people.staff.StaffRepository;
import com.orbitastra.backend.services.people.utils.OrganizationServiceUtils;

import lombok.RequiredArgsConstructor;

/**
 * The org chart a school hires into — endpoints #9 to #15 of the plan in
 * {@code controllers/people/organization/README.md}. #9, #10, #12, #13 and #52 are built.
 *
 * <p><b>This is where the people module starts, which surprises people.</b> {@code POST /staff}
 * looks like the first call, but the write that actually employs somebody needs a
 * {@code positionDocsId}, and a position needs a department. #9 is the first call anyone makes
 * against this product's people module.
 *
 * <p><b>One service for two documents</b>, because a position outside a department is not a thing.
 *
 * <p><b>The gates run in the controller</b>, not here.
 */
@Service
@RequiredArgsConstructor
public class OrganizationService {

    private final CurrentSchoolResolver currentSchool;
    private final DepartmentRepository departments;
    private final PositionRepository positions;
    private final StaffRepository staff;

    /**
     * Read by #15 and #53, and written by neither.
     *
     * <p><b>The org chart's two read endpoints reach into the staff module's collection</b>,
     * because a filled headcount is a fact about seats that is only recorded against people.
     * The alternative is a counter on {@code Position} that drifts the first time a writer
     * forgets it.
     */
    private final EmploymentRecordRepository employments;
    private final OrganizationServiceUtils utils;

    /**
     * What {@code ?sort=} accepts, keyed by the lower-cased name a caller types.
     *
     * <p>An allowlist rather than a pass-through: an arbitrary field name reaching a Mongo sort is
     * how a caller sorts on something unindexed and makes the database read every row to answer.
     */
    private static final Map<String, String> SORTABLE_DEPARTMENT_FIELDS = new LinkedHashMap<>();

    static {
        SORTABLE_DEPARTMENT_FIELDS.put("name", "name");
        SORTABLE_DEPARTMENT_FIELDS.put("departmentcode", "departmentCode");
        SORTABLE_DEPARTMENT_FIELDS.put("createdat", "createdAt");
        SORTABLE_DEPARTMENT_FIELDS.put("updatedat", "updatedAt");
    }

    /** The same set as a sentence, for the refusal to list. */
    private static final String SORTABLE_DEPARTMENT_FIELD_NAMES =
            SORTABLE_DEPARTMENT_FIELDS.values().stream().collect(Collectors.joining(", "));

    /**
     * The default order, and the tiebreaker on every other sort.
     *
     * <p><b>Two keys, because the first is not unique.</b> Two units may share a {@code name} —
     * the index says so, and two "Science" units under different parents is a real org chart. So
     * {@code name} alone would tie, and a tie with no tiebreaker puts one row on two pages while
     * another appears on none. {@code departmentCode} is unique per school and settles it.
     *
     * <p>{@link PageResponse#pageableOf} appends whichever of these the caller did not name, so
     * {@code ?sort=createdAt} is really {@code createdAt, name, departmentCode}.
     */
    private static final Sort DEPARTMENT_ORDER =
            Sort.by(Sort.Order.asc("name"), Sort.Order.asc("departmentCode"));

    /**
     * What {@code ?sort=} accepts on #15, keyed by the lower-cased name a caller types.
     *
     * <p>An allowlist rather than a pass-through, for the reason the department one is: an
     * arbitrary field name reaching a Mongo sort is how a caller sorts on something unindexed and
     * makes the database read every row to answer.
     *
     * <p><b>{@code filledHeadcount} is deliberately not on it, though it is the most interesting
     * column on the page.</b> It is not a field — it is counted from another collection after the
     * page has been chosen, so sorting by it would mean counting the whole collection first. The
     * one query that shape would serve is "which seats are emptiest", and {@code ?vacant=true}
     * answers the useful half of that without the cost.
     */
    private static final Map<String, String> SORTABLE_POSITION_FIELDS = new LinkedHashMap<>();

    static {
        SORTABLE_POSITION_FIELDS.put("title", "title");
        SORTABLE_POSITION_FIELDS.put("approvedheadcount", "approvedHeadcount");
        SORTABLE_POSITION_FIELDS.put("createdat", "createdAt");
        SORTABLE_POSITION_FIELDS.put("updatedat", "updatedAt");
    }

    /** The same set as a sentence, for the refusal to list. */
    private static final String SORTABLE_POSITION_FIELD_NAMES =
            SORTABLE_POSITION_FIELDS.values().stream().collect(Collectors.joining(", "));

    /**
     * The default order on #15, and the tiebreaker on every other sort.
     *
     * <p><b>Title within department</b>, which is how an org chart reads and what the plan asked
     * for. The pair is also <b>unique per school</b> — {@code school_department_title_uniq} says a
     * department cannot hold two seats with the same title — so it cannot tie, and a tie with no
     * tiebreaker puts one row on two pages while another appears on none.
     *
     * <p><b>{@code school_department_position_active_idx} serves this fully only when
     * {@code ?departmentDocsId=} and {@code ?active=} are both sent</b>, because {@code active}
     * sits between them in the key. With neither, Mongo sorts what the filter returned. That is
     * the honest version; the plan's claim that the index is why this is the default order is
     * true of the shape and not of every call.
     */
    private static final Sort POSITION_ORDER =
            Sort.by(Sort.Order.asc("departmentDocsId"), Sort.Order.asc("title"));

    /** What #53 says instead of returning an empty list with no explanation. */
    private static final String NOBODY_HOLDS_IT =
            "Nobody currently holds this position. That is a real state, not a missing record: "
                    + "the seat is approved and unfilled.";

    /** Repeated on every response until permissions exist. Deliberately hard to miss. */
    private static final String NO_AUTHORIZATION_YET =
            "No authorization is enforced on this endpoint yet: any caller who can reach it can "
                    + "run it.";

    /**
     * Endpoint #9 — create an org unit, optionally under another.
     *
     * <p><b>No cycle walk here, and none anywhere.</b> A brand-new department has no children, so
     * it cannot be its own ancestor whatever parent it names. The walk was to belong to #10, which
     * would have moved an existing unit under its own descendant — but #10 does not accept a
     * parent, so nothing in this API can write a cycle and {@code PeopleHelper} is still unearned.
     */
    public DepartmentResponse createDepartment(DepartmentCreateRequest request) {

        //! step 1 - who is asking
        School school = currentSchool.requireUsable();

        //! step 2 - the code is the school's business key and is never derived from the name.
        //! Stored uppercased and trimmed so "academics" and "ACADEMICS" cannot both exist: a
        //! person typing a filter should not have to know which case the school used that day.
        String departmentCode = request.departmentCode().trim().toUpperCase();

        //! step 3 - and it has to be free.
        //!
        //! THIS CHECK IS THE ENFORCEMENT, not a nicety in front of the index.
        //! school_department_code_uniq is declared on the model but built on demand
        //! (app.mongo.sync-indexes), so a database that has never synced carries no such
        //! constraint at all - where it IS built this turns a duplicate-key 500 into this 409.
        // TODO: check department exists
        if (departments.existsBySchoolIdAndDepartmentCode(school.getId(), departmentCode)) {
            throw ApiException.conflict("DEPARTMENT_CODE_TAKEN",
                    "This school already has a department coded " + departmentCode
                            + ". A code is the key twenty positions may reference, so it stays "
                            + "taken once used — pick another, or read the existing unit first.");
        }

        //! step 4 - the parent, when one was named. Scoped by schoolId, never by id alone:
        //! another school's department id is real and would otherwise nest this unit under it.
        String parentId = TextHelper.blankToNull(request.parentDepartmentDocsId());
        if (parentId != null) {
            utils.loadDepartment(school, parentId);
        }

        //! step 5 - the head, when one was named. Checked to EXIST, and deliberately not checked
        //! to be employed: during setup a school enters its org chart before its employment
        //! records, and refusing this would make it work backwards.
        String headId = TextHelper.blankToNull(request.headStaffDocsId());
        if (headId != null) {
            // TODO: read staff
            staff.findByIdAndSchoolId(headId, school.getId())
                    .orElseThrow(() -> ApiException.notFound("STAFF_NOT_FOUND",
                            "No staff member with id '" + headId + "' in this school."));
        }

        //! step 6 - build it. `active` takes its default: a unit created already retired is a
        //! state nothing asked for, and retiring one is #10.
        Department unit = Department.builder()
                .schoolId(school.getId())
                .departmentCode(departmentCode)
                .name(request.name().trim())
                .description(TextHelper.blankToNull(request.description()))
                .parentDepartmentDocsId(parentId)
                .headStaffDocsId(headId)
                .build();

        //! step 7 - insert it
        // TODO: insert department
        Department saved = departments.save(unit);

        return DepartmentResponse.fromDepartment(saved,
                "Positions hang off this unit — #13 creates one. " + NO_AUTHORIZATION_YET);
    }

    /**
     * Endpoint #10 — rename a unit, describe it, name its head, retire it or restore it.
     *
     * <p><b>No cycle walk here either, and now there never will be one.</b> The plan had this
     * endpoint moving a unit under another and refusing a cycle at {@code DEPARTMENT_CYCLE}; the
     * parent was dropped from the request on 2026-09-15, so nothing in this API can write a cycle
     * at all. #12's visited set stays — it is what makes that a property of the data rather than
     * of this method's current shape.
     *
     * <p><b>{@code active} lives here rather than on #11's endpoint pair</b>, and it carries #11's
     * refusal with it: retiring a unit that still holds active seats is a 409 naming how many. The
     * rule belongs to the transition, not to whichever endpoint performs it.
     */
    public DepartmentResponse updateDepartment(String departmentDocsId,
            DepartmentUpdateRequest request) {

        //! step 1 - who is asking
        School school = currentSchool.requireUsable();

        //! step 2 - refuse a request that asks for nothing, BEFORE reading anything. A PATCH that
        //! changes nothing and answers 200 lets a client with a broken form look healthy.
        if (request.isEmpty()) {
            throw ApiException.badRequest("NOTHING_TO_UPDATE",
                    "Send name, description, headStaffDocsId or active. departmentCode and "
                            + "parentDepartmentDocsId are not editable — a code is what exports "
                            + "and filters are written against, and where a unit sits is decided "
                            + "when it is created.");
        }

        //! step 3 - the unit, scoped to the school
        Department department = utils.loadDepartment(school, departmentDocsId);

        //! step 4 - the name. Blank is REFUSED rather than clearing, because the model requires
        //! one and it is the only thing on this document a person reads.
        if (request.name() != null) {
            String newName = request.name().trim();
            if (newName.isEmpty()) {
                throw ApiException.badRequest("DEPARTMENT_NAME_REQUIRED",
                        "A department name cannot be removed. Send a new one, or omit the field.");
            }
            department.setName(newName);
        }

        //! step 5 - the description. "" removes it; absent leaves it alone.
        if (request.description() != null) {
            department.setDescription(TextHelper.blankToNull(request.description()));
        }

        //! step 6 - the head. "" leaves the unit without one; a real id has to EXIST, and is
        //! deliberately not checked to be employed - the same rule #9 follows, because a school
        //! enters its org chart before its employment records.
        if (request.headStaffDocsId() != null) {
            String headId = TextHelper.blankToNull(request.headStaffDocsId());
            if (headId != null) {
                // TODO: read staff
                staff.findByIdAndSchoolId(headId, school.getId())
                        .orElseThrow(() -> ApiException.notFound("STAFF_NOT_FOUND",
                                "No staff member with id '" + headId + "' in this school."));
            }
            department.setHeadStaffDocsId(headId);
        }

        //! step 7 - retiring, and the one refusal this endpoint has of its own.
        //!
        //! A RETIRED UNIT HOLDING ACTIVE SEATS IS A CHART NOTHING CAN DRAW, and possibly people
        //! employed into a department that no longer exists. The count is in the message because
        //! "retire the four seats first" is actionable and "not empty" is not.
        //!
        //! SUB-DEPARTMENTS ARE NOT CHECKED, and that asymmetry is deliberate: #12 already answers
        //! for a retired parent whose children are still active by lifting them to the top and
        //! marking them `liftedToTop`. That state is designed for. A seat has no such answer.
        //!
        //! Restoring has no check and needs none.
        if (request.active() != null && !request.active().equals(department.getActive())) {
            if (Boolean.FALSE.equals(request.active())) {
                // TODO: count positions
                long stillActive = positions.countBySchoolIdAndDepartmentDocsIdAndActiveIsTrue(
                        school.getId(), department.getId());

                if (stillActive > 0) {
                    throw ApiException.conflict("DEPARTMENT_NOT_EMPTY",
                            stillActive + " position" + (stillActive == 1 ? " is" : "s are")
                                    + " still active in '" + department.getName()
                                    + "'. Retire them first — a retired unit holding live positions "
                                    + "is an org chart nothing can draw, and people may be "
                                    + "employed into them.");
                }
            }
            department.setActive(request.active());
        }

        //! step 8 - save. The code and the parent are untouched: neither is on the request, so
        //! neither can be reached from here even by a caller that sends them.
        // TODO: update department
        Department saved = departments.save(department);

        return DepartmentResponse.fromDepartment(saved,
                Boolean.FALSE.equals(saved.getActive())
                        ? "Retired. It keeps its place in the tree and its positions keep naming it — "
                                + "#12 with ?active=true is what hides it. " + NO_AUTHORIZATION_YET
                        : "Updated. " + NO_AUTHORIZATION_YET);
    }

    /**
     * Endpoint #13 — create an approved seat inside a department.
     *
     * <p><b>No cycle walk here</b>, for the same reason #9 has none: a brand-new seat has nothing
     * reporting to it, so it cannot be its own ancestor whatever it reports to. The walk belongs
     * to #14, which can move an existing seat under its own subordinate.
     */
    public PositionResponse createPosition(PositionCreateRequest request) {

        //! step 1 - who is asking
        School school = currentSchool.requireUsable();

        //! step 2 - the owning unit has to be this school's
        String departmentId = request.departmentDocsId().trim();
        Department department = utils.loadDepartment(school, departmentId);

        //! step 3 - and it has to still be one. A seat nobody may be hired into, inside a unit
        //! that no longer exists, is two problems rather than one.
        if (!Boolean.TRUE.equals(department.getActive())) {
            throw ApiException.conflict("DEPARTMENT_NOT_ACTIVE",
                    "'" + department.getName() + "' is retired, so no new position can be created in "
                            + "it. Reactivate the department first, or create the position in the unit "
                            + "that replaced it.");
        }

        //! step 4 - the title is what a seat is known by now that positionCode is gone, so it
        //! carries the uniqueness the code used to. Folded case, because "Mathematics Teacher"
        //! and "mathematics teacher" are one seat to a person reading a list.
        //!
        //! RETIRED SEATS COUNT, matching school_department_title_uniq, which does not filter on
        //! active. A check that skipped them would accept a write the index then refuses.
        String title = request.title().trim();
        // TODO: check position exists
        if (positions.existsBySchoolIdAndDepartmentDocsIdAndTitleIgnoreCase(
                school.getId(), departmentId, title)) {

            throw ApiException.conflict("POSITION_TITLE_TAKEN",
                    "'" + department.getName() + "' already has a position titled '" + title
                            + "'. A title is what names a position now that they have no code, "
                            + "so it stays taken once used — retired positions included.");
        }

        //! step 5 - the reporting line, when one was named. Checked to be this school's and
        //! NOTHING ELSE: deliberately not required to share the department, because a school with
        //! one Head of Safeguarding that every unit reports to on that line is a real structure.
        //! The org tree and the reporting line answer different questions.
        String reportsTo = TextHelper.blankToNull(request.reportsToPositionDocsId());
        if (reportsTo != null) {
            // TODO: read position
            positions.findByIdAndSchoolId(reportsTo, school.getId())
                    .orElseThrow(() -> ApiException.notFound("POSITION_NOT_FOUND",
                            "No position with id '" + reportsTo + "' in this school."));
        }

        //! step 6 - build it. approvedHeadcount follows the MODEL, which is @NotNull with a
        //! default of 1 - so absent means one seat, and "uncapped" is not a state a stored
        //! position can be in whatever the plan says. teachingPosition defaults false the same way.
        Position seat = Position.builder()
                .schoolId(school.getId())
                .title(title)
                .departmentDocsId(departmentId)
                .reportsToPositionDocsId(reportsTo)
                .approvedHeadcount(request.approvedHeadcount() == null
                        ? 1
                        : request.approvedHeadcount())
                .teachingPosition(Boolean.TRUE.equals(request.teachingPosition()))
                .build();

        //! step 7 - insert it
        // TODO: insert position
        Position saved = positions.save(seat);

        return PositionResponse.fromPosition(saved,
                utils.teachingWarning(school, departmentId, saved),
                "Employing somebody into this position needs positionDocsId " + saved.getId() + ". "
                        + NO_AUTHORIZATION_YET);
    }

    /**
     * Endpoint #14 — retitle a seat, move its headcount, change its reporting line, retire it.
     *
     * <p><b>Never its department.</b> A seat that moves department is a new seat: editing it in
     * place would rewrite where every past holder worked, and every employment record under it
     * would silently change department too. It is also what keeps the title index meaningful — a
     * title is unique <i>within</i> a unit.
     *
     * <p><b>This is the endpoint that can write a reporting cycle</b>, which is why the walk is
     * here and not on #13. A brand-new seat has nothing reporting to it; an existing one can be
     * moved under its own subordinate.
     */
    public PositionResponse updatePosition(String positionDocsId, PositionUpdateRequest request) {

        //! step 1 - who is asking
        School school = currentSchool.requireUsable();

        //! step 2 - refuse a request that asks for nothing, BEFORE reading anything.
        if (request.isEmpty()) {
            throw ApiException.badRequest("NOTHING_TO_UPDATE",
                    "Send title, reportsToPositionDocsId, approvedHeadcount, teachingPosition or "
                            + "active. departmentDocsId is not editable — a position that moves "
                            + "department is a new position, and moving it would rewrite where every "
                            + "past holder worked.");
        }

        //! step 3 - the seat, scoped to the school. Another school's real id is a real id.
        String seatId = positionDocsId == null ? "" : positionDocsId.trim();
        // TODO: read position
        Position position = positions.findByIdAndSchoolId(seatId, school.getId())
                .orElseThrow(() -> ApiException.notFound("POSITION_NOT_FOUND",
                        "No position with id '" + seatId + "' in this school."));

        //! step 4 - the title, which is the whole identity now that positionCode is gone.
        //!
        //! THE DUPLICATE CHECK SKIPS A TITLE THAT ONLY CHANGED CASE, because that is this same
        //! seat and existsBy... cannot exclude it. "mathematics teacher" -> "Mathematics Teacher"
        //! is a correction, not a collision, and the folded comparison is what tells them apart.
        //!
        //! RETIRED SEATS COUNT, matching school_department_title_uniq, which does not filter on
        //! active. A check that skipped them would accept a write the index then refuses.
        if (request.title() != null) {
            String newTitle = request.title().trim();
            if (newTitle.isEmpty()) {
                throw ApiException.badRequest("POSITION_TITLE_REQUIRED",
                        "A position's title cannot be removed — it is what names it now that "
                                + "positions have no code. Send a new one, or omit the field.");
            }

            if (!newTitle.equalsIgnoreCase(position.getTitle())) {
                // TODO: check position exists
                if (positions.existsBySchoolIdAndDepartmentDocsIdAndTitleIgnoreCase(
                        school.getId(), position.getDepartmentDocsId(), newTitle)) {

                    throw ApiException.conflict("POSITION_TITLE_TAKEN",
                            "Another position in this department is already titled '" + newTitle
                                    + "'. A title is what names a position now that they have no "
                                    + "code, so it stays taken once used — retired positions "
                                    + "included.");
                }
            }
            position.setTitle(newTitle);
        }

        //! step 5 - the reporting line. "" reports to nobody; a real id has to be this school's,
        //! and deliberately NOT the same department - a school with one Head of Safeguarding that
        //! every unit reports to on that line is a real structure.
        if (request.reportsToPositionDocsId() != null) {
            String reportsTo = TextHelper.blankToNull(request.reportsToPositionDocsId());

            if (reportsTo != null) {
                //! ONE STEP, AND THEN THE WHOLE CHAIN. Reporting to itself is the one-step case
                //! of the same walk, named separately only because the message can be clearer.
                if (reportsTo.equals(position.getId())) {
                    throw ApiException.conflict("POSITION_CYCLE",
                            "A position cannot report to itself.");
                }

                // TODO: read position
                Position supervisor = positions.findByIdAndSchoolId(reportsTo, school.getId())
                        .orElseThrow(() -> ApiException.notFound("POSITION_NOT_FOUND",
                                "No position with id '" + reportsTo + "' in this school."));

                //! THE CYCLE WALK, and #14 is the only endpoint that needs it. #13 cannot write a
                //! cycle because a brand-new seat has nothing reporting to it; this one can move
                //! an existing seat under its own subordinate.
                //!
                //! `seen` is not defensive habit either: a cycle already in the collection - hand
                //! written, restored from a backup, or left by a future writer - would make this
                //! walk itself loop forever. It stops and reports rather than hanging the request.
                //!
                //! ONE READ PER LEVEL, not one read of the collection. A reporting chain is a
                //! handful of seats deep where a department tree is read whole by #12 anyway.
                Set<String> seen = new LinkedHashSet<>();
                Position walker = supervisor;
                while (walker != null && seen.add(walker.getId())) {
                    if (position.getId().equals(walker.getReportsToPositionDocsId())) {
                        throw ApiException.conflict("POSITION_CYCLE",
                                "'" + supervisor.getTitle() + "' already reports to '"
                                        + position.getTitle() + "', directly or through the chain "
                                        + "above it. Making that position its supervisor would close "
                                        + "the line into a loop that nothing could draw.");
                    }

                    String next = walker.getReportsToPositionDocsId();
                    // TODO: read position
                    walker = next == null
                            ? null
                            : positions.findByIdAndSchoolId(next, school.getId()).orElse(null);
                }
            }

            position.setReportsToPositionDocsId(reportsTo);
        }

        //! step 6 - the headcount. @Min(1) refuses zero and below at validation: the model is
        //! @NotNull with a default of 1, so "uncapped" is not a state a stored seat can be in.
        //!
        //! LOWERING IT BELOW THE FILLED COUNT IS TO BE A WARNING, NOT A REFUSAL - a school
        //! reducing a count that is already over-filled is describing something that has already
        //! happened, and refusing it would make the number impossible to correct. It is NOT
        //! implemented, because nothing fills a seat yet: there is no EmploymentRecordRepository
        //! and no endpoint writes one, so the count could only ever be zero. #16 is what earns it.
        if (request.approvedHeadcount() != null) {
            position.setApprovedHeadcount(request.approvedHeadcount());
        }

        //! step 7 - whether the seat teaches. Turning it OFF is the interesting direction: it can
        //! leave a department with no teaching seat at all, which is what an empty teacher picker
        //! looks like - so the same warning #13 gives on the way in is computed here on the way out.
        if (request.teachingPosition() != null) {
            position.setTeachingPosition(request.teachingPosition());
        }

        //! step 8 - retiring. A RETIRED SEAT KEEPS ITS TITLE, matching the index and matching a
        //! retired term keeping its code.
        //!
        //! RETIRING A SEAT SOMEBODY HOLDS IS TO BE REFUSED - 409 POSITION_STILL_FILLED, with the
        //! holder separated or transferred first. NOT implemented, for the same reason as the
        //! headcount warning above: nothing employs anybody yet, so the check could only ever
        //! pass. #16 and #17 are what earn it, and this comment is the note that they owe it.
        if (request.active() != null) {
            position.setActive(request.active());
        }

        //! step 9 - save. The department is untouched: it is not on the request, so it cannot be
        //! reached from here even by a caller that sends it.
        // TODO: update position
        Position saved = positions.save(position);

        return PositionResponse.fromPosition(saved,
                utils.teachingWarning(school, saved.getDepartmentDocsId(), saved),
                Boolean.FALSE.equals(saved.getActive())
                        ? "Retired. It keeps its title, and records made against it still name it."
                        : "Updated. " + NO_AUTHORIZATION_YET);
    }


    /**
     * Endpoint #12, flat — one page of the school's departments.
     *
     * <p><b>Paged, though the plan said not to be.</b> Its reasoning was that a department list is
     * tens of rows. That is true of most schools and not a constraint anywhere: nothing caps the
     * count, and a group running forty units through one tenant would page. The cost is the
     * shared record and factory that already exist, and a client that handles every list in this
     * API the same way is worth more than the rows saved.
     *
     * <p><b>No gate runs on it.</b> A suspended or closed school still reads its own org chart.
     */
    public PageResponse<DepartmentResponse> listDepartments(DepartmentSearchRequest request) {

        //! step 1 - the paging and the order, validated before anything is read. Cheap checks
        //! with no I/O behind them go first, so a malformed request costs no round trip.
        Pageable pageable = PageResponse.pageableOf(request.page(), request.size(), request.sort(),
                SORTABLE_DEPARTMENT_FIELDS, SORTABLE_DEPARTMENT_FIELD_NAMES, DEPARTMENT_ORDER);

        //! step 2 - who is asking. `require`, not `requireUsable`: a suspended or closed school
        //! can still read its own structure.
        School school = currentSchool.require();

        //! step 3 - one page, filtered and ordered in the database
        // TODO: read departments
        return PageResponse.from(
                departments.search(school.getId(), request, pageable),
                // The single-argument factory, so no `nextStep` appears on a row: a read changed
                // nothing, and a null on every row is noise a client has to decide about.
                DepartmentResponse::fromDepartment);
    }

    /**
     * Endpoint #12, nested — the org chart as a tree.
     *
     * <p><b>One flat read, then assembled in memory.</b> A query per level would be a storm for a
     * structure that fits in memory comfortably, and the plan says so.
     *
     * <p><b>A filter can orphan a node, and orphans are lifted rather than dropped.</b> Ask for
     * {@code ?active=true} and a retired parent disappears while its active children remain — they
     * are real units the caller asked to see, so they surface at the top marked
     * {@code liftedToTop} instead of vanishing with their parent.
     */
    public DepartmentTreeResponse treeOfDepartments(DepartmentSearchRequest request) {

        //! step 1 - who is asking
        School school = currentSchool.require();

        //! step 2 - the whole matching set, in one read, ordered the way a tree should read
        // TODO: read departments
        List<Department> rows = departments.searchAll(school.getId(), request, DEPARTMENT_ORDER);

        //! step 3 - index what came back, so a parent can be found without another query.
        //! Insertion-ordered, because the sort above is what decides sibling order.
        Map<String, List<Department>> childrenOf = new LinkedHashMap<>();
        Set<String> present = new LinkedHashSet<>();
        for (Department one : rows) {
            present.add(one.getId());
        }

        List<Department> roots = new ArrayList<>();
        List<Department> lifted = new ArrayList<>();
        for (Department one : rows) {
            String parentId = one.getParentDepartmentDocsId();
            if (parentId == null) {
                //! A genuine top-level unit.
                roots.add(one);
            } else if (present.contains(parentId)) {
                childrenOf.computeIfAbsent(parentId, key -> new ArrayList<>()).add(one);
            } else {
                //! ORPHANED BY THE FILTER, not by the data. Its parent exists in the collection
                //! and was excluded from this answer - so the node is lifted rather than dropped,
                //! because a caller asking for active units must see every active unit.
                lifted.add(one);
            }
        }

        //! step 4 - build downwards from each root, carrying a visited set.
        //!
        //! THE VISITED SET IS NOT DEFENSIVE PROGRAMMING, it is the only thing standing between a
        //! cyclic parent chain and a stack overflow.
        //!
        //! NO ENDPOINT CAN WRITE A CYCLE as of 2026-09-15 - #9 cannot, because a new unit has no
        //! children, and #10 cannot, because it does not accept a parent at all. That is exactly
        //! why this stays: it is what makes "the chart has no cycles" true of the DATA, which a
        //! hand-edited collection, a restored backup or a later #10 that moves units can all make
        //! false without touching this file. Open item 2 says a cycle reaches this method as a
        //! stack overflow in whatever first draws the chart.
        List<DepartmentNodeResponse> built = new ArrayList<>();
        for (Department root : roots) {
            built.add(buildNode(root, childrenOf, new LinkedHashSet<>(), false));
        }
        for (Department orphan : lifted) {
            built.add(buildNode(orphan, childrenOf, new LinkedHashSet<>(), true));
        }

        return new DepartmentTreeResponse(built, rows.size(), lifted.size(), depthOf(built));
    }

    /**
     * One node and everything under it.
     *
     * <p>{@code seen} carries the ancestors of this node, so a chain that closes on itself stops
     * rather than recursing forever. A node already in its own ancestry is dropped from the tree —
     * it is unreachable in any sane reading of the chart, and returning it would mean returning it
     * infinitely.
     */
    private DepartmentNodeResponse buildNode(Department node,
            Map<String, List<Department>> childrenOf, Set<String> seen, boolean lifted) {

        if (!seen.add(node.getId())) {
            return DepartmentNodeResponse.of(node, lifted, List.of());
        }

        List<DepartmentNodeResponse> below = new ArrayList<>();
        for (Department under : childrenOf.getOrDefault(node.getId(), List.of())) {
            below.add(buildNode(under, childrenOf, new LinkedHashSet<>(seen), false));
        }

        return DepartmentNodeResponse.of(node, lifted, below);
    }

    /** How deep the answer goes. 0 for an empty tree, 1 for a flat school. */
    private int depthOf(List<DepartmentNodeResponse> nodes) {
        int deepest = 0;
        for (DepartmentNodeResponse node : nodes) {
            deepest = Math.max(deepest, 1 + depthOf(node.subDepartments()));
        }
        return deepest;
    }

    /**
     * Endpoint #52 — one department and everything it is made of.
     *
     * <p><b>Four reads, and each answers a question the caller would otherwise have to ask
     * itself.</b> The unit, the one above it, the ones under it and its seats — plus the head's
     * name, which is the one place in this package an id is resolved to a person.
     *
     * <p><b>No gate runs on it.</b> A suspended or closed school still reads its own org chart.
     */
    public DepartmentDetailResponse getDepartment(String departmentDocsId) {

        //! step 1 - who is asking. `require`, not `requireUsable`: a suspended or closed school
        //! can still read its own structure.
        School school = currentSchool.require();

        //! step 2 - the unit itself, scoped to the school
        Department department = utils.loadDepartment(school, departmentDocsId);

        //! step 3 - the parent, resolved. A DETAIL view is the one place that resolves an id: the
        //! whole question it answers is "tell me about this unit", and making a caller issue three
        //! more requests to render one page is the cost of refusing.
        //!
        //! Read directly rather than through loadDepartment, because a parent that has since been
        //! deleted must leave the page readable rather than 404 the unit the caller asked for.
        DepartmentSummaryResponse parentDepartment = null;
        if (department.getParentDepartmentDocsId() != null) {
            // TODO: read department
            parentDepartment = departments
                    .findByIdAndSchoolId(department.getParentDepartmentDocsId(), school.getId())
                    .map(DepartmentSummaryResponse::of)
                    .orElse(null);
        }

        //! step 4 - the head, resolved to a NAME and nothing more. A Staff document carries an
        //! address, a date of birth and a national identity number; a department page needs a
        //! name, and this module has no authorization yet.
        StaffSummaryResponse headStaff = null;
        if (department.getHeadStaffDocsId() != null) {
            // TODO: read staff
            headStaff = staff.findByIdAndSchoolId(department.getHeadStaffDocsId(), school.getId())
                    .map(StaffSummaryResponse::of)
                    .orElse(null);
        }

        //! step 5 - the units directly under this one. DIRECT ONLY: the whole nesting is #12 with
        //! ?tree=true, and repeating that walk here would be a second implementation of it.
        // TODO: read departments
        List<DepartmentSummaryResponse> subDepartments = departments
                .findBySchoolIdAndParentDepartmentDocsIdOrderByNameAsc(
                        school.getId(), department.getId())
                .stream()
                .map(DepartmentSummaryResponse::of)
                .toList();

        //! step 6 - the seats, retired ones included and marked. A retired seat is still part of
        //! what a unit is made of: records made against it still name it.
        // TODO: read positions
        List<Position> seats = positions.findBySchoolIdAndDepartmentDocsIdOrderByTitleAsc(
                school.getId(), department.getId());

        return new DepartmentDetailResponse(
                department.getId(),
                department.getDepartmentCode(),
                department.getName(),
                department.getDescription(),
                department.getActive(),
                parentDepartment,
                headStaff,
                subDepartments,
                seats.stream().map(PositionResponse::fromPosition).toList(),
                subDepartments.size(),
                seats.size(),
                (int) seats.stream().filter(one -> Boolean.TRUE.equals(one.getActive())).count(),
                (int) seats.stream()
                        .filter(one -> Boolean.TRUE.equals(one.getTeachingPosition())).count());
    }
    /**
     * Endpoint #15 — one page of the school's seats, each with the count of who holds it.
     *
     * <h2>The count is the endpoint</h2>
     *
     * <p>Everything else here is already on #52. What #15 adds is {@code filledHeadcount},
     * computed from current employment records and <b>never stored</b>: a counter on
     * {@code Position} drifts the first time a writer forgets it, which is the same objection that
     * keeps a weight total off {@code AcademicTerm}.
     *
     * <p><b>One grouped count for the whole page, not one per row.</b> Twenty seats answered by
     * twenty {@code countBy...} calls is the N+1 that makes a list slower the more it returns.
     *
     * <h2>{@code ?vacant=} costs more than the other filters, and here is why</h2>
     *
     * <p>Vacancy is not a field. It is {@code filledHeadcount < approvedHeadcount}, and the left
     * side lives in another collection — so it cannot go into the query that pages, and applying
     * it <i>after</i> paging returns short pages: ask for twenty and get the eleven of those
     * twenty that were vacant, with no way to tell that from "there are only eleven".
     *
     * <p>So {@code ?vacant=} takes a different path: every matching seat is read, counted in one
     * aggregation, filtered, and only then paged in memory. A school's seat count is tens,
     * occasionally hundreds. <b>The plan's "counted for the page, not the collection" holds for
     * every call except this one</b>, and that is a trade this endpoint makes deliberately rather
     * than a line it quietly crosses — {@code ?vacant=true} is the query a school runs at the
     * start of a hiring round, and a hiring round is not a hot path.
     *
     * <p><b>{@code ?vacant=false} is not "full", it is "not vacant"</b> — which includes a seat
     * holding more people than were approved. #16 warns rather than refusing when a school
     * over-hires, so that state exists and has to fall on one side of the filter.
     *
     * <p><b>No gate runs on it.</b> A suspended or closed school still reads its own org chart.
     */
    public PageResponse<PositionRowResponse> listPositions(PositionSearchRequest request) {

        //! step 1 - the paging and the order, validated before anything is read. Cheap checks
        //! with no I/O behind them go first, so a malformed request costs no round trip.
        Pageable pageable = PageResponse.pageableOf(request.page(), request.size(), request.sort(),
                SORTABLE_POSITION_FIELDS, SORTABLE_POSITION_FIELD_NAMES, POSITION_ORDER);

        //! step 2 - who is asking. `require`, not `requireUsable`: a suspended or closed school
        //! can still read its own structure.
        School school = currentSchool.require();

        //! step 3 - the vacancy path, when it was asked for. See the class note: the filter needs
        //! the counts, the counts need the ids, and the ids are what paging would have thrown
        //! away - so the set is read whole, counted, filtered, and paged last.
        if (request.vacant() != null) {
            // TODO: read positions
            List<Position> matching =
                    positions.searchAll(school.getId(), request, pageable.getSort());

            //! One grouped count over the whole matching set - the cost this path pays, and
            //! the reason the class note calls it out rather than leaving it to be discovered.
            // TODO: read employment records
            Map<String, Long> filledFor = employments.filledHeadcounts(school.getId(),
                    matching.stream().map(Position::getId).toList());

            //! A null approvedHeadcount reads as zero, so such a seat is never vacant. The model
            //! declares the field @NotNull with a default of 1 so this should be unreachable -
            //! but a document written before that default has no key at all, and the alternative
            //! is a NullPointerException in the middle of a list endpoint.
            List<Position> vacantOnes = matching.stream()
                    .filter(one -> {
                        int approved = one.getApprovedHeadcount() == null
                                ? 0
                                : one.getApprovedHeadcount();
                        boolean vacant = filledFor.getOrDefault(one.getId(), 0L) < approved;
                        return vacant == request.vacant();
                    })
                    .toList();

            //! The page, cut from the filtered list. `from` is clamped because a caller may ask
            //! for page 9 of a 2-page result, and a sublist past the end throws rather than
            //! answering empty.
            int from = Math.min((int) pageable.getOffset(), vacantOnes.size());
            int to = Math.min(from + pageable.getPageSize(), vacantOnes.size());

            return PageResponse.from(
                    new PageImpl<>(vacantOnes.subList(from, to), pageable, vacantOnes.size()),
                    one -> PositionRowResponse.of(one, filledFor.getOrDefault(one.getId(), 0L)));
        }

        //! step 4 - the ordinary path: one page, filtered and ordered in the database
        // TODO: read positions
        Page<Position> page = positions.search(school.getId(), request, pageable);

        //! step 5 - and ONE grouped count over exactly the rows on it. Twenty seats answered
        //! by twenty countBy... calls is the N+1 that makes a list slower the more it returns.
        // TODO: read employment records
        Map<String, Long> filledFor = employments.filledHeadcounts(school.getId(),
                page.getContent().stream().map(Position::getId).toList());

        return PageResponse.from(page,
                one -> PositionRowResponse.of(one, filledFor.getOrDefault(one.getId(), 0L)));
    }

    /**
     * Endpoint #53 — one seat and who is in it.
     *
     * <h2>Added after the plan, the way #52 was</h2>
     *
     * <p>#15 answers "3 of 5 filled" and a school immediately asks <i>which three</i>. Answering
     * that from the endpoints the plan numbered means #7, which has no employment filter — so the
     * same call #52 made for a department is made here for a seat.
     *
     * <h2>The count and the list come from one read</h2>
     *
     * <p>{@code filledHeadcount} is the size of {@code holders}, not a second count query. Two
     * reads of the same collection a moment apart can disagree, and a page showing "3 filled"
     * above two names is a bug report nobody can reproduce. #15 counts without listing because a
     * page of twenty seats should not fetch every holder of every one; this lists, so it counts by
     * listing.
     *
     * <h2>A holder whose person is missing is still a holder</h2>
     *
     * <p>An employment record naming a {@code staffDocsId} that does not resolve is returned with
     * its name fields absent and a note, rather than dropped. Dropping it would make the seat look
     * less filled than it is, and the count and the list would disagree — the one thing a page
     * like this must not do.
     *
     * <p><b>No gate runs on it.</b> A suspended or closed school still reads its own org chart.
     */
    public PositionDetailResponse getPosition(String positionDocsId) {

        //! step 1 - who is asking. `require`, not `requireUsable`.
        School school = currentSchool.require();

        //! step 2 - the seat, scoped to the school. Another school's real id is a real id.
        String seatId = positionDocsId == null ? "" : positionDocsId.trim();
        // TODO: read position
        Position position = positions.findByIdAndSchoolId(seatId, school.getId())
                .orElseThrow(() -> ApiException.notFound("POSITION_NOT_FOUND",
                        "No position with id '" + seatId + "' in this school."));

        //! step 3 - the unit's name, so the page has a heading without a second request. Read
        //! directly rather than through loadDepartment: a department that has since been deleted
        //! must leave the seat readable rather than 404 the thing the caller asked for.
        // TODO: read department
        String departmentName = departments
                .findByIdAndSchoolId(position.getDepartmentDocsId(), school.getId())
                .map(Department::getName)
                .orElse(null);

        //! step 4 - the seat it reports to, resolved to a TITLE. The same reason step 3 exists:
        //! a detail view is the one place that resolves an id, because the whole question it
        //! answers is "tell me about this one".
        String reportsToTitle = null;
        if (position.getReportsToPositionDocsId() != null) {
            // TODO: read position
            reportsToTitle = positions
                    .findByIdAndSchoolId(position.getReportsToPositionDocsId(), school.getId())
                    .map(Position::getTitle)
                    .orElse(null);
        }

        //! step 5 - who is in it NOW, longest-serving first. Not the seat's history: a closed
        //! record names somebody who USED TO hold this, and one person's history is #19.
        // TODO: read employment records
        List<EmploymentRecord> held = employments
                .findBySchoolIdAndPositionDocsIdAndCurrentIsTrueOrderByEffectiveFromAsc(
                        school.getId(), position.getId());

        //! step 6 - their names, in ONE read. A loop here is an N+1: a seat with eleven people in
        //! it would be eleven round trips to render one table.
        List<String> peopleIds = held.stream()
                .map(EmploymentRecord::getStaffDocsId)
                .filter(one -> one != null)
                .distinct()
                .toList();

        Map<String, Staff> peopleById = new LinkedHashMap<>();
        if (!peopleIds.isEmpty()) {
            // TODO: read staff
            for (Staff one : staff.findBySchoolIdAndIdIn(school.getId(), peopleIds)) {
                peopleById.put(one.getId(), one);
            }
        }

        //! step 7 - the rows. A record whose person did not resolve is MARKED, not dropped - see
        //! the class note on why the count and the list must never disagree.
        List<PositionHolderResponse> holders = held.stream()
                .map(one -> PositionHolderResponse.of(one, peopleById.get(one.getStaffDocsId())))
                .toList();

        long filled = holders.size();
        int approved = position.getApprovedHeadcount() == null
                ? 0
                : position.getApprovedHeadcount();

        return new PositionDetailResponse(
                position.getId(),
                position.getTitle(),
                position.getDepartmentDocsId(),
                departmentName,
                position.getReportsToPositionDocsId(),
                reportsToTitle,
                position.getApprovedHeadcount(),
                filled,
                Math.max(0L, approved - filled),
                filled > approved,
                position.getTeachingPosition(),
                position.getActive(),
                holders,
                holders.isEmpty() ? NOBODY_HOLDS_IT : null,
                NO_AUTHORIZATION_YET);
    }
}
