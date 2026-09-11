package com.orbitastra.backend.repositories.academics.schoolclass;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.orbitastra.backend.models.academics.structure.SchoolClass;

public interface SchoolClassRepository extends MongoRepository<SchoolClass, String> {

    /**
     * One class, by its document id, scoped to the school and the year.
     *
     * <p><b>A class is addressed by id, not by a code.</b> Twelve other documents already store
     * {@code classDocsId} and not one stores a class code — {@code sectionNo} and
     * {@code subjectCode} are codes only because they are embedded and have no id to reference.
     *
     * <p><b>All three arguments, even though the id alone is globally unique.</b> Looking up by id
     * only would find another school's class and quietly return it: that is the tenant boundary
     * {@code SchoolBase} exists to enforce. The year is in the query too, so a class id pasted
     * from last year's URL answers 404 rather than editing last year's structure.
     */
    Optional<SchoolClass> findByIdAndSchoolIdAndAcademicYear(
            String id, String schoolId, String academicYear);

    /**
     * Whether that name is taken in that year, for endpoint #12.
     *
     * <p>Checked in the service even though {@code school_year_class_name_uniq} is unique, so the
     * caller gets {@code 409 CLASS_NAME_TAKEN} naming the class rather than a duplicate-key error
     * surfacing as a 500. The index is the guarantee; this is the message.
     */
    boolean existsBySchoolIdAndAcademicYearAndName(
            String schoolId, String academicYear, String name);
}
