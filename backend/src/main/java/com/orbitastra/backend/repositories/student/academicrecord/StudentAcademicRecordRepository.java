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
     * <p><b>The sort is the index order, deliberately.</b>
     * {@code school_student_academic_record_history_idx} is
     * {@code {schoolId, studentDocsId, academicYear: -1, effectiveFrom: -1}} — asking for the same
     * order lets Mongo walk the index and skip the sort stage entirely. Reversing either key, or
     * sorting on anything else, turns this into a collection scan with an in-memory sort.
     *
     * <p>{@code effectiveFrom} descending is the tiebreaker within one year, and a year can hold
     * several: #17 closes one record and opens another every time a child changes section, so the
     * most recent placement of a mid-year move comes first.
     */
    List<StudentAcademicRecord> findBySchoolIdAndStudentDocsIdOrderByAcademicYearDescEffectiveFromDesc(
            String schoolId, String studentDocsId);

    /**
     * The same, narrowed to one year.
     *
     * <p><b>Still a list, not an {@code Optional}.</b> One year holds one <i>active</i> record,
     * and the index says so — but it holds as many closed ones as the child had section moves, and
     * this read returns those too. A finder that expected one would throw the first time a child
     * changed class in March.
     *
     * <p>No {@code academicYear} in the sort, because every row has the same one. The index prefix
     * {@code {schoolId, studentDocsId, academicYear}} is still what finds them.
     */
    List<StudentAcademicRecord> findBySchoolIdAndStudentDocsIdAndAcademicYearOrderByEffectiveFromDesc(
            String schoolId, String studentDocsId, String academicYear);
}
