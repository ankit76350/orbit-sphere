package com.orbitastra.backend.models.support;

import java.time.LocalDate;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.base.SchoolBase;
import com.orbitastra.backend.models.support.enums.SupportNeedCategory;
import com.orbitastra.backend.models.support.enums.SupportNeedStatus;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * One thing a child needs extra help with.
 *
 * <p>
 * Dyslexia. Poor hearing. Two years behind in reading after changing schools.
 * New to the
 * language the school teaches in. Ahead of the class and bored.
 *
 * <p>
 * It is not tied to an academic year. A child does not stop being dyslexic in
 * March, and
 * making somebody re-enter it every April is how it goes missing in the year it
 * matters. What
 * the school does about it changes each year; the need does not.
 *
 * <p>
 * {@code status} starting at SUSPECTED is the part that matters most. Every one
 * of these
 * begins with a teacher noticing that a child cannot copy from the board, and
 * it is often
 * months before a specialist confirms anything. Without a state for a
 * suspicion, the concern
 * lives in one teacher's head until they leave the school, and the next teacher
 * starts again
 * from nothing.
 *
 * <p>
 * MONITORING is not RESOLVED. A child who has caught up may fall behind again,
 * and closing
 * the record entirely throws away the history the next teacher would want.
 *
 * <p>
 * {@code identifiedByStaffDocsId} is whoever first raised it, and it is worth
 * keeping even
 * years later. A concern raised by a class teacher and later confirmed is the
 * school working
 * properly, and it is the kind of thing that gets forgotten.
 *
 * <p>
 * The category is deliberately broad. A precise clinical name belongs in the
 * assessment
 * report attached here, not in an enum: the school is not diagnosing anybody,
 * it is recording
 * what a specialist said and deciding what to do in the classroom.
 *
 * <p>
 * **Safeguarding is not in this package.** A concern that a child is being
 * harmed is a
 * different thing from a concern that they cannot read, and it needs access
 * narrowed to named
 * people rather than to a role. Conduct's {@code escalatedToSafeguarding} flag
 * remains the
 * marker for that, and it stays a flag until a module exists that can hold such
 * records safely.
 *
 * <p>
 * The service checks that a need is not duplicated for the same child and
 * category while one
 * is still open, and that reading these records needs the SUPPORT module rather
 * than plain
 * student access.
 */
@Document(collection = "support_needs")
@CompoundIndexes({
                @CompoundIndex(name = "school_support_need_student_category_uniq", def = "{'schoolId': 1, 'studentDocsId': 1, 'category': 1}", unique = true, partialFilter = "{'status': {'$in': ['SUSPECTED', 'ASSESSMENT_REQUESTED', 'ACTIVE', 'MONITORING']}}"),
                @CompoundIndex(name = "school_support_need_student_idx", def = "{'schoolId': 1, 'studentDocsId': 1, 'status': 1}"),
                @CompoundIndex(name = "school_support_need_status_idx", def = "{'schoolId': 1, 'status': 1, 'category': 1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class SupportNeed extends SchoolBase {

        // Links to Student.id. Example: "67aa15d9dc3f7d0055555555"
        @Indexed
        @NotBlank
        private String studentDocsId;

        // What kind of help is needed. Broad on purpose.
        // Example: SupportNeedCategory.SPECIFIC_LEARNING_DIFFICULTY
        @NotNull
        private SupportNeedCategory category;

        // Example: SupportNeedStatus.ACTIVE
        @NotNull
        @Builder.Default
        private SupportNeedStatus status = SupportNeedStatus.SUSPECTED;

        // What was actually noticed, in the words of whoever noticed it. This is what
        // the next
        // teacher reads first.
        // Example: "Reverses letters when copying from the board and reads far below
        // the class."
        @NotBlank
        private String description;

        // The specialist's own words, where there is a report. Not the school's
        // diagnosis.
        // Example: "Mild dyslexia. Recommends extra time and a reader for long papers."
        private String assessmentSummary;

        // Who assessed the child. Example: "Dr Meera Joshi, Sunrise Child Guidance
        // Centre"
        private String assessedBy;

        // When they did. Example: 2026-07-18
        private LocalDate assessedOn;

        // Links to DocumentRecord.id for the assessment report itself.
        // Example: "67c21122dc3f7d0011223344"
        private String assessmentDocumentDocsId;

        // The day somebody first raised it, which is usually well before any
        // assessment.
        // Example: 2026-05-04
        @NotNull
        private LocalDate identifiedOn;

        // Links to Staff.id for whoever first raised it. Worth keeping years later: a
        // concern
        // raised by a class teacher and later confirmed is the school working properly.
        // Example: "67aa15d9dc3f7d0044444444"
        @NotBlank
        private String identifiedByStaffDocsId;

        // Links to HealthProfile.id when the need also appears there as an alert, so
        // the two
        // records are visibly the same thing rather than two half-answers.
        // Example: "67b71122dc3f7d0011223344"
        private String healthProfileDocsId;

        // Why it was closed, when the status is RESOLVED.
        // Example: "Reading age now at class level after two terms of remedial
        // classes."
        private String closureNote;

        // Anything worth knowing.
        // Example: "Family reluctant to use the word dyslexia; refer to it as reading
        // support."
        private String remarks;
}
