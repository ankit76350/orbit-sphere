# compliance — what the law and the boards require

Three things a school is obliged to hold or do, and had nowhere to put:

1. **Students' government identity numbers** — Aadhaar, APAAR, state PEN
2. **Parental consent** under the Digital Personal Data Protection Act
3. **What the school owes to boards and authorities**, and whether it filed on time

## Relationship overview

```text
Student
  |
  +--> StudentGovernmentIdentity[]      one row per identity type
  |        encrypted / hashed / masked — never plaintext
  |        apaarStatus + digiLockerLinked (APAAR rows only)
  |            |
  |            +-- requires --> GuardianConsent (APAAR_GENERATION)
  |
  +--> GuardianConsent[]                one row per purpose, independently withdrawable
           +--> Guardian                 ../student/Guardian.java
           +--> DocumentRecord           ../documents/DocumentRecord.java

ComplianceRequirement                   the standing obligation
  |  "UDISE+ return, annually, by 30 September"
  |
  +--> ComplianceSubmission[]           one round of doing it
           +--> acknowledgementDocumentDocsId  ← the proof it was filed
```

### Models from other packages used here

| Model | Lives in | Used for |
|---|---|---|
| [Student](../student/Student.java) | `student` | whose identity and consent |
| [Guardian](../student/Guardian.java) | `student` | who granted a consent |
| [Staff](../people/staff/Staff.java) | `people/staff` | who is responsible, who filed, who verified |
| [GovernmentIdentityType](../common/enums/GovernmentIdentityType.java) | `common/enums` | Aadhaar, APAAR, PEN and the rest |
| [IdentityVerificationStatus](../common/enums/IdentityVerificationStatus.java) | `common/enums` | how far checking has got |
| [DocumentRecord](../documents/DocumentRecord.java) | `documents` | signed forms, cards, acknowledgements |
| [AppModule](../identity/enums/AppModule.java) | `identity/enums` | the `COMPLIANCE` permission |

Named as precedent:
[StaffGovernmentIdentity](../people/staff/StaffGovernmentIdentity.java).

## The collections

| Collection | Purpose |
|---|---|
| `student_government_identities` | One government number for one student. |
| `guardian_consents` | One family's answer to one question about their child. **The only place consent is recorded.** |
| `compliance_requirements` | One standing obligation to an authority. |
| `compliance_submissions` | One round of meeting it. |

## The gap this closes

`people/staff/StaffGovernmentIdentity` has existed since the people package was built.
**Students had nothing.** A school asked for its APAAR coverage — which is being asked — had
nowhere to hold the numbers at all.

## Aadhaar is never stored in plain text

The reference sketch kept `aadhaarNo` as a plain `String`. That is both a security problem and
a legal one: holding Aadhaar numbers in the clear is restricted, and **a leaked database of
children's Aadhaar numbers is close to the worst thing this system could do.**

So the same three fields as `StaffGovernmentIdentity`, `BankAccount` and `Visitor`:

| Field | Job |
|---|---|
| `encryptedIdentityNumber` | the real value, encrypted |
| `identityNumberLookupHash` | spot a duplicate without decrypting anything |
| `maskedIdentityNumber` | `XXXX XXXX 4821` — the only version that may reach a screen |

Also replaced the sketch's `consentDocumentUrl` and `fileUrl` with `DocumentRecord` ids —
`documents` already owns private storage and short-lived signed URLs, and a bare URL in a
database is a link that either leaks or rots.

## APAAR needs a consent behind it

APAAR is generated **from a child's Aadhaar**, so `StudentGovernmentIdentity` carries
`apaarConsentDocsId` pointing at a granted `APAAR_GENERATION` consent.

Generating it without one is precisely what the consent exists to prevent, so an APAAR row
without it should not be creatable.

`apaarStatus` matters for the coverage report a school is asked for: `NOT_APPLIED` and `ERROR`
are **different problems**. One needs somebody to do the work; the other needs somebody to
find out why the details did not match Aadhaar.

## One consent per purpose, never one blanket yes

A family happy for the nurse to hold medical details may still refuse to have their child's
photograph on the school's social media. **A single yes-or-no cannot hold both answers.**

