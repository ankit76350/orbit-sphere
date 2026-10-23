package com.orbitastra.backend.repositories.institution;

import java.util.List;
import java.util.Optional;

import com.orbitastra.backend.models.institution.NumberSequence;
import com.orbitastra.backend.models.institution.embedded.SequenceCounter;
import com.orbitastra.backend.models.institution.enums.NumberSequenceType;

/**
 * The part of {@link NumberSequenceRepository} that cannot be a derived query method.
 *
 * <p>Every counter lives inside one document per school, so nothing here can be expressed as a
 * method name: allocating means {@code $inc} on a matched array element, and adding a counter
 * means a {@code $push} that only fires when the entry is not already there. Both are one atomic
 * step, which is the whole point — see the three rules on {@link NumberSequence}.
 *
 * <p>Spring Data finds the implementation by name: {@code NumberSequenceRepositoryImpl}.
 * Renaming that class breaks the wiring silently at startup, so do not.
 */
public interface NumberSequenceRepositoryCustom {

    /**
     * Takes the next value for one counter, in a single atomic step.
     *
     * @return the document <b>as it was before</b> the increment, so the caller reads the value
     *         it now owns; empty when the school or the counter is not there
     */
    Optional<NumberSequence> allocate(String schoolId, NumberSequenceType type, String scopeKey);

    /**
     * Adds a counter only if the school has none for that type and scope.
     *
     * <p>This is what replaced the old unique index on {@code schoolId + sequenceType +
     * scopeKey}. A unique index cannot protect an array — Mongo de-duplicates the identical
     * keys one document generates — so the condition has to be part of the write.
     *
     * @return true when this call added it, false when it was already there
     */
    boolean addCounterIfAbsent(String schoolId, SequenceCounter counter);

    /**
     * Adds several counters in one write, for provisioning.
     *
     * <p>The caller has already worked out which types are missing. A {@code $push} rather than
     * saving the document back, because a full save would put every existing counter's
     * {@code nextValue} back to whatever was read — which is how a school's numbering gets reset
     * by accident.
     *
     * @return how many were added
     */
    int addCounters(String schoolId, List<SequenceCounter> counters);

    /**
     * Writes the prefix template onto a counter that has none yet.
     *
     * <p>The first caller to bring a template fixes the shape for the whole run, so every later
     * number in it reads the same way.
     */
    void setCounterPrefix(String schoolId, NumberSequenceType type, String scopeKey,
            String prefixTemplate);
}
