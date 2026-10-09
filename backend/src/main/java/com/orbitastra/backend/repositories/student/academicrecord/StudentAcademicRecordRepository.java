package com.orbitastra.backend.repositories.student.academicrecord;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.orbitastra.backend.models.student.StudentAcademicRecord;
import com.orbitastra.backend.models.student.enums.AcademicRecordStatus;

/**
 * Where a child sits, year by year.
 *
 * <p>Only what #14 and #20 need so far. The roster (#21) and the strength table (#22) will want
 * reads this does not have — they are written when those endpoints are.
 */
public interface StudentAcademicRecordRepository
        extends MongoRepository<StudentAcademicRecord, String> {

    /**
     * The child's open record for one year, if they have one.
     *
     * <p><b>This is what {@code STUDENT_ALREADY_PLACED} is decided on</b>, and it looks for
     * exactly what {@code school_year_student_active_academic_record_uniq} forbids a second of:
     * one {@code ACTIVE} record per student per year.
     *
     * <p><b>The index is the real enforcement, not this.</b> Two requests arriving together both
     * read nothing here and both go on to insert; the second gets a duplicate-key error from
     * Mongo. This exists so the ordinary case — one caller, one placement already made — answers
     * {@code 409} with a sentence naming the class the child is already in, rather than a 500.
     */
    Optional<StudentAcademicRecord> findBySchoolIdAndAcademicYearAndStudentDocsIdAndStatus(
            String schoolId, String academicYear, String studentDocsId,
            AcademicRecordStatus status);

    /**
     * The record already holding a roll number in one section of one year, if any.
     *
     * <p>The same shape as {@code school_year_class_section_active_roll_uniq}, and for the same
     * reason as above: the index refuses the collision either way, and this turns it into a
     * refusal that says where the number is already used.
     *
     * <p><b>It returns the record rather than a boolean</b>, because the refusal names the child
     * holding the number — and the record is what carries their id. <i>"Roll 1 is taken"</i> sends
     * somebody down a class list looking; <i>"roll 1 is taken by ANKIT KUMAR"</i> is something
     * they can act on, by picking another number or by correcting the one that is wrong.
     *
     * <p><b>{@code status} is part of it.</b> A roll number freed by a child who left is available
     * again — the index is partial on {@code ACTIVE}, so a closed record holding "12" does not
     * stop the next child taking it.
     */
    Optional<StudentAcademicRecord>
            findBySchoolIdAndAcademicYearAndClassDocsIdAndSectionNoAndRollNoAndStatus(
                    String schoolId, String academicYear, String classDocsId, String sectionNo,
                    String rollNo, AcademicRecordStatus status);

    /**
     * A child's whole history, newest year first.
     *
     * <p><b>Terminal records included</b>, which is the point: the question is where this child
     * has been, and a closed record is most of the answer. Only the reads that ask "where are they
     * now" filter on {@code ACTIVE}.
     *
     * <p><b>The index finds them in this order.</b>
     * {@code school_student_academic_record_history_idx} is
     * {@code {schoolId, studentDocsId, academicYear: -1, effectiveFrom: -1}}, so the first two
     * keys are walked rather than sorted.
     *
     * <p>{@code effectiveFrom} descending is the tiebreaker within one year, and a year can hold
     * several: #17 closes one record and opens another every time a child changes section.
     *
     * <p><b>{@code id} descending is the tiebreaker under that, and it was added because the pair
     * above is not enough.</b> A child transferred on the day they were placed has two records sharing
     * a year <i>and</i> an {@code effectiveFrom} — measured 2026-10-09, and the tie listed the
     * closed record above the open one, which reads as though the child is still in the section
     * they left. An ObjectId carries its creation time, so descending puts the newer record first.
     *
     * <p><b>That last key costs a sort stage</b>, because it is not in the index. The trade is
     * worth it here and would not be on the roster: this result set is one child's history, a
     * handful of rows, where #21's is a whole section.
     */
    List<StudentAcademicRecord> findBySchoolIdAndStudentDocsIdOrderByAcademicYearDescEffectiveFromDescIdDesc(
            String schoolId, String studentDocsId);

    /**
     * The same, narrowed to one year.
     *
     * <p><b>Still a list, not an {@code Optional}.</b> One year holds one <i>active</i> record,
     * and the index says so — but it holds as many closed ones as the child had section transfers, and
     * this read returns those too. A finder that expected one would throw the first time a child
     * changed class in March.
     *
     * <p>No {@code academicYear} in the sort, because every row has the same one. The index prefix
     * {@code {schoolId, studentDocsId, academicYear}} is still what finds them.
     *
     * <p><b>{@code id} descending breaks the tie</b> between records that start on the same day —
     * the same-day transfer described above, which is the common case within a single year.
     */
    List<StudentAcademicRecord> findBySchoolIdAndStudentDocsIdAndAcademicYearOrderByEffectiveFromDescIdDesc(
            String schoolId, String studentDocsId, String academicYear);
}
