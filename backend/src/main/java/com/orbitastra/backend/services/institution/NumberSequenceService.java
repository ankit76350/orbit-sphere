package com.orbitastra.backend.services.institution;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import com.orbitastra.backend.common.error.exception.ApiException;
import com.orbitastra.backend.models.institution.NumberSequence;
import com.orbitastra.backend.models.institution.embedded.SequenceCounter;
import com.orbitastra.backend.models.institution.enums.NumberSequenceType;
import com.orbitastra.backend.models.institution.enums.SequenceResetPolicy;
import com.orbitastra.backend.repositories.institution.NumberSequenceRepository;

import lombok.RequiredArgsConstructor;

/**
 * Hands out the next human-readable business number for one school.
 *
 * <p>Rewritten on 2026-09-05 when every counter moved into one document per school. The three
 * rules the new shape puts on this class are written out on
 * {@link NumberSequence}; what follows is how they are met.
 */
@Service
@RequiredArgsConstructor
public class NumberSequenceService {

    public static final String GLOBAL_SCOPE = "GLOBAL";

    // Every write below goes through the repository. The array operations this needs cannot be
    // derived query methods, so they live on NumberSequenceRepositoryCustom rather than being
    // hand-built with MongoTemplate here — data access belongs in the repository layer.
    private final NumberSequenceRepository numberSequences;

    /**
     * Allocates the next number and returns it formatted.
     *
     * <p>Makes sure the school's document and the counter exist, then increments in one atomic
     * step and formats what the counter said before the increment.
     */
    public String next(String schoolId, NumberSequenceType type, String prefixTemplate) {
        ensureCounter(schoolId, type, prefixTemplate);

        // ONE atomic step. The array element is matched in the query and incremented through the
        // positional operator, and returnNew(false) hands back the document as it was, so the
        // value we read is the one this call owns. Reading the array into Java, adding one and
        // saving it back is how two students get the same admission number.
        NumberSequence before =
                numberSequences.allocate(schoolId, type, GLOBAL_SCOPE).orElse(null);

        SequenceCounter counter = before == null ? null : findCounter(before, type);
        if (counter == null) {
            // ensureCounter just ran, so this means somebody removed it in between, or the
            // school's document is gone.
            throw ApiException.conflict("NUMBER_SEQUENCE_MISSING",
                    "The " + type + " number sequence for this school could not be read.");
        }

        long value = counter.getNextValue() == null ? 1L : counter.getNextValue();

        // The template stored on the counter wins, so a school's numbering cannot change shape
        // half way through a run just because a caller passed something different.
        String stored = counter.getPrefixTemplate();
        String prefix = stored != null && !stored.isBlank()
                ? stored
                : (prefixTemplate == null ? "" : prefixTemplate);

        // First caller to bring a template writes it onto the counter, so every later number in
        // this run reads the same.
        if ((stored == null || stored.isBlank()) && !prefix.isEmpty()) {
            numberSequences.setCounterPrefix(schoolId, type, GLOBAL_SCOPE, prefix);
        }

        int width = counter.getPaddingWidth() == null ? 6 : counter.getPaddingWidth();
        String suffix = counter.getSuffixTemplate() == null ? "" : counter.getSuffixTemplate();

        return resolve(prefix) + pad(value, width) + resolve(suffix);
    }

    /**
     * Makes sure the school has a counters document and that this counter is in it.
     *
     * <p>Two steps, because the array cannot be pushed to before the document exists.
     */
    private void ensureCounter(String schoolId, NumberSequenceType type, String prefixTemplate) {
        ensureDocument(schoolId);

        // The guard is inside addCounterIfAbsent: it pushes only when no entry for this type
        // and scope is there. Two callers racing on a school's first admission both get here;
        // one adds it, the other is told it already existed. False is success, not failure,
        // which is why the answer is not checked.
        numberSequences.addCounterIfAbsent(schoolId, SequenceCounter.builder()
                .sequenceType(type)
                .scopeKey(GLOBAL_SCOPE)
                .prefixTemplate(prefixTemplate)
                .nextValue(1L)
                .paddingWidth(6)
                .resetPolicy(SequenceResetPolicy.NEVER)
                .build());
    }

    /**
     * Creates the school's counters document if it has none.
     *
     * <p>A {@code save} rather than an update, so the auditing hook fills in createdAt and
     * createdByDocsId; an update would leave both null. The unique index on schoolId is what
     * makes the race safe — the loser catches the duplicate and carries on, because the document
     * it wanted now exists.
     */
    private void ensureDocument(String schoolId) {
        if (numberSequences.existsBySchoolId(schoolId)) {
            return;
        }
        try {
            numberSequences.save(NumberSequence.builder().schoolId(schoolId).build());
        } catch (DuplicateKeyException raced) {
            // Somebody else created it between the check and the insert. Nothing to do.
        }
    }

    /** Finds the counter inside a document that was read back. */
    private SequenceCounter findCounter(NumberSequence document, NumberSequenceType type) {
        if (document.getCounters() == null) {
            return null;
        }
        return document.getCounters().stream()
                .filter(c -> c.getSequenceType() == type
                        && GLOBAL_SCOPE.equals(c.getScopeKey()))
                .findFirst()
                .orElse(null);
    }

    private String resolve(String template) {
        if (template == null || template.isEmpty()) {
            return "";
        }
        ZonedDateTime now = ZonedDateTime.ofInstant(Instant.now(), ZoneOffset.UTC);
        return template
                .replace("{YYYY}", String.valueOf(now.getYear()))
                .replace("{YY}", String.format("%02d", now.getYear() % 100))
                .replace("{MM}", String.format("%02d", now.getMonthValue()));
    }

    private String pad(long value, int width) {
        return String.format("%0" + Math.max(1, width) + "d", value);
    }
}
