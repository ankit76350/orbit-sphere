package com.orbitastra.backend.dto.academics.schoolclass.response;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.orbitastra.backend.models.academics.structure.SchoolClass;
import com.orbitastra.backend.models.academics.structure.embedded.ClassSubject;

/**
 * A class's subject assignments. Shared by every endpoint that touches the list.
 *
 * <p>One record for #22 to #27, the same arrangement {@link SectionListResponse} uses for
 * sections and {@code HolidayCalendarResponse} for the calendar: every operation on an embedded
 * list leaves the caller wanting the same thing — what does the list look like now.
 *
 * <p><b>The class is named as well as the subjects.</b> A subject assignment has no identity
 * outside the class holding it, so a response carrying only codes would be ambiguous the moment
 * two classes both teach MATHEMATICS.
 *
 * <p><b>Two counts, because active and total are different questions.</b> A retired assignment
 * still holds its {@code subjectCode} and is still referenced by whatever was recorded against
 * it, so it has to appear; {@code activeCount} is what is actually taught.
 *
 * <p>Rows come back in the order they were added, which is how they are stored. Alphabetical by
 * code would be no more meaningful and would scatter a subject's per-section rows.
 */
public record SubjectListResponse(
        String schoolClassId,
        String className,
        String academicYear,
        int subjectCount,
        int activeCount,
        List<SubjectView> subjects,

        /** What the call just did. A <b>write</b> field, absent on a read. */
        @JsonInclude(JsonInclude.Include.NON_NULL)
        String changeSummary) {

    /** The same list, for a read. No {@code changeSummary}: nothing just happened. */
    public static SubjectListResponse fromSchoolClass(SchoolClass schoolClass) {
        return fromSchoolClass(schoolClass, null);
    }

    /**
     * The read shape for #31 — the whole list, or what one section studies.
     *
     * <p><b>A section studies its own rows PLUS the class-wide ones.</b> A row with no
     * {@code sectionNo} applies to every section, so matching {@code sectionNo} strictly would
     * hide most of what a section is taught — in a class where only languages are split by
     * section, it would hide everything else. The caller can still tell the two apart: a
     * class-wide row has no {@code sectionNo} in the JSON at all.
     *
     * <p><b>This reads {@code sectionNo} differently from #24</b>, where the same parameter names
     * one row by its key. Here it names an audience. Same word, two jobs, and the only reason it
     * is spelled the same is that "which section" is the question in both cases.
     *
     * <p><b>The counts describe the whole class, not the filtered view</b> — the same rule
     * {@link SectionListResponse#forRead} follows. "How many subjects does this class teach" is
     * not "how many did you ask to see", and a screen showing 3 of 11 needs both numbers.
     *
     * @param sectionNo the section to answer for, already validated to exist, or null for all
     */
    public static SubjectListResponse forRead(SchoolClass schoolClass, String sectionNo) {
        SubjectListResponse whole = fromSchoolClass(schoolClass, null);

        if (sectionNo == null) {
            return whole;
        }

        return new SubjectListResponse(
                whole.schoolClassId(), whole.className(), whole.academicYear(),
                whole.subjectCount(), whole.activeCount(),
                whole.subjects().stream()
                        // equalsIgnoreCase is a BACKSTOP, not the case handling. The service has
                        // already resolved the caller's spelling to the class's own, so by the
                        // time this runs the two match exactly - mutating this to equals() alone
                        // breaks nothing, measured 2026-09-11. It is kept for a caller that does
                        // not go through that resolution, and breaking BOTH layers does fail four
                        // assertions. Do not remove it as dead: it is the half that still works
                        // when the other is wrong.
                        .filter(view -> view.sectionNo() == null
                                || view.sectionNo().equalsIgnoreCase(sectionNo))
                        .toList(),
                null);
    }

    public static SubjectListResponse fromSchoolClass(SchoolClass schoolClass,
            String changeSummary) {

        // Null-safe against a field stored as an explicit null. An ABSENT one reads as an empty
        // list — Spring Data supplies one for a missing collection property, measured 2026-09-11.
        List<ClassSubject> subjects = schoolClass.getSubjects() == null
                ? List.of()
                : schoolClass.getSubjects();

        return new SubjectListResponse(
                schoolClass.getId(),
                schoolClass.getName(),
                schoolClass.getAcademicYear(),
                subjects.size(),
                (int) subjects.stream().filter(s -> Boolean.TRUE.equals(s.getActive())).count(),
                subjects.stream().map(SubjectView::of).toList(),
                changeSummary);
    }
}
