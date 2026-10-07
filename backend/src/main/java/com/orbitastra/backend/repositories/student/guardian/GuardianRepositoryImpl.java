package com.orbitastra.backend.repositories.student.guardian;

import java.util.List;
import java.util.regex.Pattern;

import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

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
            int limit) {

        if (digits == null || digits.isEmpty()) {
            return List.of();
        }

        //! THE SAME PHONE RULE as crm #15 and as the student search beside this, and it has to be
        //! the same: a number stored as "+91 98765 43210" and asked about as "9876543210" is one
        //! number. Anything is allowed between the digits. A full-length number is compared on its
        //! last ten so a country code stops mattering; a shorter one has to match the whole
        //! number, or "543210" matches every number ending in those six digits.
        StringBuilder pattern = new StringBuilder(wholeNumber ? "^[^0-9]*" : "");
        boolean first = true;
        for (char digit : digits.toCharArray()) {
            if (!first) {
                pattern.append("[^0-9]*");
            }
            pattern.append(Pattern.quote(String.valueOf(digit)));
            first = false;
        }
        pattern.append("[^0-9]*$");

        //! THE ALTERNATE NUMBER COUNTS TOO. It is the one a family gives as "my husband's phone",
        //! and leaving it out would miss exactly the second parent this read exists to find.
        Criteria criteria = Criteria.where("schoolId").is(schoolId)
                .orOperator(
                        Criteria.where("phoneNumber").regex(pattern.toString()),
                        Criteria.where("alternatePhoneNumber").regex(pattern.toString()));

        Query query = new Query(criteria)
                .with(Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id")))
                .limit(limit);

        // TODO: read guardians (whose number is this)
        return mongo.find(query, Guardian.class);
    }
}
