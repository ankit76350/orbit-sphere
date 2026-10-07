package com.orbitastra.backend.repositories.student.student;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.domain.Sort;

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

    @Override
    public List<Student> findKnownChild(String schoolId, String digits, boolean wholeNumber,
            String admissionNo, String name, Collection<String> guardianDocsIds, int limit) {

        //! step 1 - every way of asking the same question, OR-ed. A school looks a child up by
        //! whatever it happens to have in front of it, and which one that is is not ours to pick.
        List<Criteria> either = new ArrayList<>();

        //! step 2 - the phone, matched on its DIGITS rather than on what somebody typed.
        //!
        //! THE SAME RULE crm #15 uses, and on purpose: "+91 98765 43210" and "9876543210" are one
        //! number, so the pattern allows anything between the digits. A full-length number is
        //! compared on its last ten digits so a country code or a trunk 0 stops mattering; a
        //! SHORTER one has to match the whole number, because "543210" matching every number
        //! ending in those six digits is a false "we already have this child" — the worst answer
        //! this read can give.
        if (digits != null && !digits.isEmpty()) {
            String pattern = loosePhonePattern(digits, wholeNumber);
            either.add(Criteria.where("phoneNumber").regex(pattern));
        }

        //! step 3 - THE GUARDIANS' CHILDREN, which is how a child is actually found. A seven year
        //! old has no phone; the number the school holds is their mother's. The ids were looked up
        //! in `guardians` before this ran, so this part is an index seek on
        //! school_guardian_students_idx rather than a second regex.
        if (guardianDocsIds != null && !guardianDocsIds.isEmpty()) {
            either.add(Criteria.where("guardians.guardianDocsId").in(guardianDocsIds));
        }

        //! step 4 - the admission number, whole and case-insensitive. ANCHORED AT BOTH ENDS,
        //! unlike #4's name search: this is a question about identity, and "ADM/2026/0001" must
        //! not match "ADM/2026/00010". Quoted, so a caller cannot send a regular expression.
        if (admissionNo != null && !admissionNo.isEmpty()) {
            either.add(Criteria.where("admissionNo")
                    .regex("^" + Pattern.quote(admissionNo) + "$", "i"));
        }

        //! step 5 - the name, which matches ANYWHERE unlike the two above. A name is not an
        //! identifier: somebody typing "aarav" wants every Aarav on the roll to look at, and
        //! anchoring it would answer "no" to a question that was really "show me the candidates".
        if (name != null && !name.isEmpty()) {
            either.add(Criteria.where("fullName").regex(Pattern.quote(name), "i"));
        }

        //! step 6 - nothing to ask means nothing to answer. The service refuses an empty search
        //! before it gets here; this is what stops a bug there returning the whole school.
        if (either.isEmpty()) {
            return List.of();
        }

        Criteria criteria = Criteria.where("schoolId").is(schoolId)
                .orOperator(either.toArray(new Criteria[0]));

        //! step 7 - newest first, so the child somebody just admitted twice is at the top, and
        //! capped. See the service for why a cap rather than a page.
        Query query = new Query(criteria)
                .with(Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id")))
                .limit(limit);

        // TODO: read students (does this school already have this child)
        return mongo.find(query, Student.class);
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

    /**
     * The regular expression that makes two spellings of one phone number the same question.
     *
     * <p>Private, so it is an implementation detail rather than something another class can lean
     * on. Every digit is quoted as it is joined — the strip leaves nothing but digits, so there is
     * nothing left to quote, and the day somebody widens the strip this is already right.
     */
    private static String loosePhonePattern(String digits, boolean wholeNumber) {
        StringBuilder pattern = new StringBuilder(wholeNumber ? "^[^0-9]*" : "");
        boolean first = true;
        for (char digit : digits.toCharArray()) {
            if (!first) {
                pattern.append("[^0-9]*");
            }
            pattern.append(Pattern.quote(String.valueOf(digit)));
            first = false;
        }
        return pattern.append("[^0-9]*$").toString();
    }
}
