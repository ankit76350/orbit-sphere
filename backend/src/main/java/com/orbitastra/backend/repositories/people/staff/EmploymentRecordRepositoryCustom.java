package com.orbitastra.backend.repositories.people.staff;

import java.util.Collection;
import java.util.Map;

/**
 * The one thing #15 needs that a derived query cannot give it: many counts in one round trip.
 */
public interface EmploymentRecordRepositoryCustom {

    /**
     * How many people currently hold each of these seats.
     *
     * <p><b>One grouped count, not one query per seat.</b> A page of twenty seats answered by
     * {@code countBySchoolIdAndPositionDocsIdAndCurrentIsTrue} is twenty round trips to render one
     * table — the N+1 that makes a list endpoint slower the more it returns.
     *
     * <p><b>A seat with nobody in it is absent from the map, not zero in it.</b> An aggregation
     * groups what it matched and cannot invent a bucket for what it did not, so the caller reads
     * through {@code getOrDefault(id, 0L)}. Returning zeroes would mean building the empty ones
     * here from the same id list the caller already holds.
     *
     * @param positionDocsIds the seats to count for; an empty collection reads nothing and
     *                        returns an empty map, because {@code $in: []} matches nothing and
     *                        the round trip proving it is waste
     */
    Map<String, Long> filledHeadcounts(String schoolId, Collection<String> positionDocsIds);
}
