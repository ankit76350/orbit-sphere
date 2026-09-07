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
        createCounterIfMissing(schoolId, type, prefixTemplate);

        // ONE atomic step. The array element is matched in the query and incremented through the
        // positional operator, and returnNew(false) hands back the document as it was, so the
        // value we read is the one this call owns. Reading the array into Java, adding one and
        // saving it back is how two students get the same admission number.
        NumberSequence before =
                numberSequences.allocate(schoolId, type, GLOBAL_SCOPE).orElse(null);

        SequenceCounter counter = before == null ? null : findCounterForType(before, type);
        if (counter == null) {
            // createCounterIfMissing just ran, so this means somebody removed it in between, or the
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

        return fillDatePlaceholders(prefix) + padToWidth(value, width)
                + fillDatePlaceholders(suffix);
    }

    /*
    ---------------------------------------------------------------------------------
    ---------------------------------------------------------------------------------
    ---------------------------------------------------------------------------------
    ---------------------------------------------------------------------------------
    ---------------------------------------------------------------------------------
    ---------------------------------------------------------------------------------
    ---------------------------------------------------------------------------------
    ---------------------------------------------------------------------------------
    ---------------------------------------------------------------------------------
    ---------------------------------------------------------------------------------
    ---------------------------------------------------------------------------------
    ---------------------------------------------------------------------------------
    ---------------------------------------------------------------------------------
    ---------------------------------------------------------------------------------
    ---------------------------------------------------------------------------------
    ---------------------------------------------------------------------------------
    ---------------------------------------------------------------------------------
    ---------------------------------------------------------------------------------
    ---------------------------------------------------------------------------------
    ---------------------------------------------------------------------------------
    ---------------------------------------------------------------------------------
    ---------------------------------------------------------------------------------
    ---------------------------------------------------------------------------------
    ---------------------------------------------------------------------------------
    ---------------------------------------------------------------------------------
    */

    /**
     * Makes sure this school has a counters document, and that this counter is inside it.
     *
     * <p><b>Two writes, because the array cannot be pushed to before the document exists.</b> The
     * document comes first, then the counter — and both are safe to lose a race over, which is
     * what makes this callable on every allocation rather than only at provisioning time.
     *
     * <p>The document is created with a {@code save} rather than an update, so the auditing hook
     * fills in {@code createdAt} and {@code createdByDocsId}; an update would leave both null.
     * The unique index on {@code schoolId} is what makes that race safe — the loser catches the
     * duplicate and carries on, because the document it wanted now exists.
     *
     * <p>The counter's own guard is inside {@code addCounterIfAbsent}, which pushes only when no
     * entry for this type and scope is there. Two callers racing on a school's first admission
     * both arrive here; one adds it, the other is told it already existed. False is success, not
     * failure, which is why the answer is not checked.
     *
     * Used by:
     * - next()
     */
    private void createCounterIfMissing(String schoolId, NumberSequenceType type,
            String prefixTemplate) {

        if (!numberSequences.existsBySchoolId(schoolId)) {
            try {
                numberSequences.save(NumberSequence.builder().schoolId(schoolId).build());
            } catch (DuplicateKeyException raced) {
                // Somebody else created it between the check and the insert. Nothing to do.
            }
        }

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
     * Picks this type's counter out of a document that was read back.
     *
     * <p>Null when the document has no counters at all, or none for this type and scope — which
     * the caller reads as "somebody removed it between the seeding and the allocation".
     *
     * Used by:
     * - next()
     */
    private SequenceCounter findCounterForType(NumberSequence document,
            NumberSequenceType type) {
        if (document.getCounters() == null) {
            return null;
        }
        return document.getCounters().stream()
                .filter(c -> c.getSequenceType() == type
                        && GLOBAL_SCOPE.equals(c.getScopeKey()))
                .findFirst()
                .orElse(null);
    }

    /**
     * Fills the date placeholders in a prefix or suffix template.
     *
     * <p>{@code SUB/{YYYY}/{MM}/} becomes {@code SUB/2026/09/}. Resolved at the moment a number
     * is handed out and then stored back onto the counter, so a school's numbering cannot change
     * shape half way through a run.
     *
     * <p>UTC, deliberately: a number's shape must not depend on which server allocated it.
     *
     * Used by:
     * - next()
     */
    private String fillDatePlaceholders(String template) {
        if (template == null || template.isEmpty()) {
            return "";
        }
        ZonedDateTime now = ZonedDateTime.ofInstant(Instant.now(), ZoneOffset.UTC);
        return template
                .replace("{YYYY}", String.valueOf(now.getYear()))
                .replace("{YY}", String.format("%02d", now.getYear() % 100))
                .replace("{MM}", String.format("%02d", now.getMonthValue()));
    }

    /**
     * The counter's value as fixed-width digits — 41 becomes {@code 000041}.
     *
     * <p>Fixed width is what makes numbers sort and read alike: {@code SUB/2026/09/000041} next
     * to {@code SUB/2026/09/000412} lines up, where {@code 41} and {@code 412} do not.
     *
     * <p>Width is floored at 1, so a counter stored with 0 or a negative still produces a number
     * rather than an exception from {@code String.format}.
     *
     * Used by:
     * - next()
     */
    private String padToWidth(long value, int width) {
        return String.format("%0" + Math.max(1, width) + "d", value);
    }
}
