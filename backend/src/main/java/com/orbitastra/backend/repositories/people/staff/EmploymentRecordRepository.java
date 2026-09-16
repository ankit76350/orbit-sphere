package com.orbitastra.backend.repositories.people.staff;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.orbitastra.backend.models.people.staff.EmploymentRecord;

/**
 * The collection that finally joins a person to a seat.
 *
 * <p><b>Created 2026-09-15 with #16.</b> Until then nothing in this product wrote an employment
 * record, which is why #7 has no employment filters, #8 returns no employment block, and #14 owes
 * two checks that could only ever have counted zero.
 */
public interface EmploymentRecordRepository
        extends MongoRepository<EmploymentRecord, String>, EmploymentRecordRepositoryCustom {

    /**
     * The one record that says what somebody does here now.
     *
     * <p><b>At most one can exist</b> — {@code school_staff_current_employment_uniq} is unique and
     * partial on {@code current: true}, so the database refuses a second. This read is what #16
     * closes before opening a new one, and what #8 folds into a person.
     */
    Optional<EmploymentRecord> findBySchoolIdAndStaffDocsIdAndCurrentIsTrue(String schoolId,
            String staffDocsId);

    /**
     * Whether this person already has a record starting on that day.
     *
     * <p><b>The check in front of {@code school_staff_employment_start_uniq}</b>, which is unique
     * on {@code {schoolId, staffDocsId, effectiveFrom}}. It covers closed records too: somebody
     * rehired on the exact day an old contract began is a real mistake to catch, and the index
     * catches it either way — this turns a duplicate-key 500 into a readable 409.
     */
    boolean existsBySchoolIdAndStaffDocsIdAndEffectiveFrom(String schoolId, String staffDocsId,
            LocalDate effectiveFrom);

    /**
     * How many people currently hold one seat.
     *
     * <p><b>Counted, never stored.</b> A stored {@code filledHeadcount} on {@code Position} drifts
     * the first time a writer forgets it — the objection that also keeps a weight total off
     * {@code AcademicTerm}. #16 compares this against {@code approvedHeadcount} and <b>warns</b>
     * rather than refusing: a school hiring a twelfth teacher into eleven approved seats is a
     * budget conversation, and refusing it stops the system recording something that has already
     * happened.
     */
    long countBySchoolIdAndPositionDocsIdAndCurrentIsTrue(String schoolId, String positionDocsId);

    /**
     * One record of this school, by its own id.
     *
     * <p><b>Scoped by {@code schoolId}, never by id alone.</b> #18 is addressed by the record
     * rather than the person — a person has several and the URL has to say which — so this is the
     * only place the tenant can be checked on that path.
     */
    Optional<EmploymentRecord> findByIdAndSchoolId(String id, String schoolId);

    /**
     * One person's whole employment history, oldest first.
     *
     * <p><b>Read by #18 to check a corrected date against its neighbours.</b> Moving a record's
     * {@code effectiveFrom} can push it onto the one before, and the pair of unique indexes does
     * not stop that: {@code school_staff_employment_start_uniq} only forbids two records
     * <i>starting</i> on the same day, and nothing at all compares a start to the previous end.
     *
     * <p>Nobody has a hundred of these, so the whole list is cheaper than working out which two
     * rows to fetch.
     */
    List<EmploymentRecord> findBySchoolIdAndStaffDocsIdOrderByEffectiveFromAsc(String schoolId,
            String staffDocsId);

    /**
     * Everybody who currently holds one seat, longest-serving first.
     *
     * <p><b>The list behind #53, and the reason that endpoint does not also count.</b> The count
     * it reports is this list's size: two reads of the same collection a moment apart can
     * disagree — somebody is hired between them — and a page showing "3 filled" above two names
     * is a bug report nobody can reproduce.
     *
     * <p><b>Ordered by {@code effectiveFrom} ascending</b>, so the person who has been in the
     * seat longest is at the top. The opposite of #19's order, and deliberately: a history is
     * read newest-first because the current row is the interesting one, and a roster is read
     * oldest-first because seniority is what distinguishes otherwise identical rows.
     *
     * <p><b>Current only, never the seat's history.</b> A closed record names somebody who
     * <i>used to</i> hold this, and "who is in this seat" is the question.
     */
    List<EmploymentRecord> findBySchoolIdAndPositionDocsIdAndCurrentIsTrueOrderByEffectiveFromAsc(
            String schoolId, String positionDocsId);

    /**
     * The same history, newest first — what #19 answers with.
     *
     * <p><b>A second method rather than reversing the first in Java.</b> The order is what the
     * endpoint is for, and a sort the database already knows how to do is not worth moving into
     * the service; {@code school_employment_status_idx} is on
     * {@code {schoolId, status, effectiveFrom: -1}}, so descending by that field is the direction
     * this collection is already built to serve.
     *
     * <p><b>Not paged, deliberately.</b> Nobody has a hundred employment records, and a cursor on
     * a five-row list is machinery nobody uses — the argument the term list lost and this one
     * wins.
     */
    List<EmploymentRecord> findBySchoolIdAndStaffDocsIdOrderByEffectiveFromDesc(String schoolId,
            String staffDocsId);
}