It is also what makes withdrawal meaningful — taking back consent for photographs must not
switch off the consent that lets the school keep health records.

### A withdrawal is never a delete

A school asked in June why it published a photograph in March has to show the consent that
stood **in March**. Deleting the record when the family withdrew in April would remove exactly
that evidence.

So a withdrawal sets `status` and `withdrawnAt`, and the row stays.

### `channel` decides what the school can actually show

| Channel | Weight if challenged |
|---|---|
| `SIGNED_FORM` | strongest — a scanned signature |
| `PARENT_APP`, `WEB_PORTAL` | good — a timestamped action |
| `EMAIL`, `SMS` | reasonable |
| `VERBAL_RECORDED` | weakest — a member of staff's note |

When a family says they never agreed, this is what decides whether the school has anything at
all. `withdrawalReason` is deliberately optional: **a family does not owe the school a
reason.**

## Requirement and submission are separate

The same shape as everywhere else here: something standing, and something that happens on a
date.

- **`ComplianceRequirement`** — "file the UDISE+ return every September"
- **`ComplianceSubmission`** — the 2026-27 filing, and whether it was accepted

Without the split there is nowhere to hold *"this comes round every September"* — so either
somebody remembers, or the school finds out it has missed something when an inspector asks.
**Being warned before the date is the entire point**, which is why `reminderLeadDays` exists:
being told on the day is being told too late.

`frequency` lets the next submission be created as soon as the last is accepted, so nobody has
to remember in eleven months.

`responsibleStaffDocsId` matters more than it looks. An obligation with nobody against it is
the one that gets missed, because everybody assumes somebody else is doing it.

### `acknowledgementDocumentDocsId` is the field that saves the school

An authority's receipt is the **only** thing that proves the school filed on time — and it is
exactly what nobody can find two years later when it is questioned. Everything in
`evidenceDocumentDocsIds` is working paper; this is the proof.

### `REJECTED` is not `OVERDUE`

Filed and sent back is a different problem from never filed. One needs correcting and
refiling; the other needs somebody to start. A school treating them the same will chase the
wrong person.

`OVERDUE` is a real state set by a nightly job, not a date comparison each screen makes for
itself — so the list of things the school is late on is a plain query and the day something
became late is on the record.

## The scattered consents were consolidated here

Five packages each asked a version of the same question in their own shape. **On 2026-08-20
they were all repointed at [`GuardianConsent`](GuardianConsent.java)**, which is now the only
place a school records that a guardian agreed to something. See the section at the end of this
file for what was removed and why.

## Holistic Progress Card is deliberately not here

The sketch put `HolisticProgressCard`, `HpcLevel` and `LearningDomain` in compliance. **They
belong in `academics`.**

HPC is the 2020 policy's replacement for the traditional report card, and `academics` already
owns `ReportCard`, `ReportCardSubjectResult` and `GradingScheme`. Putting HPC in compliance
would split *"how we report a child's progress"* across two packages, and the next person
looking for it would look in academics.

What belongs here is the **obligation** to produce one — a `ComplianceRequirement` of type
`DATA_SUBMISSION` — not the document itself.

**It is now built, in academics.** See
[HolisticProgressCard](../academics/examination/HolisticProgressCard.java), designed on
2026-08-19 as a sibling of `ReportCard` rather than a replacement for it — most schools will
produce both for years.

What belongs here remains the obligation to produce one, not the document.

## Deliberately left out

- **The enterprise privacy surface.** `undone/a_new/privacy` has ten models: processing
  activity registers, data protection impact assessments, data disclosure logs, legal holds,
  breach incidents, retention rules. Real obligations for a large data processor; a school
  needs the consent record and little else. Design them when a school is actually asked for a
  processing register.
- **Data subject access requests.** A parent asking for everything the school holds on their
  child is a genuine right under the Act. It is a workflow over every collection rather than a
  model here, and it needs the retention rules above to be meaningful.
- **Data quality issues.** `a_new/compliance/DataQualityIssue` tracked bad records found
  during a submission. Useful once submissions are actually running and failing.
- **Automated filing.** Nothing here talks to a portal. `authorityReference` records what came
  back from one.
