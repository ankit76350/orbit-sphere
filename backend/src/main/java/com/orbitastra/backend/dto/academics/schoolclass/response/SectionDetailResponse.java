package com.orbitastra.backend.dto.academics.schoolclass.response;

import com.orbitastra.backend.models.academics.structure.SchoolClass;
import com.orbitastra.backend.models.academics.structure.embedded.ClassSection;

import java.util.List;

/**
 * One section of one class. Endpoint #37.
 *
 * <p><b>The one response in this package that is not a list.</b> #30 answers "which sections does
 * this class have" and this answers "this section" — the difference between a dropdown and a page.
 * Without it a caller wanting one section reads all of them and picks in its own code, which is
 * the same shape of duplication #31 was built to end for subjects.
 *
 * <p><b>It carries the class around the section</b> — {@code className} and {@code academicYear} —
 * because a section is embedded and has no identity away from its class. A response holding only
 * {@code sectionNo} would name something that means nothing on its own.
 *
 * <p><b>The counts describe the class, not this section</b>, the same rule
 * {@link SectionListResponse#forRead} and {@link SubjectListResponse#forRead} follow. "One of
 * three" is what a page shows beside a section's name.
 *
 * <p><b>The subjects are not here.</b> That is #31 with {@code ?sectionNo=}, which applies the
 * class-wide union — a rule this response has no business restating.
 */
public record SectionDetailResponse(
        String schoolClassId,
        String className,
        String academicYear,
        int sectionCount,
        int activeCount,
        SectionView section) {

    public static SectionDetailResponse of(SchoolClass schoolClass, ClassSection section) {
        // Null-safe against a field stored as an explicit null. An ABSENT one reads as an empty
        // list — Spring Data supplies one for a missing collection property, measured 2026-09-11.
        List<ClassSection> sections = schoolClass.getSections() == null
                ? List.of()
                : schoolClass.getSections();

        return new SectionDetailResponse(
                schoolClass.getId(),
                schoolClass.getName(),
                schoolClass.getAcademicYear(),
                sections.size(),
                (int) sections.stream().filter(s -> Boolean.TRUE.equals(s.getActive())).count(),
                SectionView.of(section));
    }
}
