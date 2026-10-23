package com.orbitastra.backend.models.compliance;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.base.SchoolBase;
import com.orbitastra.backend.models.compliance.enums.SubmissionStatus;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * One round of doing what a requirement asks.
 *
 * <p>The requirement says "file the UDISE+ return every September". This is the 2026-27
 * filing: when it was due, who did it, what was sent, whether the authority accepted it.
 *
 * <p>{@code periodKey} is what makes one round tell itself apart from the next. Stored as a
 * plain sortable string so a list reads in order and a lookup is one match.
 *
 * <p>OVERDUE is a real state, set by a nightly job, rather than every screen comparing the
 * due date to today. That way the list of things the school is late on is a plain query, and
 * the day something became late is on the record instead of being recomputed differently in
 * three places.
 *
 * <p>REJECTED is kept apart from OVERDUE deliberately. Filed and sent back is a different
 * problem from never filed: one needs correcting and refiling, the other needs somebody to
 * start. A school that treats them the same will chase the wrong person.
 *
 * <p>{@code acknowledgementDocumentDocsId} is the one that matters most. An authority's
 * receipt is the only thing that proves the school filed on time, and it is exactly what
 * nobody can find two years later when it is questioned. Anything else in
 * {@code evidenceDocumentDocsIds} is working paper; this is the proof.
 *
 * <p>Rows are kept after they are accepted, never cleared down. A school asked to show five
 * years of returns needs five years of rows.
 *
 * <p>The service checks that one submission exists per requirement per period, that a
 * REJECTED one carries the authority's reason, that ACCEPTED requires an acknowledgement, and
 * that accepting a recurring submission creates the next one so nobody has to remember.
 */
@Document(collection = "compliance_submissions")
@CompoundIndexes({
        @CompoundIndex(
                name = "school_submission_requirement_period_uniq",
                def = "{'schoolId': 1, 'complianceRequirementDocsId': 1, 'periodKey': 1}",
                unique = true),
        @CompoundIndex(
                name = "school_submission_due_idx",
                def = "{'schoolId': 1, 'status': 1, 'dueDate': 1}"),
        @CompoundIndex(
                name = "school_submission_requirement_idx",
                def = "{'schoolId': 1, 'complianceRequirementDocsId': 1, 'dueDate': -1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class ComplianceSubmission extends SchoolBase {

    // Links to ComplianceRequirement.id. Example: "67bf1125dc3f7d0044556677"
    @NotBlank
    private String complianceRequirementDocsId;

    // Which round this is, as a string that sorts. Example: "2026-27"
    @NotBlank
    private String periodKey;

    // The day it has to be in by. Example: 2026-09-30
    @NotNull
    private LocalDate dueDate;

    // Example: SubmissionStatus.ACCEPTED
    @NotNull
    @Builder.Default
    private SubmissionStatus status = SubmissionStatus.NOT_STARTED;

    // When it was actually sent. Example: 2026-09-24
    private LocalDate submittedOn;

    // Links to Staff.id for whoever sent it.
    // Example: "67aa15d9dc3f7d0044444444"
    private String submittedByStaffDocsId;

    // The authority's own reference for the filing, quoted if anybody queries it.
    // Example: "UDISE/2026-27/MH/0114872"
    private String authorityReference;

    // When the authority accepted or refused it. Example: 2026-10-08T06:30:00Z
    private Instant decidedAt;

    //! Links to DocumentRecord.id for the authority's receipt. The only thing that proves
    // the school filed on time, and exactly what nobody can find two years later.
    // Example: "67bf1126dc3f7d0055667788"
    private String acknowledgementDocumentDocsId;

    // Links to DocumentRecord.id for whatever was sent and any working papers.
    @Builder.Default
    private List<String> evidenceDocumentDocsIds = new ArrayList<>();

    // Why it was sent back. Required when the status is REJECTED.
    // Example: "Enrolment figures did not match the previous return; asked to reconcile
    // and refile."
    private String rejectionReason;

    // Why it is no longer required. Required when the status is WAIVED.
    // Example: "Authority extended the cycle to two years from 2026."
    private String waiverReason;

    // Example: "Filed a week early because the portal closes without warning."
    private String remarks;
}
