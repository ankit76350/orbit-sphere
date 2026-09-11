package com.orbitastra.backend.dto.academics.schoolclass.response;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.orbitastra.backend.models.academics.structure.SchoolClass;
import com.orbitastra.backend.models.academics.structure.embedded.ClassSection;
import com.orbitastra.backend.models.academics.structure.embedded.ClassSubject;

/**
 * One class in full — its sections and its subjects. Endpoint #29.
 *
 * <p><b>One document, one query, no joins</b>, which is the entire reason sections and subjects
 * are embedded rather than collections of their own. Were they separate this response would be
 * three reads, and the list endpoint (#28) would be three per row.
 *
 * <p><b>Why this is not {@link SchoolClassResponse}.</b> That one carries {@code sectionCount}
 * and {@code subjectCount} and no lists, because #28 returns a page of rows and a twelve-class
 * year with four sections and ten subjects each is 168 embedded rows nobody reads. Here the
 * caller asked for one class, so the lists are the point. Two records rather than one with the
 * lists sometimes populated — a field that is present on some responses and absent on others is
 * a field every client has to guard.
 *
 * <p><b>Four counts, not two.</b> Active and total are different questions for both lists: a
 * retired section keeps its {@code sectionNo} and is still referenced by whatever was recorded
 * against it, so it has to appear — but it is not one a student can be placed in. A class with
 * four sections, none active, would otherwise look ready.
 *
 * <p><b>Nothing is resolved to a name.</b> Class teachers, subject teachers and grading schemes
 * all come back as raw ids. See {@link SectionView} for why that is one decision made in one
 * place rather than repeated here.
 *
 * <p>Both lists come back in the order they were added, which is how they are stored. There is
 * nothing better: {@code sectionNo} is free text, so alphabetical would order "Blue" before
 * "Red" and mean nothing, and a subject's position in the list carries no meaning either.
 */
public record SchoolClassDetailResponse(
        String schoolClassId,
        String academicYear,
        String name,

        @JsonInclude(JsonInclude.Include.NON_NULL)
        String affiliationProgrammeDocsId,

        Boolean active,

        int sectionCount,
        int activeSectionCount,
        List<SectionView> sections,

        int subjectCount,
        int activeSubjectCount,
        List<SubjectView> subjects) {

    public static SchoolClassDetailResponse fromSchoolClass(SchoolClass schoolClass) {
        // Null-safe against a field stored as an explicit null. An ABSENT one reads as an empty
        // list, Spring Data supplying one for a missing collection property — measured
        // 2026-09-11, and narrower than the "documents written before the field existed" reason
        // three comments in this repository used to give.
        List<ClassSection> sections = schoolClass.getSections() == null
                ? List.of()
                : schoolClass.getSections();
        List<ClassSubject> subjects = schoolClass.getSubjects() == null
                ? List.of()
                : schoolClass.getSubjects();

        return new SchoolClassDetailResponse(
                schoolClass.getId(),
                schoolClass.getAcademicYear(),
                schoolClass.getName(),
                schoolClass.getAffiliationProgrammeDocsId(),
                schoolClass.getActive(),
                sections.size(),
                (int) sections.stream().filter(s -> Boolean.TRUE.equals(s.getActive())).count(),
                sections.stream().map(SectionView::of).toList(),
                subjects.size(),
                (int) subjects.stream().filter(s -> Boolean.TRUE.equals(s.getActive())).count(),
                subjects.stream().map(SubjectView::of).toList());
    }
}
