package com.orbitastra.backend.services.institution.utils;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Component;

import com.orbitastra.backend.models.institution.NumberSequence;
import com.orbitastra.backend.models.institution.embedded.SequenceCounter;
import com.orbitastra.backend.models.institution.enums.NumberSequenceType;
import com.orbitastra.backend.models.institution.enums.SequenceResetPolicy;
import com.orbitastra.backend.repositories.institution.NumberSequenceRepository;

import lombok.RequiredArgsConstructor;

/**
 * Everything {@link com.orbitastra.backend.services.institution.NumberSequenceService} does that is
 * not its one public method.
 *
 * <p>Per the service folder rules: a main service has its own {@code utils}, <b>a service file
 * holds nothing but its endpoint methods</b>, and <b>a method here never calls another method
 * here</b>. Only the service calls these.
 *
 * <p><b>This file is new on 2026-09-24</b>, created when the rule changed: the four methods below
 * were private in the service, and none of them is an endpoint. Unlike every other {@code utils} in
 * this project, its service is not a controller's — nothing calls {@code NumberSequenceService}
 * over HTTP. It allocates the numbers every other module stamps on its documents, and that made it
 * the one service where the helpers <i>were</i> the substance.
 */
@Component
@RequiredArgsConstructor
public class NumberSequenceServiceUtils {

    public static final String GLOBAL_SCOPE = "GLOBAL";

    private final NumberSequenceRepository numberSequences;

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
    public void createCounterIfMissing(String schoolId, NumberSequenceType type,
            String prefixTemplate) {

        if (!numberSequences.existsBySchoolId(schoolId)) {
            NumberSequence document = NumberSequence.builder().schoolId(schoolId).build();
            try {
                // TODO: insert number sequence
                numberSequences.save(document);
            } catch (DuplicateKeyException raced) {
                // Somebody else created it between the check and the insert. Nothing to do.
            }
        }

        SequenceCounter counter = SequenceCounter.builder()
                .sequenceType(type)
                .scopeKey(GLOBAL_SCOPE)
                .prefixTemplate(prefixTemplate)
                .nextValue(1L)
                .paddingWidth(6)
                .resetPolicy(SequenceResetPolicy.NEVER)
                .build();

        // TODO: update number sequence
        numberSequences.addCounterIfAbsent(schoolId, counter);
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
    public SequenceCounter findCounterForType(NumberSequence document,
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
    public String fillDatePlaceholders(String template) {
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
    public String padToWidth(long value, int width) {
        return String.format("%0" + Math.max(1, width) + "d", value);
    }
}
