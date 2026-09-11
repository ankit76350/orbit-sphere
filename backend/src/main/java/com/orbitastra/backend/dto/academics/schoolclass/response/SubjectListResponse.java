package com.orbitastra.backend.dto.academics.schoolclass.response;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.orbitastra.backend.models.academics.structure.SchoolClass;
import com.orbitastra.backend.models.academics.structure.embedded.ClassSubject;

/**
 * A class's subject assignments. Shared by every endpoint that touches the list.
 *
 * <p>One record for #22 to #27 and #31, the same arrangement {@link SectionListResponse} uses for
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
