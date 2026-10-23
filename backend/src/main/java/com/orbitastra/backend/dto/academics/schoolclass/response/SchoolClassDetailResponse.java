package com.orbitastra.backend.dto.academics.schoolclass.response;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.orbitastra.backend.models.academics.structure.SchoolClass;
import com.orbitastra.backend.models.academics.structure.embedded.ClassSection;
import com.orbitastra.backend.models.academics.structure.embedded.ClassSubject;

/**
 * One class — its own facts, and how much is inside it. Endpoint #29.
 *
 * <p><b>The rows are not here, and that is the point.</b> Sections come from #30 and #37,
 * subjects from #31. This response used to carry both lists; they were removed on 2026-09-11,
 * once each had an endpoint that owned it. Three responses returning the same embedded rows is
 * three places to keep in step, and the first client to read a section from here would be
 * depending on a shape this endpoint has no claim on.
 *
 * <p><b>The counts stayed.</b> "3 sections, 3 subjects" is what a class row and a page header
 * show, and making a caller fetch two lists to count them is the call this endpoint exists to
 * save. They are also the one thing here that cannot be got from the class document's own
 * fields — everything else is a column, these are derived.
 *
 * <p><b>Four counts, not two.</b> Active and total are different questions for both: a retired
 * section keeps its {@code sectionNo} and is still referenced by whatever was recorded against
 * it, so it still counts — but it is not one a student can be placed in. A class with four
 * sections, none active, would otherwise look ready.
 *
 * <p><b>Why this is still not {@link SchoolClassResponse}.</b> The two now carry nearly the same
 * fields, and the difference is {@code affiliationProgrammeDocsId} — a value #28's page of rows
 * has no use for. Kept separate rather than merged because #28 returns many and this returns one,
 * and a response type shared by both is one that grows a field for whichever caller needs it next.
 *
 * <p><b>One document, one query, no joins.</b> Still true, and still the reason sections and
 * subjects are embedded rather than collections of their own — it is what lets #30, #31 and #37
 * each be a single {@code findById} rather than a join.
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

        int subjectCount,
        int activeSubjectCount) {

    /**
     * <b>The counts are here; the rows are not.</b> Trimmed 2026-09-11, once #30, #31 and #37
     * existed: three endpoints returning the same embedded rows is three places to keep in step,
     * and a client reading them from here would be depending on a shape this response does not
     * own. What is left is what only this endpoint answers — the class's own facts, and how much
     * is inside it.
     *
     * <p><b>The counts stay</b> because "3 sections, 3 subjects" is what a class row and a page
     * header show, and fetching two lists to count them is the call this endpoint exists to save.
     */
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
                subjects.size(),
                (int) subjects.stream().filter(s -> Boolean.TRUE.equals(s.getActive())).count());
    }
}
