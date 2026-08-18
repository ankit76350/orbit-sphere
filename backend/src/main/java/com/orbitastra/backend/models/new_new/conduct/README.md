# conduct — what children did, good and bad

Two jobs, deliberately in one package:

- **Discipline** — an incident happened, and here is what the school did about it.
- **Recognition** — a child did something good, and here it is written down.

They sit together because both are the school's record of how a child conducted
themselves. A package that held only the first would give every child a file that
reads like a charge sheet, and the child who was kind every day for five years would
have nothing in it at all.

## Why this package exists

`academics/README.md:247` says it outright:

> `DisciplineLog`: moved out of academics; design it in the conduct module.

It replaces the live `models/academics/DisciplineLog`, which had **four fields** —
`violation`, `fineAmount`, `actionTaken`, `incidentDate` — and four things it could not
do:

| The old model couldn't say | Now |
|---|---|
| that one fight involved three children | `ConductEvent.participants[]` |
| who started it and who was hurt | `ParticipantRole` |
| whether the parents were told | `StudentConductCase.guardiansInformed[]` |
| whether the detention was actually served | `ConductAction.status` |

## Relationship overview

```text
ConductEvent                    one incident, once
  +--> EventParticipant[]         each child + their ROLE (responsible / affected)
        |
        |  one case per RESPONSIBLE child — never for an AFFECTED one
        v
StudentConductCase              one child's side of it
  +--> GuardianInformed[]         who was told, and when   (from common)
  +--> escalatedToSafeguarding
        |
        v
ConductAction[]                 what was decided, and whether it happened
  +--> fineAmount ---> FeeInvoice   (finance bills it)
  +--> evidenceDocumentDocsId ---> DocumentRecord

StudentRecognition              the other half — the good things
  +--> housePoints
  +--> issuedDocumentDocsId ---> IssuedDocument (the certificate)
```

## The collections

| Collection | Purpose |
|---|---|
| `conduct_events` | One incident. May involve several children. |
| `student_conduct_cases` | One child's side of one incident. |
| `conduct_actions` | One thing decided, and whether it was carried out. |
| `student_recognitions` | One good thing, recorded. |

`EventParticipant` is embedded in the event. `GuardianInformed` comes from `common`.

## `ParticipantRole` is the most important thing here

The old model had one student per row and no role. A fight between three children
became three unrelated rows, and a year later nobody could tell who threw the punch
from who was hit.

```java
participants = [
  { student: Rahul,  role: RESPONSIBLE },
  { student: Aman,   role: RESPONSIBLE, note: "Joined at the end" },
  { student: Kabir,  role: AFFECTED },
  { student: Priya,  role: WITNESS }
]
```

**A child marked `AFFECTED` never has a case opened against them for that event.**
Being hurt is not something a school disciplines you for. Storing a bullying victim
and a bully as "students involved" is exactly how a school ends up punishing the
wrong child, and it is the specific failure this shape prevents.

An `AFFECTED` child may well need looking after — that is support, not discipline, and
it belongs elsewhere.

## One incident, several answers

`ConductEvent` says what happened. `StudentConductCase` says what the school is doing
about each child.

That split is what lets the same fight be `SERIOUS` for the boy who threw the punch and
`MODERATE` for the one who joined in at the end. One incident, two different and both
defensible outcomes, because they are separate records.

It also means the event stays true even if every case arising from it is later
withdrawn. **An event records facts; a case records decisions.** Nothing in
`ConductEvent` is a judgement — no warning, no outcome, no punishment.

## The gap the old model had: did it actually happen?

`actionTaken` was a single string. It could say a detention was set. It could never say
whether anybody served it.

`ConductActionStatus` has **`NOT_COMPLETED`** for exactly this. If the only states were
pending and done, a skipped detention would sit as pending forever and look like a
queue rather than a failure — which is precisely how a discipline system quietly stops
working while appearing to run.

## Fines connect to finance instead of sitting in a note

The old model's `fineAmount` was a number in a discipline row that nobody ever billed.

Now `ConductAction` records the amount, and `feeInvoiceDocsId` is set once finance has
billed it under a head with `FeeCategory.FINE` — which already exists and had nothing
feeding it. A fine with a null invoice id is one that was decided and never charged,
and that is a list somebody should look at.

Money owed belongs on a bill, not in a note about behaviour.

## Suspension and expulsion need an approver

`approvedByStaffDocsId` is optional for most actions and required for `SUSPENSION` and
`EXPULSION`.

Those two stop a child being educated, and a school will be asked who authorised it. A
class teacher can decide a detention on their own; a class teacher cannot expel
anybody.

