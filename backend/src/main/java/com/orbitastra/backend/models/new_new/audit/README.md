# Audit collection model mapping

One collection records who did what inside a school tenant.

```text
audit/
├── AuditEvent.java              collection: audit_events
├── embedded/
│   └── AuditFieldChange.java    embedded in AuditEvent.changes
└── enums/
    ├── AuditEventType.java
    ├── AuditActorType.java
    └── AuditOutcome.java
```

## Base class and the append-only guarantee

`AuditEvent` extends `SchoolBase`, consistent with every other school-owned
collection, so `schoolId`, record lifecycle, audit timestamps, and `version` are
all inherited and every query must include `schoolId`.

Several of those inherited fields are not appropriate for an append-only record
and must never be exercised. Because the base cannot prevent that, **append-only
is a service and database-role guarantee here rather than a structural one.**

| Inherited field | Required handling |
|---|---|
| `recordState` | Must stay `ACTIVE` for the document's lifetime. Writing `DELETED` or `ARCHIVED` would hide evidence; any code path that does is a defect. |
| `deletedAt`, `deletedByDocsId` | Always null. Aged events are archived with evidence, never soft-deleted. |
| `archivedAt` | Always null; archival is an external process. |
| `updatedAt`, `updatedByDocsId` | Never exercised; the document is written once. |
| `version` | Never exercised; optimistic locking is meaningless without updates. |
| `createdAt` | Persistence time. `occurredAt` is the business time and is what every index orders by. Identical for a synchronous write, different when events are queued through an outbox. |
| `createdByDocsId` | Incidental. `actorDocsId` with `actorType` is the authoritative actor record. |

Enforce the guarantee in three places:

1. the repository interface exposes insert and query operations only, with no
   save, update, or delete method;
2. the database role used by the application has insert and find privileges on
   `audit_events` but not update or delete, so a future service or script cannot
   rewrite history even by mistake;
3. a periodic check asserts that no document has a `recordState` other than
   `ACTIVE`, and treats any hit as an incident.

`SchoolBase` also contributes a single-field index on `schoolId`. Every compound
index below is already prefixed with `schoolId`, so that inherited index is
redundant here and is a candidate for removal when indexes are deployed through
migrations, since each extra index costs write throughput on the highest-volume
collection in the system.

## AuditEvent — `audit_events`

Inherited from `SchoolBase`: `id`, `schoolId`, `recordState`, `archivedAt`,
`deletedAt`, `deletedByDocsId`, `createdAt`, `updatedAt`, `createdByDocsId`,
`updatedByDocsId`, `version`.

| Field | Meaning and mapping |
|---|---|
| `occurredAt` | UTC business time the action happened. |
| `eventType` | Stable generic category from `AuditEventType`. |
| `action` | Specific business operation, such as `MARKS_PUBLISHED`. |
| `outcome` | `SUCCESS`, `FAILURE`, or `DENIED`. |
| `actorType` | Kind of principal; distinguishes human from automated action. |
| `actorDocsId` | Acting Staff, Student, Guardian, or identity account; null for `SYSTEM`. |
| `actorDisplayName` | Actor name snapshot. |
| `actorRoleCode` | Role held at the time of the action. |
| `targetCollection` | Collection acted on; required to interpret `targetDocsId`. |
| `targetDocsId` | Document acted on. |
| `targetLabel` | Human-readable target snapshot. |
| `changes` | Embedded `AuditFieldChange` list for field-level diffs. |
| `correlationId` | Groups all events from one request or workflow run. |
| `ipAddress` | Caller address observed by the server. |
| `userAgent` | Client user agent for browser and app calls. |
| `reason` | Why the action was taken, or why it was refused. |

### Two-part naming instead of one large enum

`eventType` is a fixed set of eleven generic categories. The specific operation
lives in the free-text `action`:

```text
eventType = STATE_CHANGE   action = "MARKS_PUBLISHED"
eventType = STATE_CHANGE   action = "REPORT_CARD_REVOKED"
eventType = EXPORT         action = "STUDENT_LIST_DOWNLOADED"
eventType = DELETE         action = "GUARDIAN_LINK_REMOVED"
```

A single enum covering every business verb would need editing for every new
workflow, forever. This split keeps `eventType` stable and indexable while
`action` absorbs unlimited domain growth. Services must keep `action` values
consistent, since nothing at the database level enforces the vocabulary.

### Snapshots

`actorDisplayName`, `actorRoleCode`, and `targetLabel` are copied at write time.
An audit entry must remain readable years later, after the staff member has left,
the role has been renamed, or the target document has been purged under a
retention rule. Resolving those references at read time would leave old entries
blank exactly when they matter.

