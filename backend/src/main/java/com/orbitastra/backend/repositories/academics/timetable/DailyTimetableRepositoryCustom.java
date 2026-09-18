package com.orbitastra.backend.repositories.academics.timetable;

import java.time.LocalDate;
import java.util.Map;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.orbitastra.backend.dto.academics.timetable.request.DailyTimetableSearchRequest;
import com.orbitastra.backend.dto.academics.timetable.response.DailyTimetableSummaryResponse;
import com.orbitastra.backend.models.academics.timetable.embedded.TimetableEntry;

/**
 * The part of #10's read that a derived query cannot express.
 *
 * <p>Two things put it here: the filters reach <i>inside</i> the embedded periods — which needs an
 * {@code $elemMatch} to pair a class with a section — and the row is five counts over an array
 * this endpoint deliberately never ships.
 *
 * <h2>And the three single-entry writes, which are the module's whole persistence risk</h2>
 *
 * <p>#3, #4 and #5 touch <b>one embedded period</b> — {@code $push}, {@code $set} through an array
 * filter, {@code $pull} — and never re-save the document. A day is 120 KB; rewriting all of it to
 * change one teacher would make every substitution a race with every other edit of that morning.
 *
 * <h2>Three things were measured before these were written — 2026-09-18</h2>
 *
 * <p><b>1. An entry's {@code _id} is a real {@code ObjectId}, and only the ARRAY FILTER needs to
 * be told.</b> {@code TimetableEntry.id} is a {@code String} in Java, and
 * {@code @Field(targetType = OBJECT_ID)} means the stored value is not one — in the raw driver a
 * hex string matches <b>zero</b> documents rather than failing. Spring Data's query mapper knows
 * that mapping and converts a {@code String} for you in a <i>query</i>: passing one to
 * {@code where("entries._id")} was measured to work. <b>It cannot do the same for an array
 * filter</b>, whose {@code entry._id} is a placeholder path belonging to no entity — a
 * {@code String} there matches no element, the {@code $set} quietly applies to nothing, and the
 * document still reports as matched. That is exactly the silent success the model contract's rule 2
 * exists to prevent, so {@link #patchEntry} converts and the suite has a test that fails when it
 * does not.
 *
 * <p><b>2. Spring Data bumps {@code @Version} on a targeted update by itself.</b> Measured by
 * removing the explicit {@code $inc} and watching the version still move: {@code MongoTemplate}
 * adds one for a versioned entity when the update does not already carry it, so a {@code $push}
 * cannot leave the version standing still while the day gains a period — which is what #2's
 * required version depends on. <b>The explicit {@code $inc} stays anyway</b>: it costs nothing, it
 * says out loud what the guarantee is, and it is what a future switch to a raw
 * {@code MongoCollection} call would otherwise lose without a test noticing.
 *
 * <p><b>3. A {@code LocalTime} is stored as a BSON date carrying the day the document was
 * written</b>, not a time of day: {@code 09:00} became {@code 2026-09-17T03:30:00Z}. Two days
 * written on one afternoon share that date part, and two written a day apart do not — so
 * <b>{@code startTime} can never be compared in a Mongo query</b>. That is why the overlap rules
 * are checked in Java against the document just read, and why no method here tries to make them
 * part of the update. See open item 8 of the module plan.
 */
public interface DailyTimetableRepositoryCustom {

    /**
     * One page of a year's days, as counts rather than periods.
     *
     * <p><b>It returns the response type, and that is deliberate.</b> The rows are not
     * {@code DailyTimetable} documents — they are an aggregation's output, and inventing an
     * intermediate type to carry five ints between two layers would be a class whose only purpose
     * is to be copied into another.
     */
    Page<DailyTimetableSummaryResponse> search(String schoolId, String academicYear,
            DailyTimetableSearchRequest request, Pageable pageable);

    /**
     * Endpoint #3 — add one period with a {@code $push}, never a re-save.
     *
     * <p><b>The period code is guarded in the query itself</b>, so two clerks adding "P3" to one
     * section at the same moment cannot both succeed: the second one matches no document. That is
     * the one conflict rule expressible without comparing times — see measurement 3 above — and it
     * is the one most likely to be raced, because a period code is what a person types twice.
     *
     * @return the number of documents modified: 1 when the period was added, 0 when the day was
     *         gone or that section already had that period code
     */
    long pushEntry(String schoolId, LocalDate date, TimetableEntry entry);

    /**
     * Endpoint #4 — correct one period with {@code $set entries.$[entry].<field>}.
     *
     * <p><b>It returns the MATCHED count, not the modified one.</b> The two happen to agree today,
     * because the same update bumps {@code version} and {@code updatedAt} — so the document is
     * always modified, even by a patch that writes the value a field already holds. <b>That
     * agreement is incidental, and matched is still the right one to read</b>: it answers "was the
     * entry there", which is the question, while modified answers "did anything change", which
     * would turn a perfectly good no-op patch into a 404 the moment those two bumps moved.
     *
     * @param expectedVersion honoured when given and ignored when null, per open item 1: a
     *        targeted write cannot lose somebody else's edit to a <i>different</i> period, so
     *        requiring it would refuse work that never overlapped
     * @param set   field name to new value, applied inside the matched entry
     * @param unset field names to remove from it — how a room or a label is cleared
     * @return the number of documents matched: 1 when the entry was found, 0 otherwise
     */
    long patchEntry(String schoolId, LocalDate date, String entryId, Long expectedVersion,
            Map<String, Object> set, Set<String> unset);

    /**
     * Endpoint #5 — remove one period with a {@code $pull} by {@code _id}.
     *
     * <p><b>Here the modified count is the right signal</b>, unlike #4: a {@code $pull} that
     * removes nothing modifies nothing, so 0 means the entry was not there — which is a 404 and
     * not an idempotent success, because a caller deleting a period that has already gone has a
     * stale screen and should know.
     *
     * @return the number of documents modified: 1 when the period was removed, 0 otherwise
     */
    long pullEntry(String schoolId, LocalDate date, String entryId);
}
