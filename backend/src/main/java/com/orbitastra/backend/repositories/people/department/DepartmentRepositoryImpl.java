package com.orbitastra.backend.repositories.people.department;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import com.orbitastra.backend.dto.people.department.request.DepartmentSearchRequest;
import com.orbitastra.backend.models.people.department.Department;

import lombok.RequiredArgsConstructor;

/**
 * The custom fragment behind {@link DepartmentRepositoryCustom}.
 *
 * <p><b>The name and the package are load-bearing.</b> Spring Data resolves a fragment by
 * {@code <Interface>Impl} in the same package as the repository. Rename it or move it and the
 * application compiles, starts, and fails only when somebody calls it.
 */
@RequiredArgsConstructor
public class DepartmentRepositoryImpl implements DepartmentRepositoryCustom {

    private final MongoTemplate mongo;

    @Override
    public Page<Department> search(String schoolId, DepartmentSearchRequest request,
            Pageable pageable) {

        //! step 1 - the filter: the tenant, then whichever were sent
        Criteria criteria = buildCriteria(schoolId, request);

        //! step 2 - the count, carrying the filter and nothing else
        // TODO: reading departments (how many match)
        long total = mongo.count(new Query(criteria), Department.class);

        //! step 3 - the page, the same filter plus the paging and sorting
        // TODO: reading departments (one page of them)
        List<Department> rows = mongo.find(new Query(criteria).with(pageable), Department.class);

        return new PageImpl<>(rows, pageable, total);
    }

    @Override
    public List<Department> searchAll(String schoolId, DepartmentSearchRequest request,
            Sort sort) {

        //! The same filter, no paging. A tree has no page boundary, so this is the read behind
        //! ?tree=true - one query for the whole structure.
        // TODO: reading departments (all of them, for the tree)
        return mongo.find(new Query(buildCriteria(schoolId, request)).with(sort), Department.class);
    }

    private Criteria buildCriteria(String schoolId, DepartmentSearchRequest request) {

        //! step 1 - the tenant, always, and never from the caller
        List<Criteria> filters = new ArrayList<>();
        filters.add(Criteria.where("schoolId").is(schoolId));

        //! step 2 - the headline filter, and the one school_department_active_name_idx covers
        if (request.active() != null) {
            filters.add(Criteria.where("active").is(request.active()));
        }

        //! step 3 - the search, across BOTH things a unit is known by. A person looking for one
        //! types either its name or its code, and which they remember is not this endpoint's to
        //! decide.
        //!
        //! Quoted before it is compiled, so a caller typing "Science (Lower)" searches for those
        //! characters rather than injecting a regex group - and a stray "(" is an empty result
        //! instead of a 500 from PatternSyntaxException.
        if (request.search() != null && !request.search().isBlank()) {
            String needle = Pattern.quote(request.search().trim());
            filters.add(new Criteria().orOperator(
                    Criteria.where("name").regex(needle, "i"),
                    Criteria.where("departmentCode").regex(needle, "i")));
        }

        //! step 4 - the children of one unit
        if (request.parentDepartmentDocsId() != null
                && !request.parentDepartmentDocsId().isBlank()) {

            filters.add(Criteria.where("parentDepartmentDocsId")
                    .is(request.parentDepartmentDocsId().trim()));
        }

        //! step 5 - what one person runs
        if (request.headStaffDocsId() != null && !request.headStaffDocsId().isBlank()) {
            filters.add(Criteria.where("headStaffDocsId").is(request.headStaffDocsId().trim()));
        }

        //! step 6 - the roots, or everything below them. Asked with `exists` rather than a null
        //! comparison, because a document written before the field existed has no key at all and
        //! must read as top-level - the same reason #28 of academics asks `sections.0`.
        if (request.topLevelOnly() != null) {
            filters.add(Criteria.where("parentDepartmentDocsId").exists(!request.topLevelOnly()));
        }

        //! step 7 - AND them together
        return new Criteria().andOperator(filters.toArray(new Criteria[0]));
    }
}
