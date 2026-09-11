package com.orbitastra.backend.dto.academics.schoolclass.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * A new class for one academic year. Endpoint #12.
 *
 * <p><b>The academic year is not in the body.</b> It comes from the URL —
 * {@code POST /schools/current/academic-years/2026-2027/classes} — because a class belongs to one
 * year. A year in the body would be a second place to say the same thing, and the two could
 * disagree.
 *
 * <p><b>There is no code field, and the name is editable.</b> A class is addressed and referenced
 * by its document id: twelve other documents store {@code classDocsId} and not one stores a class
 * code. So nothing joins on the name, and renaming it breaks nothing — it only has to stay unique
 * inside the year, which {@code school_year_class_name_uniq} enforces.
 *
 * <p><b>Sections and subjects are not accepted here.</b> A class is created empty and they are
 * added through their own endpoints — #17 for a section, #22 for a subject. This is the shape the
 * codebase already uses twice: an academic year is created with no holidays, a plan with no
 * features. A create that can fail on either a bad class name or a stray subject leaves the
 * caller working out which, and a half-written subject list is worse than an empty one.
 */
public record SchoolClassCreateRequest(

        /**
         * What a school calls it. Editable afterwards, unique within the year. Example: "Grade 7"
         *
         * <p>No shape is enforced. A school may use "Grade 7", "VII", "Class 7" or "Std VII",
         * and imposing one would be this platform deciding something that is not its business.
         */
        @NotBlank @Size(max = 120) String name,

        /**
         * Sort order for the UI, or absent. Example: 7
         *
         * <p>Not unique and not required: "Nursery, LKG, UKG, 1, 2, 3" is the order a school
         * reads, and it is neither alphabetical nor derivable from the name. Absent sorts last.
         */
        @Min(0) Integer displayOrder,

        /**
         * Which board programme this class runs under, or absent. Example:
         * "67aa15d9dc3f7d0011111111"
         *
         * <p>Checked to exist and to belong to this school. Absent is the ordinary case for a
         * school that has not recorded its affiliation yet.
         */
        @Size(max = 60) String affiliationProgrammeDocsId) {
}
