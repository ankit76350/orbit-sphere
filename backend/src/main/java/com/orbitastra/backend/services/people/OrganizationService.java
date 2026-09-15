package com.orbitastra.backend.services.people;

import org.springframework.stereotype.Service;

import com.orbitastra.backend.common.current.CurrentSchoolResolver;
import com.orbitastra.backend.common.error.exception.ApiException;
import com.orbitastra.backend.common.text.TextHelper;
import com.orbitastra.backend.dto.people.organization.request.DepartmentCreateRequest;
import com.orbitastra.backend.dto.people.organization.response.DepartmentResponse;
import com.orbitastra.backend.models.core.School;
import com.orbitastra.backend.models.people.organization.Department;
import com.orbitastra.backend.repositories.people.organization.DepartmentRepository;
import com.orbitastra.backend.repositories.people.staff.StaffRepository;

import lombok.RequiredArgsConstructor;

/**
 * The org chart a school hires into — endpoints #9 to #15 of the plan in
 * {@code controllers/people/organization/README.md}. #9 is built.
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
    private final StaffRepository staff;

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
}
