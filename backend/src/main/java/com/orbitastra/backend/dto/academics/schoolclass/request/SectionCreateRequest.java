package com.orbitastra.backend.dto.academics.schoolclass.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * One new section of a class. Endpoint #17.
 *
 * <p><b>The endpoint the student module is waiting for.</b> {@code StudentAcademicRecord} stores
 * {@code sectionNo} as a plain string, so no student can be placed anywhere until a section
 * exists — which is why this is the first thing built in phase 1 after the class itself.
 *
 * <p><b>{@code sectionNo} can never be changed.</b> Eight collections store it as a plain string
 * and none of them references a section by id — a section is <i>embedded</i>, so it has no id to
 * be referenced by. A rename would not fail and would not cascade: every one of those strings
 * would name a section that no longer answers to it, and every row would still look valid.
 *
 * <p><b>It is both the reference and the display value</b>, which is why there is no separate
 * name field and there must not be one. {@code ClassSection} deliberately has no {@code name}.
 */
public record SectionCreateRequest(

        /**
         * The section's identifier, and what a school sees. Example: "A"
         *
         * <p><b>Stored exactly as typed</b> — "A", "Blue", "Alpha" — because it is the display
         * value. No shape is imposed: a school that names its sections by colour is not making a
         * mistake, and deciding otherwise is not this platform's business.
         *
         * <p><b>Uniqueness is checked case-insensitively.</b> "A" and "a" in one class is a
         * typo every time, not two sections, and the two would be indistinguishable on screen.
         * So the check folds case while the stored value keeps it.
         */
        @NotBlank @Size(max = 20) String sectionNo,

        /**
         * The class teacher's {@code Staff.id}, or absent. Example: "67aa15d9dc3f7d0011111111"
         *
         * <p>Checked to exist <b>and</b> to belong to this school. Absent is the ordinary case
         * before staff are onboarded, and a section with no class teacher is a real state rather
         * than an incomplete one.
         */
        @Size(max = 60) String classTeacherDocsId,

        /**
         * How many students the section is planned for, or absent. Example: 40
         *
         * <p><b>Nothing enforces it.</b> This module cannot count students — the count lives in
         * {@code student}, and so does the refusal. It is a plan, not a limit.
         *
         * <p>At least 1: a section nobody can be placed in is not a section. Absent means no
         * plan has been recorded, which is different from a plan of zero.
         */
        @Min(1) Integer capacity) {
}
