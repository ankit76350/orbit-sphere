package com.orbitastra.backend.dto.academics.schoolclass.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.orbitastra.backend.models.academics.structure.embedded.ClassSection;

/**
 * One section, as every endpoint that returns one describes it.
 *
 * <p><b>Its own file because three responses use it</b> — {@link SectionListResponse} for #17 to
 * #21 and #30, and {@link SchoolClassDetailResponse} for #29. It lived inside
 * {@code SectionListResponse} until #29 needed it too; a second copy would have been two shapes
 * for one thing, and the day they drifted a client reading both would have to know which was
 * which. Same reasoning as {@code HolidayView} in {@code core}.
 *
 * <p><b>{@code classTeacherDocsId} is the raw id, not a resolved name.</b> {@code StaffRepository}
 * exists as of #17, so resolving it is now possible — and deliberately not done here. If this
 * response resolved it and #29 did too, there would be two places deciding how a teacher is
 * presented; when a screen needs the name it should read the staff record. <b>Decide once, when
 * something actually needs it, and do it in one place.</b>
 *
 * <p>No {@code sectionNo} normalisation on the way out: it is stored exactly as the school typed
 * it, because it is the display value as well as the reference.
 */
public record SectionView(
        String sectionNo,

        @JsonInclude(JsonInclude.Include.NON_NULL)
        String classTeacherDocsId,

        /** A plan, not a limit. Absent when the school never recorded one. */
        @JsonInclude(JsonInclude.Include.NON_NULL)
        Integer capacity,

        Boolean active) {

    public static SectionView of(ClassSection section) {
        return new SectionView(section.getSectionNo(), section.getClassTeacherDocsId(),
                section.getCapacity(), section.getActive());
    }
}
