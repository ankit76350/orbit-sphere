package com.orbitastra.backend.repositories.student.academicrecord;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.orbitastra.backend.models.student.StudentAcademicRecord;
import com.orbitastra.backend.models.student.enums.AcademicRecordStatus;

/**
 * Where a child sits, year by year.
 *
 * <p>Only what #14 needs so far. The roster (#21) and the history (#20) will want reads this does
 * not have — they are written when those endpoints are.
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
     * Whether a roll number is already taken in one section of one year.
     *
     * <p>The same shape as {@code school_year_class_section_active_roll_uniq}, and for the same
     * reason as above: the index refuses the collision either way, and this turns it into a
     * refusal that says which section the number is already used in.
     *
     * <p><b>{@code status} is part of it.</b> A roll number freed by a child who left is available
     * again — the index is partial on {@code ACTIVE}, so a closed record holding "12" does not
     * stop the next child taking it.
     */
    boolean existsBySchoolIdAndAcademicYearAndClassDocsIdAndSectionNoAndRollNoAndStatus(
            String schoolId, String academicYear, String classDocsId, String sectionNo,
            String rollNo, AcademicRecordStatus status);
}
