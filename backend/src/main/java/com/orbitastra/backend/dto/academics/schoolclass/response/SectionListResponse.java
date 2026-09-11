package com.orbitastra.backend.dto.academics.schoolclass.response;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.orbitastra.backend.models.academics.structure.SchoolClass;
import com.orbitastra.backend.models.academics.structure.embedded.ClassSection;

/**
 * A class's sections. Shared by every endpoint that touches the list.
 *
 * <p>One record for all of #17 to #21 and #30 rather than one per verb, the same arrangement
 * {@code HolidayCalendarResponse} uses for the calendar: every operation on an embedded list
 * leaves the caller wanting the same thing — what does the list look like now.
 * {@code changeSummary} is what distinguishes them, so a {@code 200} that changed nothing is
 * still legible.
 *
 * <p><b>The class is named as well as the sections.</b> A section has no identity of its own
 * outside the class that holds it — it is embedded, with no id and no {@code schoolId} — so a
 * response carrying only {@code sectionNo} values would be ambiguous the moment two classes both
 * have an "A".
 *
 * <p><b>Two counts, because active and total are different questions.</b> A retired section still
 * holds its {@code sectionNo} and is still referenced by whatever was recorded against it, so it
 * has to appear in the list; {@code activeCount} is the number a school can actually place a
 * student in. Reporting only the total would make a class look ready when none of its sections
 * is usable.
 *
 * <p>Sections come back in the order they were added, which is how they are stored. There is
 * nothing better to sort them by: {@code sectionNo} is free text, so alphabetical would put
 * "Blue" before "Red" and mean nothing.
 *
 * <p><b>{@code SectionView} used to be nested in here.</b> It moved to its own file when #29
 * needed it too — a second copy would have been two shapes for one thing.
 */
public record SectionListResponse(
        String schoolClassId,
        String className,
        String academicYear,
        int sectionCount,
        int activeCount,
        List<SectionView> sections,

        /**
         * What the call just did. A <b>write</b> field.
         *
         * <p>Left out of the JSON when null, which is what the read passes: a read changed
         * nothing and has nothing to summarise.
         */
        @JsonInclude(JsonInclude.Include.NON_NULL)
        String changeSummary) {

    /**
     * The list for a read, optionally narrowed to active sections or to retired ones. #30.
     *
     * <p><b>Its own name rather than another {@code fromSchoolClass} overload.</b> Two overloads
     * differing only in a nullable second argument are ambiguous to the compiler —
     * {@code fromSchoolClass(cls, null)} matched both — and they would have been ambiguous to a
     * reader for the same reason. The name now says which half of the API is calling: writes
     * pass a summary, reads pass a filter.
     *
     * <p><b>The counts describe the WHOLE class, not the filtered view</b>, and that is
     * deliberate: {@code sectionCount} answers "how many does this class have", which does not
     * change because a caller asked to see some of them. A filtered count would make
     * {@code ?active=true} on a class with two retired sections report two sections and two
     * active — a lie in both halves.
     *
     * <p>No {@code changeSummary}: a read changed nothing.
     *
     * @param active {@code null} for every section, which is not the same as {@code false}
     */
    public static SectionListResponse forRead(SchoolClass schoolClass, Boolean active) {
        SectionListResponse whole = fromSchoolClass(schoolClass, null);

        if (active == null) {
            return whole;
        }

        return new SectionListResponse(
                whole.schoolClassId(), whole.className(), whole.academicYear(),
                whole.sectionCount(), whole.activeCount(),
                whole.sections().stream()
                        .filter(view -> active.equals(Boolean.TRUE.equals(view.active())))
                        .toList(),
                null);
    }

    public static SectionListResponse fromSchoolClass(SchoolClass schoolClass,
            String changeSummary) {

        // Null-safe against a field stored as an explicit null. An ABSENT one reads as an empty
        // list - Spring Data supplies one for a missing collection property - so this guard is
        // narrower than it looks. Measured 2026-09-11.
        List<ClassSection> sections = schoolClass.getSections() == null
                ? List.of()
                : schoolClass.getSections();

        return new SectionListResponse(
                schoolClass.getId(),
                schoolClass.getName(),
                schoolClass.getAcademicYear(),
                sections.size(),
                (int) sections.stream().filter(s -> Boolean.TRUE.equals(s.getActive())).count(),
                sections.stream().map(SectionView::of).toList(),
                changeSummary);
    }
}
