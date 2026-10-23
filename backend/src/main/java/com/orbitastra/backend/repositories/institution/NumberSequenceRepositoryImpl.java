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
 *
 * <p>Every method builds its query first and runs it second, so what is being asked for and what
 * is being done with it can be read apart.
 */
@RequiredArgsConstructor
public class NumberSequenceRepositoryImpl implements NumberSequenceRepositoryCustom {

    private final MongoTemplate mongo;

    @Override
    public Optional<NumberSequence> allocate(String schoolId, NumberSequenceType type,
            String scopeKey) {

        //! step 1 - build the query. Matching the array element here is what makes the
        //! positional operator usable below: $ refers to the element this query matched.
        Query query = counter(schoolId, type, scopeKey);

        //! step 2 - build the update: add one to that element's counter
        Update update = new Update().inc("counters.$.nextValue", 1);

        //! step 3 - build the options. returnNew(false) hands back the document as it was, so
        //! the value the caller reads is the one it now owns.
        FindAndModifyOptions options = FindAndModifyOptions.options().returnNew(false);

        //! step 4 - run it, in one atomic step
        // TODO: write number sequence (take the next value)
        NumberSequence before = mongo.findAndModify(query, update, options, NumberSequence.class);

        //! step 5 - hand back what it was, or empty when there is no such counter
        return Optional.ofNullable(before);
    }

    @Override
    public boolean addCounterIfAbsent(String schoolId, SequenceCounter counter) {
        //! step 1 - build the query: this school, and NO entry for this type and scope. The
        //! guard lives here because a unique index cannot protect an array.
        Query query = new Query(Criteria.where("schoolId").is(schoolId)
                .norOperator(Criteria.where("counters")
                        .elemMatch(Criteria.where("sequenceType").is(counter.getSequenceType())
                                .and("scopeKey").is(counter.getScopeKey()))));

        //! step 2 - build the update
        Update update = new Update().push("counters", counter);

        //! step 3 - run it. Two callers racing on a school's first admission both get here;
        //! one matches and pushes, the other matches nothing.
        // TODO: write number sequence (add a counter if it is missing)
        long modified = mongo.updateFirst(query, update, NumberSequence.class).getModifiedCount();

        //! step 4 - say whether this call was the one that added it
        return modified > 0;
    }

    @Override
    public int addCounters(String schoolId, List<SequenceCounter> counters) {
        //! step 1 - nothing to add means no write at all
        if (counters == null || counters.isEmpty()) {
            return 0;
        }

        //! step 2 - build the query
        Query query = school(schoolId);

        //! step 3 - build the update. A push rather than saving the document back, because a
        //! full save would put every existing counter's nextValue back to whatever was read.
        Update update = new Update().push("counters").each(counters.toArray());

        //! step 4 - run it
        // TODO: write number sequences (add the missing counters)
        mongo.updateFirst(query, update, NumberSequence.class);

        //! step 5 - report how many went in
        return counters.size();
    }

    @Override
    public void setCounterPrefix(String schoolId, NumberSequenceType type, String scopeKey,
            String prefixTemplate) {

        //! step 1 - build the query, matching the one array entry
        Query query = counter(schoolId, type, scopeKey);

        //! step 2 - build the update
        Update update = new Update().set("counters.$.prefixTemplate", prefixTemplate);

        //! step 3 - run it
        // TODO: write number sequence (store the prefix template)
        mongo.updateFirst(query, update, NumberSequence.class);
    }

    /** Builds the query for the school's one document. Runs nothing. */
    private Query school(String schoolId) {
        return new Query(Criteria.where("schoolId").is(schoolId));
    }

    /**
     * Builds the query for the school's document AND the one array entry, so {@code $} has
     * something to point at. Runs nothing.
     */
    private Query counter(String schoolId, NumberSequenceType type, String scopeKey) {
        return new Query(Criteria.where("schoolId").is(schoolId)
                .and("counters").elemMatch(Criteria.where("sequenceType").is(type)
                        .and("scopeKey").is(scopeKey)));
    }
}
