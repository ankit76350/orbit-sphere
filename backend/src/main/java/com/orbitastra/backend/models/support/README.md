# support — extra help with learning

For children who need something different from the rest of the class: a child with dyslexia, a
child who cannot hear well, a child two years behind in reading after changing schools, a child
who is ahead and bored.

**Safeguarding is deliberately not in this package.** See the end of this file.

## Relationship overview

```text
Student
   |
   +--> SupportNeed[]              what the child needs help with. standing.
   |       dyslexia / hearing / catch-up      not tied to a year
   |       status: SUSPECTED -> ASSESSED -> ACTIVE -> MONITORING
   |          |
   |          +-- also appears as a HealthAlert of type SUPPORT_NEED
   |                                ../health/embedded/HealthAlert.java
   |          v
   +--> SupportPlan                this year's answer to it
   |       +--> SupportAccommodation[]   what changes in class and in exams
   |       |        appliesInClassroom / appliesInExamination
   |       |        extraTimePercent
   |       |             |
   |       |             v  read when a datesheet is built
   |       |        ../academics/examination/ExamSchedule.java
   |       |
   |       +--> SupportGoal[]            aim + baseline + what happened
   |       +--> guardianConsentDocsId --> GuardianConsent
   |          |
   |          v
   +--> SupportSession[]           did the help actually happen?
             DELIVERED / STUDENT_ABSENT / NOT_DELIVERED
```

### Models from other packages used here

| Model | Lives in | Used for |
|---|---|---|
| [Student](../student/Student.java) | `student` | the child |
| [Staff](../people/staff/Staff.java) | `people/staff` | who noticed, who coordinates, who delivers |
| [HealthProfile](../health/HealthProfile.java) | `health` | the same need, seen as an alert |
| [HealthAlert](../health/embedded/HealthAlert.java) | `health/embedded` | its `SUPPORT_NEED` type |
| [ExamSchedule](../academics/examination/ExamSchedule.java) | `academics/examination` | where exam accommodations must land |
| [DocumentRecord](../documents/DocumentRecord.java) | `documents` | assessment reports, signed consent |
| [AppModule](../identity/enums/AppModule.java) | `identity/enums` | the `SUPPORT` permission |
| [NumberSequenceType](../institution/enums/NumberSequenceType.java) | `institution/enums` | `SUPPORT_PLAN` |

Named as precedent:
[ConductAction](../conduct/ConductAction.java) and
[StockIssue](../inventory/StockIssue.java) — both for the same "a failure needs its own state"
reasoning.

## The collections

| Collection | Purpose |
|---|---|
| `support_needs` | What a child needs help with. Standing, not per year. |
| `support_plans` | What the school will do about it, for one stretch of time. |
| `support_sessions` | One session of help, and whether it happened. |

`SupportAccommodation` and `SupportGoal` are embedded in the plan.

## Need, plan, session — the shape

The same split as everywhere else here, three deep this time:

| | Lasts | Example |
|---|---|---|
| `SupportNeed` | years | "has dyslexia" |
| `SupportPlan` | one year | "25% extra time, a reader, two remedial classes a week" |
| `SupportSession` | 40 minutes | "Tuesday's reading class — happened" |

A child does not stop being dyslexic in March, so the **need** is not academic-year scoped. What
helps a seven-year-old is not what helps a fourteen-year-old, so the **plan** is.

## `SUSPECTED` is the state that matters most

Every one of these starts with a teacher noticing that a child cannot copy from the board — and
it is often months before any specialist confirms anything.

Without a state for a suspicion, **the concern lives in one teacher's head until they leave the
school**, and the next teacher starts again from nothing. That is the commonest way a child with
an undiagnosed difficulty goes through school unhelped.

`MONITORING` is likewise not `RESOLVED`. A child who has caught up may fall behind again, and
closing the record throws away exactly the history the next teacher would want.

## Accommodations are the point of the module

Everything else is context. These are the concrete things that change a child's day:

> 25% extra time · a separate room · a reader for the question paper · seat at the front ·
> hearing aid on the left, so seat on the right

**Classroom and exam are separate flags**, because the two settings are genuinely different.
Sitting at the front helps every day and means nothing in an exam hall. Extra time means nothing
in a lesson and is the whole point in an exam. A single "where does this apply" would force a
wrong answer for half of them.

`extraTimePercent` is its own field rather than a phrase in the description, because it is the
one accommodation that has to be **arithmetic**. An invigilator needs to know a ninety-minute
paper becomes a hundred and thirteen minutes for this child, and reading that out of a sentence
is how it gets got wrong.

### The link that matters most, and is easiest to leave unbuilt

**An exam accommodation that does not reach the invigilator on the morning did not happen.**

So `school_support_plan_exam_idx` indexes straight into the accommodations —
`accommodations.appliesInExamination` — and whoever prepares a datesheet finds every child
needing arrangements in one query instead of opening every plan and hoping. The examination
service is expected to read it. Without that link the rest of this package is paperwork.

An earlier version kept a `hasExaminationAccommodation` boolean on the plan for this. It was
dropped on 2026-08-19: MongoDB indexes into embedded arrays perfectly well, so the flag bought
nothing and could disagree with the list beside it — and the way it would fail is **a child not
getting their extra time.**

## `SupportGoal.baseline` is the field everybody forgets

*"Improve reading"* is not a goal. *"Reads twenty words a minute now, aiming for forty by
December"* is one — because in December somebody can say whether it worked.

Without a baseline recorded **before** anything starts, every review turns into an argument about
whether the child has improved and nobody can settle it. `progressNote` is filled in at review,
and a plan whose goals are all blank at review is a plan that was written and never read again.

## Sessions exist to catch the plan failing

