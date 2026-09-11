package com.orbitastra.backend.dto.academics.schoolclass.request;

import java.util.List;

import com.orbitastra.backend.models.academics.enums.SubjectType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * One subject assigned to a class, or to one section of it. Endpoint #22.
 *
 * <p><b>The row key is the pair {@code (subjectCode, sectionNo)}</b>, not the code alone. A
 * class-wide assignment has no {@code sectionNo}; a per-section one names it.
 *
 * <h2>Class-wide or per-section — never both for one subject</h2>
 *
 * <p><b>Settled 2026-09-11, and the plan deliberately left it open.</b> The model README says a
 * class-wide subject has a null {@code sectionNo}, and that when sections have different teachers
 * you "repeat the same {@code subjectCode} with <i>each</i> section code" — each section, not one
 * section beside a class-wide row.
 *
 * <p>So a subject in a class is <b>either</b> class-wide <b>or</b> listed per section, and this
 * endpoint refuses the mixture. The reason is what reads the list: a section studies its own rows
 * <i>plus</i> the class's, so MATHEMATICS class-wide beside MATHEMATICS for section A would give
 * section A the subject twice, and nothing defines which row's teacher or grading scheme wins.
 *
 * <p>The alternative — let a per-section row override a class-wide one — is a real design and is
 * <b>not</b> what this does. It would put a precedence rule in every consumer: mark entry, the
 * timetable, attendance. One of them forgetting it is a silently double-counted subject. If it is
 * ever wanted it has to arrive with that rule written down, not by relaxing this check.
 *
 * <p><b>{@code subjectCode} can never be changed.</b> Seven collections store it as a plain
 * string and a subject assignment is embedded, so it has no id for them to reference instead.
 */
public record SubjectCreateRequest(

        /**
         * The stable reference, uppercased on the way in. Example: "MATHEMATICS"
         *
         * <p><b>Normalised, unlike {@code sectionNo}.</b> A section number is the display value
         * as well as the reference, so it is stored as typed; a subject has {@code name} for
         * display, which leaves the code free to be a code. "maths-2" becomes {@code MATHS_2}.
         */
        @NotBlank @Size(max = 40) String subjectCode,

        /** What a school reads. Example: "Mathematics" */
        @NotBlank @Size(max = 120) String name,

        /** An abbreviation for a timetable cell or a report-card column. Example: "Maths" */
        @Size(max = 40) String shortName,

        /**
         * Example: CORE
         *
         * <p>{@code ELECTIVE} is the one with a consequence: nothing records which students chose
         * it, so a register built from this list will include students who do not sit it. See the
         * upstream gap in this module's README.
         */
        @NotNull SubjectType subjectType,

        /**
         * One section of this class, or absent for every section. Example: "A"
         *
         * <p>Absent is the ordinary case. A value must be a section the class actually has —
         * assigning a subject to a section that does not exist is a typo that would otherwise sit
         * there until somebody built a timetable.
         */
        @Size(max = 20) String sectionNo,

        /**
         * The staff who teach it, or absent. Example: ["67aa15d9dc3f7d0011111111"]
         *
         * <p>Every id is checked to exist <b>and</b> to belong to this school. An empty list is
         * legitimate — a subject with no teacher assigned yet is a real state, and the ordinary
         * one before staff are onboarded.
         *
         * <p>Duplicates are refused rather than silently collapsed: the same teacher twice is a
         * client bug, and a list that quietly changes length is one nobody notices.
         */
        List<@Size(max = 60) String> teacherDocsIds,

        /**
         * How marks for this subject are read, or absent. Example: "67aa15d9dc3f7d0022222222"
         *
         * <p>Checked to exist and to belong to this school. Absent falls through to
         * {@code Exam.gradingSchemeDocsId} and then to none — the resolution order is in the
         * model README.
         */
        @Size(max = 60) String gradingSchemeDocsId) {
}
