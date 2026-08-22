package com.orbitastra.backend.models.support;

import java.time.Instant;
import java.time.LocalDate;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.base.AcademicStudentSchoolBase;
import com.orbitastra.backend.models.support.enums.SupportProviderType;
import com.orbitastra.backend.models.support.enums.SupportSessionStatus;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * One session of extra help, and whether it actually happened.
 *
 * <p>A remedial reading class on Tuesday. Forty minutes with the visiting speech therapist. The
 * plan promises these; this records whether the school delivered them.
 *
 * <p>**That gap is the reason this collection exists.** A plan promising two reading classes a
 * week, where six of forty actually happened, is a plan that failed — and nobody would know
 * from the plan itself, which still reads exactly as it did in July. Only the sessions can say
 * whether the school did what it wrote down.
 *
 * <p>NOT_DELIVERED is the state that makes it work. With only "scheduled" and "delivered", the
 * thirty-four missing classes would sit as scheduled forever and look like a diary rather than a
 * failure. Same reason ConductAction has NOT_COMPLETED and a stock issue has NOT_RETURNED: a
 * failure has to be a distinct state or it hides inside a queue.
 *
 * <p>STUDENT_ABSENT is deliberately separate from NOT_DELIVERED. A child who did not come is a
 * conversation with the family; a session the school did not run is a conversation with the
 * school. Counting them together lets the school blame the child, which is the wrong way round
 * and easy to do by accident.
 *
 * <p>{@code providerType} matters because the two behave differently. A member of staff is on a
 * timetable and their sessions can be checked against it. An outside specialist visits, invoices
 * and may simply stop coming — and a plan that depends on somebody the school does not employ is
 * a plan worth watching.
 *
 * <p>{@code note} is what was covered, kept short on purpose. This is not a therapy record and
 * must not become one: anything that belongs in a clinical or counselling note does not belong in
 * a collection the SUPPORT module can read. That kind of record needs narrower access than this
 * package offers, which is why counselling stayed out of health as well.
 *
 * <p>The service checks that a session belongs to an ACTIVE plan, that a status other than
 * DELIVERED carries a reason, that {@code deliveredAt} is set only when DELIVERED, and that
 * sessions missed in a run appear on the coordinator's list rather than being noticed at review.
 */
@Document(collection = "support_sessions")
@CompoundIndexes({
        @CompoundIndex(
                name = "school_support_session_plan_idx",
                def = "{'schoolId': 1, 'supportPlanDocsId': 1, 'scheduledOn': -1}"),
        @CompoundIndex(
                name = "school_year_student_session_idx",
                def = "{'schoolId': 1, 'academicYear': 1, 'studentDocsId': 1, 'scheduledOn': -1}"),
        @CompoundIndex(
                name = "school_support_session_status_idx",
                def = "{'schoolId': 1, 'status': 1, 'scheduledOn': -1}"),
        @CompoundIndex(
                name = "school_support_session_provider_idx",
                def = "{'schoolId': 1, 'providerStaffDocsId': 1, 'scheduledOn': -1}",
                partialFilter = "{'providerStaffDocsId': {'$type': 'string'}}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class SupportSession extends AcademicStudentSchoolBase {

    // Links to SupportPlan.id this session was promised by.
    // Example: "67c21124dc3f7d0033445566"
    @NotBlank
    private String supportPlanDocsId;

    // The day it was meant to happen. Example: 2026-08-25
    @NotNull
    private LocalDate scheduledOn;

    // What the session is. Example: "Remedial reading, one to one"
    @NotBlank
    private String sessionTitle;

    // Example: SupportSessionStatus.DELIVERED
    @NotNull
    @Builder.Default
    private SupportSessionStatus status = SupportSessionStatus.SCHEDULED;

    // Whether the school's own staff, a visiting specialist, or somebody the family
    // arranged. Example: SupportProviderType.SCHOOL_STAFF
    @NotNull
    private SupportProviderType providerType;

    // Links to Staff.id when the provider is the school's own. Null for an outsider.
    // Example: "67aa15d9dc3f7d0044444444"
    private String providerStaffDocsId;

    // Who it was, when the provider is not staff. Plain text: the school records the visit
    // rather than employing the person. Example: "Ms Kavita Rao, speech therapist"
    private String externalProviderName;

    // When it actually happened. Set only when the status is DELIVERED.
    // Example: 2026-08-25T09:40:00Z
    private Instant deliveredAt;

    // How long it ran. Example: 40
    private Integer durationMinutes;

    // Which goal on the plan it was working towards, by its text, so a review can see what
    // was actually worked on. Example: "Read a Class III passage aloud at forty words a
    // minute."
    private String goalWorkedOn;

    // What was covered, kept short. Not a therapy record and must not become one: anything
    // clinical needs narrower access than this module gives.
    // Example: "Worked on three-letter blends. Managed 28 words a minute today."
    private String note;

    // Why it did not happen. Required for STUDENT_ABSENT, NOT_DELIVERED and CANCELLED,
    // because the reason is the whole point of recording the failure.
    // Example: "Remedial teacher covering an absent class teacher."
    private String notDeliveredReason;
}
