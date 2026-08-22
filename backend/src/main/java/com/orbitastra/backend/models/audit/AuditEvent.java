package com.orbitastra.backend.models.new_new.audit;


import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.new_new.audit.embedded.AuditFieldChange;
import com.orbitastra.backend.models.new_new.audit.enums.AuditActorType;
import com.orbitastra.backend.models.new_new.audit.enums.AuditEventType;
import com.orbitastra.backend.models.new_new.audit.enums.AuditOutcome;
import com.orbitastra.backend.models.new_new.base.SchoolBase;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * One append-only record of something that happened inside a school tenant: who
 * did what, to which document, from where, and whether it succeeded.
 *
 * <p>Like every other school-owned collection this extends {@code SchoolBase},
 * so {@code schoolId} is the tenant boundary and every query must include it.
 *
 * <p><b>Append-only is a service and database-role guarantee here, not a
 * structural one.</b> The inherited lifecycle fields are not appropriate for an
 * audit record and must never be used:
 *
 * <ul>
 * <li>{@code recordState} must remain {@code ACTIVE} for the lifetime of the
 * document. Setting it to {@code DELETED} or {@code ARCHIVED} would hide
 * evidence, which is the one thing an audit trail exists to prevent, so any code
 * path that writes it is a defect.</li>
 * <li>{@code deletedAt}, {@code deletedByDocsId}, and {@code archivedAt} stay
 * null. Aged events are removed by an evidenced archival process, never by soft
 * deletion.</li>
 * <li>{@code updatedAt}, {@code updatedByDocsId}, and {@code version} are never
 * exercised, because the document is written once and never modified.</li>
 * </ul>
 *
 * <p>Enforce this with an insert-only repository interface and an insert-only
 * database role, so a future service or script cannot rewrite history even by
 * mistake. A TTL index must never be placed on this collection.
 *
 * <p>{@code occurredAt} is the business time the action happened and is the field
 * every index and report orders by. The inherited {@code createdAt} is the
 * persistence time; the two are identical for a synchronous write and differ when
 * events are queued through an outbox.
 *
 * <p>{@code eventType} is a small stable category and {@code action} names the
 * specific business operation, so new workflows never require an enum change.
 * {@code actorDisplayName}, {@code actorRoleCode}, and {@code targetLabel} are
 * snapshots: an audit entry must stay readable after the staff member or record
 * it refers to has been removed.
 */
@Document(collection = "audit_events")
@CompoundIndexes({
        @CompoundIndex(
                name = "school_audit_timeline_idx",
                def = "{'schoolId': 1, 'occurredAt': -1}"),
        @CompoundIndex(
                name = "school_audit_target_history_idx",
                def = "{'schoolId': 1, 'targetCollection': 1, 'targetDocsId': 1, 'occurredAt': -1}"),
        @CompoundIndex(
                name = "school_audit_actor_idx",
                def = "{'schoolId': 1, 'actorDocsId': 1, 'occurredAt': -1}"),
        @CompoundIndex(
                name = "school_audit_event_type_idx",
                def = "{'schoolId': 1, 'eventType': 1, 'occurredAt': -1}"),
        @CompoundIndex(
                name = "school_audit_action_idx",
                def = "{'schoolId': 1, 'action': 1, 'occurredAt': -1}"),
        @CompoundIndex(
                name = "school_audit_denied_idx",
                def = "{'schoolId': 1, 'outcome': 1, 'occurredAt': -1}"),
        @CompoundIndex(
                name = "school_audit_session_idx",
                def = "{'schoolId': 1, 'authSessionDocsId': 1, 'occurredAt': -1}",
                partialFilter = "{'authSessionDocsId': {'$type': 'string'}}"),
        @CompoundIndex(
                name = "school_audit_correlation_idx",
                def = "{'schoolId': 1, 'correlationId': 1}",
                partialFilter = "{'correlationId': {'$type': 'string'}}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class AuditEvent extends SchoolBase {
    //! this will record the every interaction action with the all the api

    // Business time the audited action happened, in UTC. Ordered by every index
    // on this collection. Example: 2026-08-22T11:05:00Z
    @NotNull
    private Instant occurredAt;

    // Stable generic category. Example: AuditEventType.STATE_CHANGE
    @NotNull
    private AuditEventType eventType;

    // Specific business operation, screaming snake case.
    // Example: "MARKS_PUBLISHED"
    @NotBlank
    private String action;

    // Whether the action succeeded, failed, or was refused.
    // Example: AuditOutcome.SUCCESS
    @NotNull
    private AuditOutcome outcome;

    // Kind of principal responsible. Example: AuditActorType.STAFF
    @NotNull
    private AuditActorType actorType;

    // Links to the acting Staff, Student, Guardian, or identity account. This is
    // the authoritative actor field; the inherited createdByDocsId is incidental.
    // Null when actorType is SYSTEM. Example: "67aa15d9dc3f7d0055555555"
    private String actorDocsId;

    // Actor name snapshotted so the entry stays readable after that record is
    // removed. Example: "Anita Sharma"
    private String actorDisplayName;

    // Role the actor was acting under at the time, not their current role.
    // Example: "EXAM_CONTROLLER"
    private String actorRoleCode;

    // Collection the action targeted; required to interpret targetDocsId.
    // Example: "student_marks"
    private String targetCollection;

    // Document id the action targeted. Example: "67aa15d9dc3f7d0066666666"
    private String targetDocsId;

    // Human-readable snapshot of the target for audit screens.
    // Example: "Aarav Sharma (ADM/2026/000001)"
    private String targetLabel;

    // Field-level detail of an UPDATE. Empty for actions with no field diff.
    @Builder.Default
    private List<AuditFieldChange> changes = new ArrayList<>();

    // Groups every event produced by one request or workflow run.
    // Example: "req-7f3a9c21"
    private String correlationId;

    // Links to AuthSession.id the action came through, so a run of events can be tied
    // to one device rather than only to one person. Without it, "somebody signed in from
    // an unfamiliar phone and then changed twelve marks" is two unconnected facts.
    // Null for a SYSTEM actor. Example: "67b11228dc3f7d0011223344"
    private String authSessionDocsId;

    // The module the action belongs to, so an audit screen can be filtered the same way
    // permissions are granted. Copied as text rather than the enum, because a module
    // renamed or removed later must not make old entries unreadable.
    // Example: "EXAMINATIONS"
    private String moduleCode;

    // Caller address as observed by the server. Example: "203.0.113.42"
    private String ipAddress;

    // Client user agent when the action came from a browser or app.
    // Example: "Mozilla/5.0 (Windows NT 10.0; Win64; x64)"
    private String userAgent;

    // Why the action was taken, or why it was refused. Required by policy for
    // sensitive operations and for DENIED outcomes.
    // Example: "Correction requested by the exam controller."
    private String reason;

    // Set when the entry must be kept longer than the ordinary retention period, such as
    // anything touching money, consent or a child's identity. Archival reads this rather
    // than deciding for itself from the event type, because the rule about what must be
    // kept is a policy decision and should be recorded on the row it applies to.
    // Example: false
    @NotNull
    @Builder.Default
    private Boolean retainLongTerm = false;

    // What went wrong, on a FAILURE. Never a stack trace and never anything the caller
    // sent: an audit row is read by people who are not entitled to see the payload.
    // Example: "Fee head no longer active."
    private String failureReason;
}