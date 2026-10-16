package com.orbitastra.backend.repositories.academics.exam;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.orbitastra.backend.models.academics.examination.Exam;

/**
 * The exams of a school, one per exam event per academic year.
 *
 * <p><b>Every query carries {@code schoolId}.</b> An exam's name is unique only inside one
 * school's year, so a lookup without the school would find another school's exam.
 */
public interface ExamRepository extends MongoRepository<Exam, String> {

    /**
     * An exam of this year with the same name, ignoring upper and lower case — so "Half Yearly"
     * and "half yearly" count as the same name. Cancelled exams are included: they keep their name.
     *
     * <p>{@code school_year_exam_name_uniq} only catches an exact match, so this check is the
     * one that stops "Half Yearly" and "half yearly" from both being saved.
     */
    Optional<Exam> findFirstBySchoolIdAndAcademicYearAndNameIgnoreCase(String schoolId,
            String academicYear, String name);
}
