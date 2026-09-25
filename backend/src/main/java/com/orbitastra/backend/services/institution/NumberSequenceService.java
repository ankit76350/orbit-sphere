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
import com.orbitastra.backend.services.institution.utils.NumberSequenceServiceUtils;

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


    // Every write below goes through the repository. The array operations this needs cannot be
    // derived query methods, so they live on NumberSequenceRepositoryCustom rather than being
    // hand-built with MongoTemplate here — data access belongs in the repository layer.
    private final NumberSequenceRepository numberSequences;
    private final NumberSequenceServiceUtils utils;
    
    //! Not an endpoint — the next number for one school and type ----------------------
    /**
     * Allocates the next number and returns it formatted.
     *
     * <p>Makes sure the school's document and the counter exist, then increments in one atomic
     * step and formats what the counter said before the increment.
    */
    public String next(String schoolId, NumberSequenceType type, String prefixTemplate) {
        utils.createCounterIfMissing(schoolId, type, prefixTemplate);

        // ONE atomic step. The array element is matched in the query and incremented through the
        // positional operator, and returnNew(false) hands back the document as it was, so the
        // value we read is the one this call owns. Reading the array into Java, adding one and
        // saving it back is how two students get the same admission number.
        NumberSequence before =
                numberSequences.allocate(schoolId, type, NumberSequenceServiceUtils.GLOBAL_SCOPE).orElse(null);

        SequenceCounter counter = before == null ? null : utils.findCounterForType(before, type);
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
            numberSequences.setCounterPrefix(schoolId, type, NumberSequenceServiceUtils.GLOBAL_SCOPE, prefix);
        }

        int width = counter.getPaddingWidth() == null ? 6 : counter.getPaddingWidth();
        String suffix = counter.getSuffixTemplate() == null ? "" : counter.getSuffixTemplate();

        return utils.fillDatePlaceholders(prefix) + utils.padToWidth(value, width)
                + utils.fillDatePlaceholders(suffix);
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

}
