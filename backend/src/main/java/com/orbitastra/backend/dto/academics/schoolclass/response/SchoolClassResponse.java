package com.orbitastra.backend.dto.academics.schoolclass.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.orbitastra.backend.models.academics.structure.SchoolClass;

/**
 * A class as it now stands.
 *
 * <p><b>Counts, not the lists themselves.</b> A twelve-class year with four sections and ten
 * subjects each is 168 embedded rows, and returning them from a create — where both are
 * guaranteed empty — would bury the fields that actually changed. The lists have their own reads:
 * #29 for one class in full, and #30 for its sections. There is no read for the subjects alone:
 * #31 was dropped on 2026-09-11 because it was #29 with fields removed.
 *
 * <p><b>{@code affiliationProgrammeDocsId} comes back as the raw id</b>, not resolved to a board
 * name. Resolving it here would mean this response and #29 both deciding how to present it; when
 * a screen needs the name it should read the programme, and that decision belongs in one place
 * rather than two.
 */
public record SchoolClassResponse(
        /** The id every other document stores as {@code classDocsId}, and every URL uses. */
        String schoolClassId,

        String academicYear,
        String name,

        @JsonInclude(JsonInclude.Include.NON_NULL)
        String affiliationProgrammeDocsId,

        int sectionCount,
        int subjectCount,
        Boolean active,

        /**
         * What to do next. A <b>write</b> field — it says what just happened.
         *
         * <p>Left out of the JSON when null, which is what the reads pass. A read changed
         * nothing, so it has nothing to say about what happens next, and a {@code "nextStep":
         * null} on every row of a list is noise a client then has to decide whether to trust.
         */
        @JsonInclude(JsonInclude.Include.NON_NULL)
        String nextStep) {

    /** The same class, for a read. No {@code nextStep}: nothing just happened. */
    public static SchoolClassResponse fromSchoolClass(SchoolClass schoolClass) {
        return fromSchoolClass(schoolClass, null);
    }

    public static SchoolClassResponse fromSchoolClass(SchoolClass schoolClass, String nextStep) {
        return new SchoolClassResponse(
                schoolClass.getId(),
                schoolClass.getAcademicYear(),
                schoolClass.getName(),
                schoolClass.getAffiliationProgrammeDocsId(),
                // Null-safe on both. NOT for the reason this comment used to give: an absent
                // collection reads as an EMPTY LIST, because Spring Data supplies one. It is a
                // field stored as an explicit null that reads null. Measured 2026-09-11.
                schoolClass.getSections() == null ? 0 : schoolClass.getSections().size(),
                schoolClass.getSubjects() == null ? 0 : schoolClass.getSubjects().size(),
                schoolClass.getActive(),
                nextStep);
    }
}
