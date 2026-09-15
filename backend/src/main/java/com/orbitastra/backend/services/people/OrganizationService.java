package com.orbitastra.backend.services.people;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.orbitastra.backend.common.current.CurrentSchoolResolver;
import com.orbitastra.backend.common.error.exception.ApiException;
import com.orbitastra.backend.common.text.TextHelper;
import com.orbitastra.backend.common.web.PageResponse;
import com.orbitastra.backend.dto.people.organization.request.DepartmentCreateRequest;
import com.orbitastra.backend.dto.people.organization.request.DepartmentSearchRequest;
import com.orbitastra.backend.dto.people.organization.request.PositionCreateRequest;
import com.orbitastra.backend.dto.people.organization.response.DepartmentDetailResponse;
import com.orbitastra.backend.dto.people.organization.response.DepartmentNodeResponse;
import com.orbitastra.backend.dto.people.organization.response.DepartmentResponse;
import com.orbitastra.backend.dto.people.organization.response.DepartmentSummaryResponse;
import com.orbitastra.backend.dto.people.organization.response.DepartmentTreeResponse;
import com.orbitastra.backend.dto.people.organization.response.StaffSummaryResponse;
import com.orbitastra.backend.dto.people.organization.response.PositionResponse;
import com.orbitastra.backend.models.core.School;
import com.orbitastra.backend.models.people.organization.Department;
import com.orbitastra.backend.models.people.organization.Position;
import com.orbitastra.backend.repositories.people.organization.DepartmentRepository;
import com.orbitastra.backend.repositories.people.organization.PositionRepository;
import com.orbitastra.backend.repositories.people.staff.StaffRepository;
import com.orbitastra.backend.services.people.utils.OrganizationServiceUtils;

import lombok.RequiredArgsConstructor;

/**
 * The org chart a school hires into — endpoints #9 to #15 of the plan in
 * {@code controllers/people/organization/README.md}. #9, #12, #13 and #52 are built.
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

    /** Repeated on every response until permissions exist. Deliberately hard to miss. */
    private static final String NO_AUTHORIZATION_YET =
            "No authorization is enforced on this endpoint yet: any caller who can reach it can "
                    + "run it.";

    /**
     * Endpoint #9 — create an org unit, optionally under another.
     *
     * <p><b>No cycle walk here.</b> A brand-new department has no children, so it cannot be its
     * own ancestor whatever parent it names. The walk belongs to #10, which can move an existing
     * unit under its own descendant — and lives in {@code PeopleHelper} when that is built.
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

        //! step 6 - insert. `active` takes its default: retiring is #11, an event with its own
        //! endpoint rather than a field to set at create.
        // TODO: create department
        Department saved = departments.save(Department.builder()
                .schoolId(school.getId())
                .departmentCode(departmentCode)
                .name(request.name().trim())
                .description(TextHelper.blankToNull(request.description()))
                .parentDepartmentDocsId(parentId)
                .headStaffDocsId(headId)
                .build());

        return DepartmentResponse.fromDepartment(saved,
                "Positions hang off this unit — #13 creates one. " + NO_AUTHORIZATION_YET);
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
                    "'" + department.getName() + "' is retired, so no new seat can be created in "
                            + "it. Reactivate the department first, or create the seat in the unit "
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
                    "'" + department.getName() + "' already has a seat titled '" + title
                            + "'. A title is what names a seat now that positions have no code, "
                            + "so it stays taken once used — retired seats included.");
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

        //! step 6 - insert. approvedHeadcount follows the MODEL, which is @NotNull with a default
        //! of 1 - so absent means one seat, and "uncapped" is not a state a stored position can
        //! be in whatever the plan says. teachingPosition defaults false the same way.
        // TODO: create position
        Position saved = positions.save(Position.builder()
                .schoolId(school.getId())
                .title(title)
                .departmentDocsId(departmentId)
                .reportsToPositionDocsId(reportsTo)
                .approvedHeadcount(request.approvedHeadcount() == null
                        ? 1
                        : request.approvedHeadcount())
                .teachingPosition(Boolean.TRUE.equals(request.teachingPosition()))
                .build());

        return PositionResponse.fromPosition(saved,
                utils.teachingWarning(school, departmentId, saved),
                "Employing somebody into this seat needs positionDocsId " + saved.getId() + ". "
                        + NO_AUTHORIZATION_YET);
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
        //! cyclic parent chain and a stack overflow. Nothing can write a cycle today - #9 cannot,
        //! because a new unit has no children - but #10 will be able to, and open item 2 says a
        //! cycle written then is a crash in whatever first draws the chart. This is that thing.
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
}