- **Reminding anybody.** `reminderLeadDays` makes the condition computable. Sending the
  message is `notification`, designed last. Do not add a `reminderSentAt` field.

## Rules the services must enforce

**Access**

1. Everything here requires the `COMPLIANCE` module. Student access is not enough — a class
   teacher has no business reading a child's Aadhaar.
2. No plaintext identity number is ever persisted, logged, exported or returned to a client.
3. The masked value carries no more than the last four digits.

**Identity**

4. One row per student per `identityType`.
5. An `APAAR` row requires a `GRANTED` `APAAR_GENERATION` consent in `apaarConsentDocsId`.
   The number is derived from the child's Aadhaar; without consent it must not exist.
6. `apaarStatus` and `digiLockerLinked` are only set on an `APAAR` row.
7. `verificationStatus` moves to verified only when a named member of staff has seen the
   evidence.

**Consent**

8. `grantedByGuardianDocsId` must be a guardian of that student.
9. Only a `GRANTED` and unexpired consent permits the thing it covers.
10. A withdrawal sets `status` and `withdrawnAt`. **It never deletes the row.**
11. Anything relying on a consent stops the moment it is withdrawn — including an APAAR row's
    continued use.
12. One open (`PENDING` or `GRANTED`) consent per student per purpose. Superseded ones stay.
13. `withdrawalReason` is never required. A family does not owe the school a reason.

**Requirements and submissions**

14. One submission per requirement per `periodKey`.
15. A requirement with submissions against it is never deleted, only made inactive.
16. `ACCEPTED` requires an `acknowledgementDocumentDocsId`. Without the receipt there is no
    proof.
17. `REJECTED` requires the authority's reason; `WAIVED` requires a waiver reason.
18. A nightly job moves overdue submissions to `OVERDUE`. No screen decides it for itself.
19. Accepting a recurring submission creates the next one, so nobody has to remember in eleven
    months.

## `DpdpConsent` became `GuardianConsent` on 2026-08-20

Two changes in one, and the rename is the smaller half.

**The rename.** The Act is about processing personal data, which is where this model started.
But permission to give a child paracetamol, or to have them board in the hostel, is not a
data-protection question — and filing it in a collection called `dpdp_consents` is the category
error a data-protection audit trips over. The mechanism is genuinely identical, so the model is
shared; the name no longer claims the purposes are the same kind of thing.
[`ConsentPurpose`](enums/ConsentPurpose.java) lists which five purposes are the DPDP ones, and a
data-protection register is a query for exactly that set.

**The consolidation.** There were **seven** places recording that a guardian agreed to
something:

| Where | What it was |
|---|---|
| `compliance` | this model — the only one with a status, an expiry, a channel and a withdrawal |
| [`hostel`](../hostel/HostelAllocation.java) | `guardianConsentDocumentDocsId` → a scanned form |
| [`health`](../health/HealthProfile.java) | `routineMedicineConsent` **boolean** + a document link + a date |
| [`health`](../health/MedicationAdministration.java) | `guardianConsentDocumentDocsId` + `usedStandingConsent` **boolean** |
| [`conduct`](../conduct/StudentRecognition.java) | `publicationConsent` **boolean** + a document link |
| [`support`](../support/SupportPlan.java) | `guardianConsentDocumentDocsId` → a scanned form |

All five now hold a `*ConsentDocsId` pointing here, and **the three booleans and the duplicate
date are gone.**

The booleans were the dangerous part. A boolean can still say `true` the day after a family
withdrew, and there is no date on it to notice. On the health side that means giving a child
medicine their family had said no to. `usedStandingConsent` was worse than redundant — a null
`guardianConsentDocsId` already means the standing consent was used, so it was a second fact
able to contradict the pointer beside it.

`ConsentScope` was added to make one model serve both shapes of question. A standing consent is
one per student per purpose, which the unique index enforces; a record-specific one is found
through the record that points at it, and a child may have many. Without the distinction, either
a family could consent to medical treatment once in their child's whole school career, or the
index had to go and nothing could say which photograph consent was current.

The link is **one-directional** — records point at consents, never back. A pointer home from
here would be a second fact able to disagree with the first.