## `escalatedToSafeguarding` is a stopping point, not a link

Some cases are not discipline at all. A child who lashes out repeatedly may be being
hurt at home, and processing that as a detention to arrange is a failure of a serious
kind.

The safeguarding module is **not built**. So this is a flag plus a note, recording that
somebody recognised it and passed it on, and taking the case out of the discipline
queue. When safeguarding is designed, this becomes a link to it.

A `COUNSELLING_REFERRAL` action is in the same spirit: the school deciding to help
rather than punish. **What the counsellor then records is not here and must not be** —
those notes need narrower access than the conduct module, the same reason counselling
stayed out of `health`.

## Recognition, and the consent that goes with it

`housePoints` lives on the recognition rather than in its own model. A house
leaderboard is this column added up over a term; no separate collection is needed to
hold a number that can be derived.

`publicationConsent` matters more than it looks. Putting a child's name and photograph
on a noticeboard, a newsletter or a social media post needs the family's agreement, and
some families withhold it for reasons the school does not need to know. Recording it
means somebody can check **before** publishing rather than apologising afterwards.

Certificates are not printed here. `issuedDocumentDocsId` points at `documents`, which
already numbers and verifies them.

## Where conduct joins the rest of the system

- **`student`** — `Student` for participants, `Guardian` for who was told.
- **`people`** — `Staff` for who reported, decided, approved and supervised. Four
  different people, four different fields, because each is accountable for something
  different.
- **`finance`** — a `RESTITUTION` fine becomes a `FeeInvoice` under `FeeCategory.FINE`.
- **`documents`** — evidence, consent forms, and the certificate for a recognition.
- **`common`** — `GuardianInformed`, now shared with `health`. Proving you told a
  family is the same problem wherever it comes up, so it was moved out of `health`
  rather than copied.
- **`identity`** — a new `CONDUCT` module, held apart from `STUDENTS`. What a child did
  wrong in Class VI should not be on every screen for the rest of their time here.

## Added to the shared enums

- `AppModule.CONDUCT`
- `NumberSequenceType.CONDUCT_EVENT`, `CONDUCT_CASE`, `STUDENT_RECOGNITION`

## Deliberately left out

- **Safeguarding cases.** Referred to by a flag, not modelled. They need much tighter
  access than the conduct module and involve outside agencies, statutory timelines and
  records the head may not share with teachers. Designing that as a corner of
  discipline would get it wrong.
- **Counselling notes.** Same reason, same as in `health`.
- **A behaviour points system with automatic escalation** — "three yellow cards make a
  red". Schools that want this all want it differently. Build it as rules over these
  records when a school asks, not as fields now.
- **Anonymous reporting.** A worthwhile feature and its own design problem: an
  anonymous report has no `reportedByStaffDocsId`, which every part of this package
  currently relies on.
- **Telling the parent automatically.** `guardiansInformed` records that somebody told
  them. Nothing here records a message being sent — that is `notification`, designed
  last. Do not add a `notifiedAt` field to get around it.

## Rules the services must enforce

**Access**

1. Everything here requires the `CONDUCT` module. Student access is not enough.
2. A student's own case history is visible to their guardians, never another family's.

**Events**

3. At least one participant, and no child listed twice.
4. `occurredAt` is not in the future, though it may be well before `reportedAt`.
5. An event is never edited to change what happened. A correction is a new event
   referencing the old one in `remarks`.

**Cases**

6. A case may only be opened for a participant whose role is `RESPONSIBLE` or
   `PRESENT`. **Never for `AFFECTED`.**
7. One case per student per event.
8. A case above `MINOR` severity must have at least one `guardiansInformed` entry
   before it can be `CLOSED`.
9. A `SEVERE` case is assigned to a senior member of staff the same day.
10. Closing requires an `outcome`. `WITHDRAWN` requires a reason too — a child accused
    of something they did not do deserves that on the record, not absent from it.
11. `escalatedToSafeguarding` takes the case out of the discipline queue; it is not
    left sitting as `ACTION_PENDING`.

**Actions**

12. `SUSPENSION` and `EXPULSION` require `approvedByStaffDocsId`.
13. `NOT_COMPLETED` and `CANCELLED` require `notCompletedReason`.
14. `fineAmount` is only set for `RESTITUTION`, and billing it is finance's job — this
    package never touches an invoice directly.
15. `completedAt` is set only when the status is `COMPLETED`.
16. An action's `studentDocsId` must match its case's.

**Recognitions**

17. Nothing is published outside the school without `publicationConsent = true`.
18. `housePoints`, when set, is positive.
19. `awardedOn` falls inside the academic year.
