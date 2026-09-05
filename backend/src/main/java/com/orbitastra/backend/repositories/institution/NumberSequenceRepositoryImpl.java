package com.orbitastra.backend.repositories.institution;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import com.orbitastra.backend.models.institution.NumberSequence;
import com.orbitastra.backend.models.institution.embedded.SequenceCounter;
import com.orbitastra.backend.models.institution.enums.NumberSequenceType;

import lombok.RequiredArgsConstructor;

/**
 * The array writes behind the number counters.
 *
 * <p><b>Everything here is one operation.</b> Not one of these methods reads a document, changes
 * it in Java and writes it back — that pattern is how two students end up with the same
 * admission number, and it is why none of this could stay in the service.
 */
@RequiredArgsConstructor
public class NumberSequenceRepositoryImpl implements NumberSequenceRepositoryCustom {

    private final MongoTemplate mongo;

    @Override
    public Optional<NumberSequence> allocate(String schoolId, NumberSequenceType type,
            String scopeKey) {

        // The array element is matched in the query, which is what makes the positional
        // operator usable: $ refers to the element this query matched. returnNew(false) hands
        // back the document as it was, so the value the caller reads is the one it owns.
        return Optional.ofNullable(mongo.findAndModify(
                counter(schoolId, type, scopeKey),
                new Update().inc("counters.$.nextValue", 1),
                FindAndModifyOptions.options().returnNew(false),
                NumberSequence.class));
    }

    @Override
    public boolean addCounterIfAbsent(String schoolId, SequenceCounter counter) {
        // Push only when no entry for this type and scope exists. Two callers racing on a
        // school's first admission both run this; one pushes, the other matches nothing.
        Query absent = new Query(Criteria.where("schoolId").is(schoolId)
                .norOperator(Criteria.where("counters")
                        .elemMatch(Criteria.where("sequenceType").is(counter.getSequenceType())
                                .and("scopeKey").is(counter.getScopeKey()))));

        return mongo.updateFirst(absent, new Update().push("counters", counter),
                NumberSequence.class).getModifiedCount() > 0;
    }

    @Override
    public int addCounters(String schoolId, List<SequenceCounter> counters) {
        if (counters == null || counters.isEmpty()) {
            return 0;
        }
        mongo.updateFirst(school(schoolId),
                new Update().push("counters").each(counters.toArray()),
                NumberSequence.class);
        return counters.size();
    }

    @Override
    public void setCounterPrefix(String schoolId, NumberSequenceType type, String scopeKey,
            String prefixTemplate) {

        mongo.updateFirst(counter(schoolId, type, scopeKey),
                new Update().set("counters.$.prefixTemplate", prefixTemplate),
                NumberSequence.class);
    }

    /** The school's one document. */
    private Query school(String schoolId) {
        return new Query(Criteria.where("schoolId").is(schoolId));
    }

    /** The school's document AND the one array entry, so {@code $} has something to point at. */
    private Query counter(String schoolId, NumberSequenceType type, String scopeKey) {
        return new Query(Criteria.where("schoolId").is(schoolId)
                .and("counters").elemMatch(Criteria.where("sequenceType").is(type)
                        .and("scopeKey").is(scopeKey)));
    }
}
