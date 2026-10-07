package com.orbitastra.backend.repositories.student.student;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.orbitastra.backend.models.student.Student;

/**
 * Reads and writes for the {@code students} collection.
 *
 * <p>Only what #1, #4, #5 and #6 need so far. #4's and #6's searches are in
 * {@link StudentRepositoryCustom}: every filter on them is optional, so the query has to be built
 * while it runs rather than written into a method name.
 */
public interface StudentRepository
        extends MongoRepository<Student, String>, StudentRepositoryCustom {

    /**
     * One child, for #5.
     *
     * <p><b>The school is in the query, never checked after the read.</b> An id from another
     * school is a real id: looking it up without the school would find it and hand over a child's
     * date of birth, their address and their guardians' phone numbers. A check written after the
     * read is one {@code if} away from being forgotten; this one cannot be.
     */
    Optional<Student> findByIdAndSchoolId(String id, String schoolId);

    /**
     * Has this admission application already produced a child? For {@code crm} #33.
     *
     * <p>The database also stops it with {@code school_admission_application_uniq}, which is
     * unique on {@code {schoolId, admissionApplicationDocsId}} and partial on the field being
     * there. We ask first so the caller is told what happened instead of getting a duplicate key
     * error that the handler turns into a 500.
     */
    Optional<Student> findBySchoolIdAndAdmissionApplicationDocsId(
            String schoolId, String admissionApplicationDocsId);

    /**
     * Every child one guardian is attached to. For #5, which names the other children a contact
     * belongs to so the person reading knows they are looking at a family and not a stranger.
     *
     * <p>{@code school_guardian_students_idx} is keyed {@code {schoolId, guardians.guardianDocsId}}
     * and exists for exactly this.
     */
    List<Student> findBySchoolIdAndGuardiansGuardianDocsId(String schoolId, String guardianDocsId);

    /**
     * How many children one guardian is attached to. For #8.
     *
     * <p><b>A count rather than the documents</b>, because that is the whole question: correcting
     * a guardian changes them for every child linked to them, and the answer says how many. Reading
     * four whole students to report the number 4 would be carrying their dates of birth across to
     * print a digit.
     *
     * <p>Same index as the read above — {@code school_guardian_students_idx}.
     */
    long countBySchoolIdAndGuardiansGuardianDocsId(String schoolId, String guardianDocsId);
}
