package com.orbitastra.backend.repositories.student.student;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import com.orbitastra.backend.dto.student.student.request.StudentSearchRequest;
import com.orbitastra.backend.models.student.Student;

import lombok.RequiredArgsConstructor;

/**
 * The custom part of {@link StudentRepositoryCustom}.
 *
 * <p><b>The name and the package matter.</b> Spring Data finds this by looking for
 * {@code <Interface>Impl} in the same package as the repository. Rename it or move it and
 * everything still compiles and starts, and the call fails the first time somebody makes it.
 */
@RequiredArgsConstructor
public class StudentRepositoryImpl implements StudentRepositoryCustom {

    private final MongoTemplate mongo;

    @Override
    public Page<Student> search(String schoolId, StudentSearchRequest request, Pageable pageable) {

        //! step 1 - the filter: the school first, then whichever filters were sent
        Criteria criteria = buildCriteria(schoolId, request);

        //! step 2 - how many match, with the filter and nothing else
        // TODO: read students (how many match)
        long total = mongo.count(new Query(criteria), Student.class);

        //! step 3 - one page of them, the same filter plus the paging and the order
        // TODO: read students (one page of them)
        List<Student> rows = mongo.find(new Query(criteria).with(pageable), Student.class);

        return new PageImpl<>(rows, pageable, total);
    }

    private Criteria buildCriteria(String schoolId, StudentSearchRequest request) {

        //! step 1 - the school, always, and never taken from the caller
        List<Criteria> filters = new ArrayList<>();
        filters.add(Criteria.where("schoolId").is(schoolId));

        //! step 2 - the name or the admission number, matched anywhere and case-insensitively.
        //! Both, because an office looks a child up by name and a fee receipt looks them up by
        //! number, and which one the caller has is not this endpoint's to decide.
        //!
        //! QUOTED BEFORE IT IS COMPILED, so somebody typing "O'Brien (jr)" searches for those
        //! characters instead of injecting a regex group — and a stray "(" is an empty result
        //! rather than a 500 out of PatternSyntaxException.
        if (request.search() != null && !request.search().isBlank()) {
            String needle = Pattern.quote(request.search().trim());
            filters.add(new Criteria().orOperator(
                    Criteria.where("fullName").regex(needle, "i"),
                    Criteria.where("admissionNo").regex(needle, "i")));
        }

        //! step 3 - the status. Bound by Spring on the way in, so an unknown value is a 400 before
        //! this runs rather than an empty page here.
        if (request.status() != null) {
            filters.add(Criteria.where("status").is(request.status()));
        }

        //! step 4 - the gender, the same way.
        if (request.gender() != null) {
            filters.add(Criteria.where("gender").is(request.gender()));
        }

        //! step 5 - who has been placed in a class and who has not, which is the real question at
        //! the start of a term: a child admitted in January has no academic record until somebody
        //! puts them in a section.
        //!
        //! ASKED WITH `exists`, NOT a null comparison: an absent id is stored as no key at all
        //! rather than as null, so `is(null)` would match nothing.
        if (request.placed() != null) {
            filters.add(Criteria.where("currentAcademicRecordDocsId").exists(request.placed()));
        }

        //! step 6 - who came through admissions and who was typed in by hand. The same `exists`
        //! rule, and the only way to ask "which of these children did the CRM give us".
        if (request.fromAdmissions() != null) {
            filters.add(Criteria.where("admissionApplicationDocsId")
                    .exists(request.fromAdmissions()));
        }

        //! step 7 - AND them together
        return new Criteria().andOperator(filters.toArray(new Criteria[0]));
    }

}