`actorRoleCode` records the role held at the time and not the actor's current
role, because a permission question is always about the capacity someone acted
in.

### Failed and denied attempts are recorded

`AuditOutcome` includes `FAILURE` and `DENIED`. A trail that contains only
successful actions cannot answer whether anyone attempted to reach data they were
not entitled to, which is usually the question being asked. Authorization
rejections must produce an event with `DENIED` and a `reason` naming the failed
check.

## AuditFieldChange — embedded

| Field | Meaning |
|---|---|
| `fieldPath` | Dotted path of the changed field. |
| `previousValue` | Display value before the change. |
| `newValue` | Display value after the change. |
| `redacted` | True when values are withheld because the field is classified. |

Values are stored as display strings rather than original BSON types. An audit
trail is read, not recalculated, and a fixed string type keeps this collection
free of schema drift as the audited models change.

### Redaction

`redacted` is how restricted data is audited without duplicating it. Government
identity numbers, health and counselling notes, payroll amounts, safeguarding
detail, and credential numbers record the field name with both values omitted:

```text
fieldPath      = "encryptedIdentityNumber"
previousValue  = null
newValue       = null
redacted       = true
```

The entry proves the field changed without disclosing what it changed to. Audit
records are frequently readable by more people than the data they describe, so
storing values by default would turn this collection into a second, less
protected copy of the sensitive data.

## Rules

1. **Never update or delete a persisted event.** Repository interfaces expose
   insert and query operations only, and the inherited `recordState` must stay
   `ACTIVE`. See the base-class section above for the three enforcement points.
2. **Never place a TTL index on this collection.** TTL belongs on telemetry,
   sessions, and transient messages, never on audit, business, or financial
   records.
3. **Retention is archival, not deletion.** Removing aged events requires an
   evidenced process with export, checksum, and operator record, and any legal
   hold overrides it.
4. **The server derives `schoolId`, `actorDocsId`, and `occurredAt`.** None may be
   accepted from a request body, or the trail can be forged.
5. **Audit writes must not silently fail.** If an audited action commits but its
   event does not, the trail is incomplete. Write the event in the same
   transaction as the action where possible, otherwise through a durable outbox.
6. **One correlationId per request.** A single user action that touches several
   documents produces several events sharing one `correlationId`.

## Intentionally deferred

**Hash chaining.** `previousEventHash` and `eventHash` fields would let deletion
or modification of an event be detected. They are omitted because a partially
implemented chain gives false assurance, and because the chain requires
serialized writes per school, which conflicts with a high-volume collection.
Chaining can begin at any future point in history without migrating existing
documents, so nothing is lost by deferring it.

**Sequence numbers.** The reference model allocated a `sequenceNo` per event
under a unique index. That places a counter allocation in front of the busiest
write path in the system. Events are ordered by `occurredAt` instead, matching
the decision already recorded for `SubscriptionHistory`.

**Read auditing by default.** `AuditEventType.READ` exists, but recording every
read would dwarf all other data. Only restricted-data reads should produce
events, decided per module.

## Relationship to the reference models

Two sketches were combined and both were substantially changed.

`a_working/audit/AuditLog` supplied the practical shape — actor, role at the time,
action, target, before and after values, IP address — and its `SchoolBase` parent
is kept here for consistency with the rest of the design. Its
`currentRoleOfUser` idea survives as `actorRoleCode`.

What was corrected: a time-zone-ambiguous `LocalDateTime` became an `Instant`;
single-field indexes that were not tenant-prefixed became compound indexes
beginning with `schoolId`; `resourceDocsId` gained the `targetCollection` needed
to interpret it; the flat `oldValue`/`newValue` pair, which could not say which
field changed and broke entirely when two fields changed at once, became the
`changes` list; and `outcome`, `actorType`, and `eventType` were added so failed
attempts, automated actions, and event categories can be recorded and queried.

`a_new/audit/AuditEvent` supplied the correct structure — `Instant` timestamps,
actor type, outcome, target type and id, correlation id, and tenant-prefixed
compound indexes. Its `sequenceNo` unique index, campus scope, untyped string
enums, and untyped `safeMetadata` map were dropped.

## Validation responsibility

The model carries only structural constraints. Services and DTOs validate the
`action` vocabulary, that `targetCollection` names a real collection, that
`actorDocsId` is present for every non-`SYSTEM` actor type, that a `reason`
accompanies sensitive operations and `DENIED` outcomes, that classified fields are
recorded with `redacted = true`, and that no caller can supply tenant, actor, or
timestamp values.

MongoDB indexes and collection validators should be deployed through controlled
database migrations. Write access should be insert-only at the database role
level, so that a future service or script cannot modify history even by mistake.
