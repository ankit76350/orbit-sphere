package com.orbitastra.backend.repositories.academics.schoolclass;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.orbitastra.backend.models.academics.structure.SchoolClass;

public interface SchoolClassRepository
        extends MongoRepository<SchoolClass, String>, SchoolClassRepositoryCustom {

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
     * Several classes of one year, by their ids — for a read that holds a handful of them.
     *
     * <p><b>One query, not one per id.</b> A school day names every class that has anything
     * scheduled, so timetable #7 would otherwise make twelve round trips to put a name beside
     * twelve class ids. This is the same reason {@code findBySchoolIdAndIdIn} exists on
     * {@code StaffRepository}.
     *
     * <p><b>The year is in the query, so a class id from another year simply does not come back</b>
     * and the name beside that period stays absent. That is the right answer: the period does name
     * a class this year does not have, and inventing a name for it would hide a real problem.
     *
     * <p>Returns what it finds. A caller matching ids to names has to cope with a missing one
     * anyway — a class deleted after a day was written is a normal thing to read back.
     */
    List<SchoolClass> findBySchoolIdAndAcademicYearAndIdIn(
            String schoolId, String academicYear, Collection<String> ids);

    /**
     * Whether that name is taken in that year, for endpoint #12.
     *
     * <p>Checked in the service even though {@code school_year_class_name_uniq} is unique, so the
     * caller gets {@code 409 CLASS_NAME_TAKEN} naming the class rather than a duplicate-key error
     * surfacing as a 500. The index is the guarantee; this is the message.
     */
    boolean existsBySchoolIdAndAcademicYearAndName(
            String schoolId, String academicYear, String name);

    /**
     * The class holding that name in that year, if any — for the rename in #13.
     *
     * <p><b>Why not the {@code exists} check above.</b> A rename has to allow a class to keep its
     * own name: {@code exists} answers true for the row being edited, so it would refuse
     * {@code PATCH {"name": "Grade 7"}} on the class already called Grade 7, and refuse every
     * request that sends the name unchanged alongside a new sort order. This returns the holder
     * so the caller can compare ids.
     *
     * <p>#12 keeps the cheaper {@code exists} because a create has no row to be its own conflict.
     */
    /**
     * Whether any subject of any class in this school is graded by one scheme.
     *
     * <p><b>The reference check behind #3 of the grading module</b>, and the only one of the three
     * it needs that can be made at all: {@code exams} and {@code report_cards} also store
     * {@code gradingSchemeDocsId} and neither collection has a repository, because neither has an
     * endpoint to write a row. So a pass here means "nothing REACHABLE uses it" and the refusal
     * message says so rather than claiming more.
     *
     * <p><b>It traverses an embedded list</b> — {@code subjects[].gradingSchemeDocsId} — which
     * Mongo matches without an index today. A year holds tens of classes, so it is cheap now and
     * wants an index before anything calls it in a loop.
     *
     * <p>Scoped by {@code schoolId}: another school using the same scheme id is impossible, but
     * the query would scan their documents to prove it.
     */
    boolean existsBySchoolIdAndSubjectsGradingSchemeDocsId(String schoolId,
            String gradingSchemeDocsId);

    Optional<SchoolClass> findBySchoolIdAndAcademicYearAndName(
            String schoolId, String academicYear, String name);
}
