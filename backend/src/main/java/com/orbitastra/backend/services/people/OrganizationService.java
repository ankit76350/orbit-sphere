package com.orbitastra.backend.services.people;

import org.springframework.stereotype.Service;

import com.orbitastra.backend.common.current.CurrentSchoolResolver;
import com.orbitastra.backend.common.error.exception.ApiException;
import com.orbitastra.backend.common.text.TextHelper;
import com.orbitastra.backend.dto.people.organization.request.DepartmentCreateRequest;
import com.orbitastra.backend.dto.people.organization.request.PositionCreateRequest;
import com.orbitastra.backend.dto.people.organization.response.DepartmentResponse;
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
 * {@code controllers/people/organization/README.md}. #9 and #13 are built.
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
            // TODO: read department
            departments.findByIdAndSchoolId(parentId, school.getId())
                    .orElseThrow(() -> ApiException.notFound("DEPARTMENT_NOT_FOUND",
                            "No department with id '" + parentId + "' in this school."));
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
        // TODO: read department
        Department department = departments.findByIdAndSchoolId(departmentId, school.getId())
                .orElseThrow(() -> ApiException.notFound("DEPARTMENT_NOT_FOUND",
                        "No department with id '" + departmentId + "' in this school."));

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

}