A plan promising two reading classes a week, where **six of forty** actually happened, is a plan
that failed — and nobody would know from the plan itself, which still reads exactly as it did in
July.

`NOT_DELIVERED` is what makes that visible. With only "scheduled" and "delivered", the
thirty-four missing classes sit as scheduled forever and look like a diary rather than a failure.
Same reasoning as `ConductAction.NOT_COMPLETED` and `StockIssueStatus.NOT_RETURNED`: **a failure
has to be its own state or it hides inside a queue.**

`STUDENT_ABSENT` is separate from `NOT_DELIVERED` on purpose. A child who did not come is a
conversation with the family; a session the school did not run is a conversation with the school.
Counting them together lets the school blame the child, which is the wrong way round and easy to
do by accident.

## The family has to agree

`guardianConsentDocsId` points at a `RECORD_SPECIFIC` `LEARNING_SUPPORT`
[`GuardianConsent`](../compliance/GuardianConsent.java) — consent to *this plan*, not to being
supported in general. A plan is something done *to* a child, and some families
decline — they do not want their child treated differently, or they disagree with the assessment.
That is their decision.

A plan running without the family's knowledge is how a school loses their trust for good.

`studentVoice` is the child's own view of what helps. Older children usually know perfectly well
and are rarely asked, and a plan written entirely by adults about a fifteen-year-old is often
quietly ignored by the one person it is for.

## `nextReviewOn` is not administrative tidiness

An accommodation that helped in April may be holding a child back by December. The review is
where somebody asks whether it is still right, and a plan past its review date belongs on
somebody's list.

`DISCONTINUED` is kept apart from `COMPLETED`. Completed means the child no longer needs it.
Discontinued means it stopped for another reason — the family declined, the specialist left, the
school could not staff it. **Only one of those is good news**, and a school whose plans are mostly
discontinued has a problem worth being able to see.

## How this differs from the `SUPPORT_NEED` health alert

Both exist, and they are not duplicates:

| | `HealthAlert` of type `SUPPORT_NEED` | this package |
|---|---|---|
| Answers | "what must any teacher know about this child?" | "what is the school doing about it?" |
| Who reads it | anybody who may see the child | the `SUPPORT` module |
| Length | one line and what to do | a plan, goals, sessions, reviews |

The alert is the warning on the front. This is the file behind it. `SupportNeed.healthProfileDocsId`
links them so the two are visibly the same thing rather than two half-answers.

## Safeguarding is not in this package

Deliberately, and this is the most important paragraph here.

A concern that a child **cannot read** and a concern that a child **is being harmed** are
different in kind, not degree. The second needs access narrowed to **named individuals** — often
not the class teacher, and sometimes not the parents, because occasionally the concern is the
parents.

`identity` grants permission by **role**: anybody with the `SUPPORT` module sees everything in
these collections. That is right for a reading plan and completely wrong for a safeguarding
record.

So:

- `conduct`'s `escalatedToSafeguarding` stays a **flag with a note**, not a link. It remains the
  correct answer until something exists that can hold such records safely.
- Counselling and mental-health notes stay out of `health` **and** out of here.
- `SupportSession.note` is explicitly *"not a therapy record and must not become one"*.

Building it would mean first solving named-person access control in `identity`, which nothing in
the system currently expresses. That is the prerequisite, not a detail.

## Deliberately left out

- **Safeguarding cases and confidential case notes.** See above. The prerequisite is
  named-person access control.
- **Counselling and wellbeing case work.** Same reason. Sketches called `WellbeingCase` and
  `ConfidentialCaseNote` existed and were deleted with the rest of `models/undone` on
  2026-08-21. They held little the description above does not: a case with a counsellor, a
  confidentiality level, and notes readable by named people rather than by a role. **The
  prerequisite is still named-person access control, not a model.**
- **Billing for external specialists.** A visiting therapist invoices the school or the family.
  That is a finance question, and `SupportProviderType.EXTERNAL_SPECIALIST` records only that
  they came.
- **Automatic detection of children who might need help.** Falling marks plus poor attendance is
  a real signal, and turning it into "this child may be dyslexic" is not something a school
  management system should assert. A human notices; this records what they noticed.
- **Reminding a coordinator that a review is due.** `nextReviewOn` makes it queryable. Sending
  the message is `notification`, designed last.

## Rules the services must enforce

**Access**

1. Everything here requires the `SUPPORT` module. Student access is not enough — a child's
   learning difficulty is theirs, not something every screen should announce.
2. `SupportSession.note` never holds clinical or counselling content. That needs narrower access
   than this module gives.

**Needs**

3. One open need per student per category. A resolved one does not block a new one.
4. A need is never academic-year scoped. It follows the child.
5. `SUSPECTED` requires only a description and who noticed. Nothing waits on an assessment.
6. `RESOLVED` carries a closure note.

**Plans**

7. One `ACTIVE` plan per student per academic year.
8. Every accommodation sets at least one of `appliesInClassroom` and `appliesInExamination`. An
   adjustment that applies nowhere is not an adjustment.
9. An `EXTRA_TIME` accommodation carries `extraTimePercent`.
10. Every goal carries a `baseline`. Without it no review can settle whether it was met.
11. The examination service reads `accommodations.appliesInExamination` when a datesheet is
    built. No separate flag summarises it; the accommodations are the only record.
12. A plan past `nextReviewOn` appears on the coordinator's list.
13. `DISCONTINUED` carries a reason. The reason is the point.

**Sessions**

14. A session belongs to an `ACTIVE` plan.
15. `deliveredAt` is set only when the status is `DELIVERED`.
16. `STUDENT_ABSENT`, `NOT_DELIVERED` and `CANCELLED` all carry a reason.
17. A run of `NOT_DELIVERED` sessions reaches the coordinator before the review, not at it.
