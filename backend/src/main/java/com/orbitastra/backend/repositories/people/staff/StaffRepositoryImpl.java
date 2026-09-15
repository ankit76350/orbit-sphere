package com.orbitastra.backend.repositories.people.staff;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import com.orbitastra.backend.dto.people.staff.request.StaffSearchRequest;
import com.orbitastra.backend.models.people.staff.Staff;

import lombok.RequiredArgsConstructor;

/**
 * The custom fragment behind {@link StaffRepositoryCustom}.
 *
 * <p><b>The name and the package are load-bearing.</b> Spring Data resolves a fragment by
 * {@code <Interface>Impl} in the same package as the repository. Rename it or move it and the
 * application compiles, starts, and fails only when somebody calls it.
 */
@RequiredArgsConstructor
public class StaffRepositoryImpl implements StaffRepositoryCustom {

    private final MongoTemplate mongo;

    @Override
    public Page<Staff> search(String schoolId, StaffSearchRequest request, Pageable pageable) {

        //! step 1 - the filter: the tenant, then whichever were sent
        Criteria criteria = buildCriteria(schoolId, request);

        //! step 2 - the count, carrying the filter and nothing else
        // TODO: reading staff (how many match)
        long total = mongo.count(new Query(criteria), Staff.class);

        //! step 3 - the page, the same filter plus the paging and sorting
        // TODO: reading staff (one page of them)
        List<Staff> rows = mongo.find(new Query(criteria).with(pageable), Staff.class);

        return new PageImpl<>(rows, pageable, total);
    }

    private Criteria buildCriteria(String schoolId, StaffSearchRequest request) {

        //! step 1 - the tenant, always, and never from the caller
        List<Criteria> filters = new ArrayList<>();
        filters.add(Criteria.where("schoolId").is(schoolId));

        //! step 2 - the search, across BOTH things a person is known by. An office looks somebody
        //! up by name and a payroll run looks them up by number, and which one the caller has is
        //! not this endpoint's to decide.
        //!
        //! QUOTED BEFORE IT IS COMPILED, so a caller typing "O'Brien (acting)" searches for those
        //! characters rather than injecting a regex group - and a stray "(" is an empty result
        //! instead of a 500 from PatternSyntaxException.
        if (request.search() != null && !request.search().isBlank()) {
            String needle = Pattern.quote(request.search().trim());
            filters.add(new Criteria().orOperator(
                    Criteria.where("fullName").regex(needle, "i"),
                    Criteria.where("employeeNo").regex(needle, "i")));
        }

        //! step 3 - the enum. Bound by Spring on the way in, so an unknown value is a 400 before
        //! this is reached rather than an empty page here.
        if (request.gender() != null) {
            filters.add(Criteria.where("gender").is(request.gender()));
        }

        //! step 4 - nationality, upper-cased to match how #1 stores it. A caller typing "in"
        //! should not get an empty page because the school typed "IN".
        if (request.nationalityCode() != null && !request.nationalityCode().isBlank()) {
            filters.add(Criteria.where("nationalityCode")
                    .is(request.nationalityCode().trim().toUpperCase()));
        }

        //! step 5 - who is missing contact details, which is a real question a school asks.
        //!
        //! ASKED WITH `exists`, NOT a null comparison: #1 stores an absent email as no key at all
        //! rather than as null, so `is(null)` would match nothing. This is the same reason #12
        //! asks `exists` for topLevelOnly and #28 of academics asks `sections.0`.
        if (request.hasEmail() != null) {
            filters.add(Criteria.where("emailAddress").exists(request.hasEmail()));
        }

        if (request.hasPhone() != null) {
            filters.add(Criteria.where("phoneNumber").exists(request.hasPhone()));
        }

        //! step 6 - AND them together
        return new Criteria().andOperator(filters.toArray(new Criteria[0]));
    }
}
