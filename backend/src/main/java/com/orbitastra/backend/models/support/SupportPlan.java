package com.orbitastra.backend.models.new_new.support;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.new_new.base.AcademicStudentSchoolBase;
import com.orbitastra.backend.models.new_new.support.embedded.SupportAccommodation;
import com.orbitastra.backend.models.new_new.support.embedded.SupportGoal;
import com.orbitastra.backend.models.new_new.support.enums.SupportPlanStatus;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * What the school will do about a child's needs, for one stretch of time.
 *
 * <p>The need is standing; the plan is this year's answer to it. A child with dyslexia has one
 * SupportNeed and a new plan each year as they move up, because what helps a seven-year-old is
 * not what helps a fourteen-year-old.
 *
 * <p>{@code accommodations} is the part that changes a child's day, and it is the reason the
 * module is worth building. Everything else here is context around it.
 *
 * <p>**Exam accommodations have to reach the exam.** An entry saying twenty-five percent extra
 * time is worth nothing if the invigilator does not know on the morning. So each accommodation
 * carries {@code appliesInExamination}, and the examination service is expected to read them
 * when a datesheet is built rather than somebody remembering to mention it. That link is the
 * single most useful thing in the package, and the easiest to leave unbuilt.
 *
 * <p>The index does that in one query by reaching into the accommodations themselves. An earlier
 * version kept a {@code hasExaminationAccommodation} flag on the plan instead; it was dropped
 * because it could disagree with the list beside it, and the way it would fail is a child not
 * getting their extra time.
 *
 * <p>{@code nextReviewOn} is not administrative tidiness. A plan nobody looks at again is a plan
 * nobody follows, and an accommodation that helped in April may be holding a child back by
 * December. The review is the point at which somebody asks whether it is still right, and a plan
 * past its review date should be on somebody's list.
 *
 * <p>{@code guardianConsentDocsId} matters because a plan is something done to a child
 * and their family should have agreed to it. Some families decline: they do not want their child
 * treated differently, or they disagree with the assessment, and that is their decision. A plan
 * running without their knowledge is how a school loses a family's trust for good.
 *
 * <p>{@code studentVoice} is the child's own view of what helps. Older children usually know
 * perfectly well what works for them and are rarely asked. A plan written entirely by adults
 * about a fifteen-year-old is often quietly ignored by the one person it is for.
 *
 * <p>DISCONTINUED is kept apart from COMPLETED. Completed means the child no longer needs it.
 * Discontinued means it stopped for another reason: the family declined, the specialist left,
 * the school could not staff it. Only one of those is good news, and a school whose plans are
 * mostly discontinued has a problem worth seeing.
 *
 * <p>The service checks that every goal carries a baseline, that an EXTRA_TIME accommodation
 * carries a percentage, that at least one of the two applicability flags is set on each
 * accommodation, that one ACTIVE plan exists per student per year, and that exam accommodations
 * are visible to whoever prepares the datesheet.
 */
@Document(collection = "support_plans")
@CompoundIndexes({
        @CompoundIndex(
                name = "school_support_plan_no_uniq",
                def = "{'schoolId': 1, 'planNo': 1}",
                unique = true),
        @CompoundIndex(
                name = "school_year_student_support_plan_uniq",
                def = "{'schoolId': 1, 'academicYear': 1, 'studentDocsId': 1}",
                unique = true,
                partialFilter = "{'status': 'ACTIVE'}"),
        @CompoundIndex(
                name = "school_support_plan_review_idx",
                def = "{'schoolId': 1, 'status': 1, 'nextReviewOn': 1}"),
        @CompoundIndex(
                name = "school_support_plan_exam_idx",
                def = "{'schoolId': 1, 'academicYear': 1, 'status': 1, 'accommodations.appliesInExamination': 1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class SupportPlan extends AcademicStudentSchoolBase {

    // School-scoped number from NumberSequence type SUPPORT_PLAN, so a plan can be referred
    // to in a meeting. Example: "SP/2026/000042"
    @NotBlank
    private String planNo;

    // Links to SupportNeed.id this plan answers. A plan may cover more than one need: a
    // child who is hard of hearing and behind in reading gets one plan, not two.
    @NotEmpty
    @Builder.Default
    private List<String> supportNeedDocsIds = new ArrayList<>();

    // What the school will actually do differently. The part that changes a child's day.
    @Valid
    @NotEmpty
    @Builder.Default
    private List<SupportAccommodation> accommodations = new ArrayList<>();

    // what the school is trying to achieve, each with a baseline so a review can settle whether it
    // worked.
    @Valid
    @Builder.Default
    private List<SupportGoal> goals = new ArrayList<>();

    // Example: SupportPlanStatus.ACTIVE
    @NotNull
    @Builder.Default
    private SupportPlanStatus status = SupportPlanStatus.DRAFT;

    // First day the plan applies. Example: 2026-07-01
    @NotNull
    private LocalDate effectiveFrom;

    // Last day it applies. Null while it is current. Example: 2027-03-31
    private LocalDate effectiveTo;

    // When somebody should look at it again. A plan past this date belongs on a list.
    // Example: 2026-12-15
    private LocalDate nextReviewOn;

    // When it was last looked at. Example: 2026-12-12
    private LocalDate lastReviewedOn;

    // Links to Staff.id for whoever holds this plan together, usually a special educator or
    // the class teacher. A plan with nobody against it is a plan nobody follows.
    // Example: "67aa15d9dc3f7d0044444444"
    @NotBlank
    private String coordinatorStaffDocsId;

    // Links to Staff.id for whoever approved it, usually the head or a coordinator.
    // Example: "67aa15d9dc3f7d0055555555"
    private String approvedByStaffDocsId;

    // Example: 2026-06-28T09:00:00Z
    private Instant approvedAt;

    // Links to GuardianConsent.id for the family's agreement to this plan.
    // RECORD_SPECIFIC, purpose LEARNING_SUPPORT, because consent is to this plan and not
    // to being supported in general. Some families decline, and that is their decision; a
    // plan running without their knowledge is how a school loses their trust.
    // Example: "67bf1124dc3f7d0033445566"
    private String guardianConsentDocsId;

    // The child's own view of what helps. Older children usually know and are rarely asked.
    // Example: "Says she can follow better if she can record the lesson and listen again."
    private String studentVoice;

    // What the family said at the review meeting.
    // Example: "Mother has arranged reading practice at home three evenings a week."
    private String guardianInput;

    // Why it stopped, when the status is DISCONTINUED. Never left blank: the reason is the
    // point. Example: "Visiting speech therapist stopped coming in November; no replacement
    // found."
    private String discontinuationReason;

    // Anything worth knowing.
    // Example: "Do not read her marks aloud in class; she asked for this."
    private String remarks;
}
