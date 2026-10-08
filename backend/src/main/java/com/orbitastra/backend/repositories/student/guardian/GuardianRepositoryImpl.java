package com.orbitastra.backend.repositories.student.guardian;

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

import com.orbitastra.backend.dto.student.guardian.request.GuardianSearchRequest;
import com.orbitastra.backend.common.text.PhoneMatch;
import com.orbitastra.backend.models.student.Guardian;

import lombok.RequiredArgsConstructor;

/**
 * The custom part of {@link GuardianRepositoryCustom}.
 *
 * <p><b>The name and the package matter.</b> Spring Data finds this by looking for
 * {@code <Interface>Impl} beside the repository; move it and the call fails the first time
 * somebody makes it, not at startup.
 */
@RequiredArgsConstructor
public class GuardianRepositoryImpl implements GuardianRepositoryCustom {

    private final MongoTemplate mongo;

    @Override
    public List<Guardian> findByLoosePhone(String schoolId, String digits, boolean wholeNumber,
            boolean includeAlternate, int limit) {

        if (digits == null || digits.isEmpty()) {
            return List.of();
        }

        String pattern = PhoneMatch.loosePattern(digits, wholeNumber);

        //! THE ALTERNATE NUMBER COUNTS ONLY WHEN THE CALLER SAYS SO. For #6 and #9 it must —
        //! it is the one a family gives as "my husband's phone", and leaving it out would miss
        //! exactly the second parent those reads exist to find. For #7 it must NOT: that number
        //! is deliberately shared, so refusing on it would make a mother impossible to add once
        //! the father listed the family landline as his second number.
        Criteria criteria = Criteria.where("schoolId").is(schoolId)
                .orOperator(includeAlternate
                        ? new Criteria[] {
                            Criteria.where("phoneNumber").regex(pattern),
                            Criteria.where("alternatePhoneNumber").regex(pattern) }
                        : new Criteria[] { Criteria.where("phoneNumber").regex(pattern) });

        Query query = new Query(criteria)
                .with(Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id")))
                .limit(limit);

        // TODO: read guardians (whose number is this)
        return mongo.find(query, Guardian.class);
    }

    @Override
    public Page<Guardian> search(String schoolId, GuardianSearchRequest request, String digits,
            boolean wholeNumber, Pageable pageable) {

        //! step 1 - the school, always, and never taken from the caller
        List<Criteria> filters = new ArrayList<>();
        filters.add(Criteria.where("schoolId").is(schoolId));

        //! step 2 - the phone, on its DIGITS and across BOTH numbers. The same rule #7 refuses on,
        //! and it has to be: a caller who checks here, sees nothing, and is then refused by #7
        //! would have been told two different things about one number.
        if (digits != null && !digits.isEmpty()) {
            String pattern = PhoneMatch.loosePattern(digits, wholeNumber);
            filters.add(new Criteria().orOperator(
                    Criteria.where("phoneNumber").regex(pattern),
                    Criteria.where("alternatePhoneNumber").regex(pattern)));
        }

        //! step 3 - the email, WHOLE and case-insensitive. A question about identity: "a@b.com"
        //! must not match "maria@b.com" merely because the letters appear in it. Quoted, so a
        //! caller cannot send a regular expression and an address full of dots means dots.
        if (request.email() != null && !request.email().isBlank()) {
            filters.add(Criteria.where("emailAddress")
                    .regex("^" + Pattern.quote(request.email().trim()) + "$", "i"));
        }

        //! step 4 - the name, matched ANYWHERE unlike the two above. A name is not an identifier:
        //! somebody typing "sharma" wants every Sharma to look at.
        if (request.name() != null && !request.name().isBlank()) {
            filters.add(Criteria.where("fullName")
                    .regex(Pattern.quote(request.name().trim()), "i"));
        }

        //! step 5 - AND them together. NARROWING, not widening: the caller is filtering a list
        //! rather than asking "is this person here", which is what #6 does and why that one ORs.
        Criteria criteria = new Criteria().andOperator(filters.toArray(new Criteria[0]));

        //! step 6 - how many match, with the filter and nothing else
        // TODO: read guardians (how many match)
        long total = mongo.count(new Query(criteria), Guardian.class);

        //! step 7 - one page of them, the same filter plus the paging and the order
        // TODO: read guardians (one page of them)
        List<Guardian> rows = mongo.find(new Query(criteria).with(pageable), Guardian.class);

        return new PageImpl<>(rows, pageable, total);
    }

}
