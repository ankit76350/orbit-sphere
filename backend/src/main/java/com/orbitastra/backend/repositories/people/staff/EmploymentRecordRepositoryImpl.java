package com.orbitastra.backend.repositories.people.staff;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.query.Criteria;

import com.orbitastra.backend.models.people.staff.EmploymentRecord;

import lombok.RequiredArgsConstructor;

/**
 * The custom fragment behind {@link EmploymentRecordRepositoryCustom}.
 *
 * <p><b>The name and the package are load-bearing.</b> Spring Data resolves a fragment by
 * {@code <Interface>Impl} in the same package as the repository. Rename it or move it and the
 * application compiles, starts, and fails only when somebody calls it.
 */
@RequiredArgsConstructor
public class EmploymentRecordRepositoryImpl implements EmploymentRecordRepositoryCustom {

    private final MongoTemplate mongo;

    @Override
    public Map<String, Long> filledHeadcounts(String schoolId,
            Collection<String> positionDocsIds) {

        //! step 1 - nothing to count is not a query. `$in: []` matches nothing, so the round trip
        //! could only ever prove what the caller already knows.
        Map<String, Long> counts = new LinkedHashMap<>();
        if (positionDocsIds == null || positionDocsIds.isEmpty()) {
            return counts;
        }

        //! step 2 - the tenant first, then the seats asked about, then only what is held NOW.
        //!
        //! THE TENANT IS NOT REDUNDANT even though the ids came from a school-scoped read. The
        //! ids are document ids and another school's are real ids; leaving it off would mean a
        //! caller who learns an id from anywhere gets a count for it.
        Aggregation counting = Aggregation.newAggregation(
                Aggregation.match(Criteria.where("schoolId").is(schoolId)
                        .and("positionDocsId").in(positionDocsIds)
                        .and("current").is(true)),
                Aggregation.group("positionDocsId").count().as("filled"));

        //! step 3 - one round trip for the whole page
        // TODO: reading employment records (how many hold each seat)
        AggregationResults<Document> grouped =
                mongo.aggregate(counting, EmploymentRecord.class, Document.class);

        //! step 4 - unwrap. `_id` is the grouping key, which is the position id; `filled` comes
        //! back as an Integer from a count, so it is read as a Number rather than cast.
        for (Document one : grouped) {
            Object id = one.get("_id");
            Object filled = one.get("filled");
            if (id != null && filled instanceof Number number) {
                counts.put(id.toString(), number.longValue());
            }
        }
        return counts;
    }
}
