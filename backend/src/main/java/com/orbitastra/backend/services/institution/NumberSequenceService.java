package com.orbitastra.backend.services.institution;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import com.orbitastra.backend.common.error.exception.ApiException;
import com.orbitastra.backend.models.institution.NumberSequence;
import com.orbitastra.backend.models.institution.embedded.SequenceCounter;
import com.orbitastra.backend.models.institution.enums.NumberSequenceType;
import com.orbitastra.backend.models.institution.enums.SequenceResetPolicy;

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

    private final MongoTemplate mongo;

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
        NumberSequence before = mongo.findAndModify(
                counterQuery(schoolId, type),
                new Update().inc("counters.$.nextValue", 1),
                FindAndModifyOptions.options().returnNew(false),
                NumberSequence.class);

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
            mongo.updateFirst(counterQuery(schoolId, type),
                    new Update().set("counters.$.prefixTemplate", prefix), NumberSequence.class);
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

        // GUARDED PUSH. The old shape had a unique index on schoolId + type + scope, which made
        // a second copy impossible. A unique index cannot do that inside an array, so the
        // condition lives in the query: push only if no entry for this type and scope is there.
        //
        // Two callers racing on a school's first admission both run this; one pushes, the other
        // matches nothing and does nothing. A modified count of zero is success here, not
        // failure, which is why it is not checked.
        Query absent = new Query(Criteria.where("schoolId").is(schoolId)
                .norOperator(Criteria.where("counters")
                        .elemMatch(Criteria.where("sequenceType").is(type)
                                .and("scopeKey").is(GLOBAL_SCOPE))));

        mongo.updateFirst(absent,
                new Update().push("counters", SequenceCounter.builder()
                        .sequenceType(type)
                        .scopeKey(GLOBAL_SCOPE)
                        .prefixTemplate(prefixTemplate)
                        .nextValue(1L)
                        .paddingWidth(6)
                        .resetPolicy(SequenceResetPolicy.NEVER)
                        .build()),
                NumberSequence.class);
    }

    /**
     * Creates the school's counters document if it has none.
     *
     * <p>An insert rather than an upsert so the auditing hook fills in createdAt and
     * createdByDocsId; a MongoTemplate update would leave both null. The unique index on
     * schoolId is what makes the race safe — the loser catches the duplicate and carries on,
     * because the document it wanted now exists.
     */
    private void ensureDocument(String schoolId) {
        if (mongo.exists(schoolQuery(schoolId), NumberSequence.class)) {
            return;
        }
        try {
            mongo.insert(NumberSequence.builder().schoolId(schoolId).build());
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

    /** The school's document. */
    private Query schoolQuery(String schoolId) {
        return new Query(Criteria.where("schoolId").is(schoolId));
    }

    /**
     * The school's document AND the one array entry, which is what makes the positional
     * operator usable: {@code $} refers to the element this query matched.
     */
    private Query counterQuery(String schoolId, NumberSequenceType type) {
        return new Query(Criteria.where("schoolId").is(schoolId)
                .and("counters").elemMatch(Criteria.where("sequenceType").is(type)
                        .and("scopeKey").is(GLOBAL_SCOPE)));
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
