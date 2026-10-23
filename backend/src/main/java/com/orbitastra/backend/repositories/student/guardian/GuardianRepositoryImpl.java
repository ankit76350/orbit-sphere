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

        //! THE ALTERNATE NUMBER COUNTS ONLY WHEN THE CALLER SAYS SO. For #4 and #9 it must —
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

        //! step 2 - the one search box: the THREE things a guardian is known by, OR-ed. For the
        //! caller who has something and does not want to decide which field it is.
        //!
        //! LOOSE ON PURPOSE, unlike the three precise filters below it. "9876" is meant to find a
        //! number containing it and "gmail" everybody on gmail — this is the box on a screen, not
        //! an identity question, so nothing here is anchored.
        //!
        //! Quoted, so a caller sending "(" gets an empty result rather than a 500 from a broken
        //! pattern, and ".*" finds the guardians with a dot in their name.
        if (request.search() != null && !request.search().isBlank()) {
            String typed = request.search().trim();
            String needle = Pattern.quote(typed);

            List<Criteria> anywhere = new ArrayList<>();
            anywhere.add(Criteria.where("fullName").regex(needle, "i"));
            anywhere.add(Criteria.where("emailAddress").regex(needle, "i"));

            //! THE DIGITS ARE TAKEN FROM THE BOX ITSELF, not from the service, because this is a
            //! different question from the one the service worked out. `phone` has to agree with
            //! what #7 refuses on — identity, last ten digits. This is "does the number contain
            //! what I typed", so it is always a loose contains, however many digits arrived.
            String typedDigits = PhoneMatch.digitsOf(typed);
            if (!typedDigits.isEmpty()) {
                String loose = PhoneMatch.loosePattern(typedDigits, false);
                anywhere.add(Criteria.where("phoneNumber").regex(loose));
                anywhere.add(Criteria.where("alternatePhoneNumber").regex(loose));
            }

            filters.add(new Criteria().orOperator(anywhere.toArray(new Criteria[0])));
        }

        //! step 3 - the phone, on its DIGITS and across BOTH numbers. The same rule #7 refuses on,
        //! and it has to be: a caller who checks here, sees nothing, and is then refused by #7
        //! would have been told two different things about one number.
        if (digits != null && !digits.isEmpty()) {
            String pattern = PhoneMatch.loosePattern(digits, wholeNumber);
            filters.add(new Criteria().orOperator(
                    Criteria.where("phoneNumber").regex(pattern),
                    Criteria.where("alternatePhoneNumber").regex(pattern)));
        }

        //! step 4 - the email, WHOLE and case-insensitive. A question about identity: "a@b.com"
        //! must not match "maria@b.com" merely because the letters appear in it. Quoted, so a
        //! caller cannot send a regular expression and an address full of dots means dots.
        if (request.email() != null && !request.email().isBlank()) {
            filters.add(Criteria.where("emailAddress")
                    .regex("^" + Pattern.quote(request.email().trim()) + "$", "i"));
        }

        //! step 5 - the name, matched ANYWHERE unlike the two above. A name is not an identifier:
        //! somebody typing "sharma" wants every Sharma to look at.
        if (request.name() != null && !request.name().isBlank()) {
            filters.add(Criteria.where("fullName")
                    .regex(Pattern.quote(request.name().trim()), "i"));
        }

        //! step 6 - the occupation, anywhere. "teacher" finds "Primary school teacher", which is
        //! the only way this field is useful: it is typed free-hand, so nobody can spell it the
        //! way it was stored.
        if (request.occupation() != null && !request.occupation().isBlank()) {
            filters.add(Criteria.where("occupation")
                    .regex(Pattern.quote(request.occupation().trim()), "i"));
        }

        //! step 7 - the address, anywhere. THE ONE A SCHOOL ACTUALLY ASKS: everybody in one
        //! village, so a bus route change or a flooded road can be rung round. Anchoring it would
        //! be useless — an address is a paragraph, and the village is in the middle of it.
        if (request.address() != null && !request.address().isBlank()) {
            filters.add(Criteria.where("address")
                    .regex(Pattern.quote(request.address().trim()), "i"));
        }

        //! step 8 - AND them together. NARROWING, not widening: the caller is filtering a list
        //! rather than asking "is this person here". The one field that ORs is the search box, and
        //! it ORs WITHIN ITSELF — it still narrows against everything else here.
        Criteria criteria = new Criteria().andOperator(filters.toArray(new Criteria[0]));

        //! step 9 - how many match, with the filter and nothing else
        // TODO: read guardians (how many match)
        long total = mongo.count(new Query(criteria), Guardian.class);

        //! step 10 - one page of them, the same filter plus the paging and the order
        // TODO: read guardians (one page of them)
        List<Guardian> rows = mongo.find(new Query(criteria).with(pageable), Guardian.class);

        return new PageImpl<>(rows, pageable, total);
    }

}
