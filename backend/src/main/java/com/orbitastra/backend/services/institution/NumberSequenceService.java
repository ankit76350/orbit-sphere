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
import com.orbitastra.backend.models.institution.enums.NumberSequenceType;
import com.orbitastra.backend.models.institution.enums.SequenceResetPolicy;

import lombok.RequiredArgsConstructor;

/**
 * Hands out the human-readable numbers documents carry — {@code SUB/2026/000001},
 * {@code ADM/2026/000123}, an invoice number, a receipt number.
 *
 * <p>The first thing in this codebase to read a {@link NumberSequence}. Every module needs one
 * eventually, so it is built once here rather than in whichever service happened to need it
 * first.
 *
 * <h2>Why the increment must be a database operation</h2>
 *
 * <p>The obvious version — read the row, add one, save it — hands the <b>same number to two
 * callers</b> the moment two requests overlap. Both read 41, both write 42, and two subscriptions
 * are called SUB/2026/000041. The unique index would catch it on a good day and let it through on
 * a bad one, because the index is on {@code subscriptionNo} per school and the collision is
 * silent when the two are in different schools.
 *
 * <p>So the increment is a single {@code findAndModify}: Mongo applies the {@code $inc} and hands
 * back the document as it was, atomically, and no two callers can see the same value. It is the
 * one operation here that cannot be written any other way.
 *
 * <h2>Where the number comes from</h2>
 *
 * <p>{@code nextValue} on the row is <b>the number to hand out next</b>, not the last one used.
 * The row seeded by provisioning starts at 1, so the first number allocated is 1.
 */
@Service
@RequiredArgsConstructor
public class NumberSequenceService {

    /** Every sequence a school has is scoped {@code GLOBAL} unless something needs otherwise. */
    public static final String GLOBAL_SCOPE = "GLOBAL";

    private final MongoTemplate mongo;

    /**
     * The next number for one school and one kind of document.
     *
     * @param schoolId the school the number belongs to — numbering restarts per school, which is
     *                 why two schools can both have a SUB/2026/000001
     * @param type     which counter
     * @param prefixTemplate used only when the row has none of its own. {@code {YYYY}} becomes
     *                 the four-digit year and {@code {YY}} the last two. Example: "SUB/{YYYY}/"
     * @return the formatted number, ready to store
     */
    public String next(String schoolId, NumberSequenceType type, String prefixTemplate) {
        //! step 1 - make sure there is a row to increment. Provisioning seeds one for every type
        //! a school needs, but a type added to the enum later has no row on schools provisioned
        //! before it — this is what stops that being a 500.
        ensureRow(schoolId, type, prefixTemplate);

        //! step 2 - take a number, atomically. returnNew(false) hands back the row as it WAS, so
        //! the value read is the one being allocated and the stored nextValue has already moved
        //! on. Two callers can never be given the same one.
        NumberSequence before = mongo.findAndModify(
                query(schoolId, type),
                new Update().inc("nextValue", 1),
                FindAndModifyOptions.options().returnNew(false),
                NumberSequence.class);

        if (before == null) {
            // The row was there a line ago. Something deleted it mid-request, and guessing a
            // number is worse than failing.
            throw ApiException.conflict("NUMBER_SEQUENCE_MISSING",
                    "The " + type + " number sequence for this school could not be read.");
        }

        //! step 3 - format it
        long value = before.getNextValue() == null ? 1L : before.getNextValue();

        String stored = before.getPrefixTemplate();
        String prefix = stored != null && !stored.isBlank()
                ? stored
                : (prefixTemplate == null ? "" : prefixTemplate);

        //! step 4 - remember the template on the row the first time it is used.
        //! Provisioning seeds these rows with no template, so without this the format of every
        //! number would depend on each caller passing the same string — and the day one of them
        //! passes a different one, a school's numbering changes shape halfway through.
        if ((stored == null || stored.isBlank()) && !prefix.isEmpty()) {
            mongo.updateFirst(query(schoolId, type),
                    new Update().set("prefixTemplate", prefix), NumberSequence.class);
        }
        int width = before.getPaddingWidth() == null ? 6 : before.getPaddingWidth();
        String suffix = before.getSuffixTemplate() == null ? "" : before.getSuffixTemplate();


        return resolve(prefix) + pad(value, width) + resolve(suffix);
    }

    /**
     * Creates the row if it is missing, and does nothing if it is not.
     *
     * <p>A duplicate key here means another request created it first, which is the good outcome
     * — the number itself is allocated by the increment that follows, and that is safe however
     * many callers arrive at once.
     */
    private void ensureRow(String schoolId, NumberSequenceType type, String prefixTemplate) {
        if (mongo.exists(query(schoolId, type), NumberSequence.class)) {
            return;
        }
        try {
            mongo.insert(NumberSequence.builder()
                    .schoolId(schoolId)
                    .sequenceType(type)
                    .scopeKey(GLOBAL_SCOPE)
                    .prefixTemplate(prefixTemplate)
                    .nextValue(1L)
                    .paddingWidth(6)
                    .resetPolicy(SequenceResetPolicy.NEVER)
                    .build());
        } catch (DuplicateKeyException raced) {
            // Somebody else created it between the check and the insert. Nothing to do.
        }
    }

    private Query query(String schoolId, NumberSequenceType type) {
        return new Query(Criteria.where("schoolId").is(schoolId)
                .and("sequenceType").is(type)
                .and("scopeKey").is(GLOBAL_SCOPE));
    }

    /**
     * Fills in the date placeholders.
     *
     * <p><b>UTC, not the school's time zone.</b> A number is an identifier, not a date: it has to
     * be stable and it has to be the same wherever it is read. Using the school's zone would mean
     * a subscription created at 11pm on 31 December in Kolkata is numbered 2027 while the row it
     * sits next to says 2026.
     */
    private String resolve(String template) {
        if (template == null || template.isEmpty()) {
            return "";
        }
        ZonedDateTime now = ZonedDateTime.ofInstant(Instant.now(), ZoneOffset.UTC);
        return template
                .replace("{YYYY}", String.valueOf(now.getYear()))
                .replace("{YY}", String.format("%02d", now.getYear() % 100));
    }

    private String pad(long value, int width) {
        return String.format("%0" + Math.max(1, width) + "d", value);
    }
}
