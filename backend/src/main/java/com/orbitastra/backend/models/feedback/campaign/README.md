# feedback/campaign — the school asks, and counts the answers

**Read [`../README.md`](../README.md) first.** The anonymity design is shared with
[`../report`](../report/README.md) and is not repeated here — including the three ways this
codebase will destroy anonymity by accident, all of which apply to every model below.

Models only, no API. See the parent README for what that means.

## What these models answer

1. What kinds of feedback does the school collect, from whom, about whom?
2. What did the school promise the person who gave it?
3. What did they actually say?
4. What does the person it is about get to see — and when?

## Relationship overview

```text
FeedbackTopic                    the standing config: the rules for one kind of feedback
   |   who may submit / about what / which promises / who may read / threshold
   +--> FeedbackQuestion[]         asked here, not on the campaign, so terms compare
   |
   v
FeedbackCampaign                 one drive, open between two dates. OPTIONAL.
   |   targets classes, not students, so a November joiner is included
   |   CLOSED and PUBLISHED are two decisions
   |
   v
FeedbackSubmission               one answer to a drive
   +--> FeedbackAnswer[]           question wording copied in at submission time
   |    anonymityMode decides which identity fields may be filled AT ALL
   |    subjectDocsId only -- the TYPE is read through the topic, never copied
   |
   v
FeedbackAggregate                the numbers, and the ONLY thing a subject ever reads
   +--> FeedbackQuestionAggregate[]
          +--> FeedbackRatingBucket[]   distribution, which says more than the mean
        no comments stored here, ever
        suppressed until responseCount passes the topic's threshold
```

## The collections

| Collection | Purpose |
|---|---|
| `feedback_topics` | One kind of solicited feedback, and all the rules for it. |
| `feedback_campaigns` | One drive to collect it, open between two dates. Optional. |
| `feedback_submissions` | One answer to a drive. |
| `feedback_aggregates` | The numbers for one subject in one campaign. What a subject reads. |

`FeedbackQuestion`, `FeedbackAnswer`, `FeedbackQuestionAggregate` and `FeedbackRatingBucket` are
embedded and have no collections of their own.

## Models named here from other packages

| Model | Package |
|---|---|
| [`FeedbackAnonymityMode`](../enums/FeedbackAnonymityMode.java) | feedback — shared with the report side |
| [`FeedbackSubjectType`](../enums/FeedbackSubjectType.java) | feedback — shared with the report side |
| [`AuditedDocument`](../../base/AuditedDocument.java) | base — **the `createdByDocsId` hazard** |
| [`AuditEvent`](../../audit/AuditEvent.java) | audit — the second hazard, and where unmasking is recorded |
| [`PersonType`](../../identity/enums/PersonType.java) | identity — reused as the submitter type, not re-invented |
| [`UserAccount`](../../identity/UserAccount.java) | identity — the submitter, for `IDENTIFIED` only |
| [`Staff`](../../people/staff/Staff.java) | people — a subject, and every reviewer and coordinator |
| [`Student`](../../student/Student.java) | student — a subject, when the school allows it |
| [`Department`](../../people/organization/Department.java) | people — a subject |
| [`SchoolClass`](../../academics/structure/SchoolClass.java) | academics — the campaign audience and the submitter's class |
| [`AcademicTerm`](../../academics/structure/AcademicTerm.java) | academics — which term a drive belongs to |
| [`AcademicYear`](../../core/AcademicYear.java) | core — `academicYear` is its `name` |
| [`DailyTimetable`](../../academics/timetable/DailyTimetable.java) | academics — resolves which teachers a student rates |
| [`DocumentRecord`](../../documents/DocumentRecord.java) | documents — attachments |
| [`NumberSequence`](../../institution/NumberSequence.java) | institution — `referenceNo` |
| [`StockMovement`](../../inventory/StockMovement.java) | inventory — the precedent for not storing a derivable type |
| [`StudentRecognition`](../../conduct/StudentRecognition.java) | conduct — where praise for a child already lives |
| [`StudentConductCase`](../../conduct/StudentConductCase.java) | conduct — where a complaint about a child already lives |
| [`SupportNeed`](../../support/SupportNeed.java) | support — a child's learning difficulty |

## Six decisions worth explaining

### 1. `minimumResponsesToReveal` is what makes anonymity hold

An average built from three responses in a class of five is **not anonymous arithmetic.** The
teacher can work out who said what, and if two of the three were kind, they know exactly who
the third was.

So nothing reaches the subject until the topic's threshold is met. Five is a reasonable floor. A
small school may have classes that never produce a releasable result, and that is the honest
outcome rather than a problem to configure away.

**The threshold applies to every breakdown, not only the total.** Thirty responses overall with
three from Class VI-A means the per-class view must be suppressed even though the total passes.
`submitterType` and `submitterClassDocsId` are kept because feedback cannot be read without them
— "students say one thing, parents another" is the finding — but they are **quasi-identifiers**,
and a breakdown by a quasi-identifier is a new, smaller cohort with its own threshold.

### 2. Aggregate vs full comments: the teacher will go looking

A teacher seeing *"your average was 4.2 from thirty-one responses"* learns something useful and
cannot go hunting. **A teacher reading thirty-one individual anonymous comments will try to work
out who wrote the unkind one** — and in a class they teach every day, they will often be right.

That is not a hypothetical. It is the ordinary human response to being criticised anonymously by
people you can name. So `SUBJECT_AGGREGATE` is the safe default for anything students or parents
say about staff, and `SUBJECT_FULL` is a deliberate decision for feedback that was never
anonymous to begin with.

### 3. `FeedbackAggregate` is a collection, and it contradicts my own rule

Everywhere else in this system I have argued that a report is a report and a derivable total
should be derived. This one is materialised anyway, and **the reason is not performance.**

If a teacher's screen computed the average on the fly, that request path would have to open
thirty anonymous submissions belonging to children they teach. The only thing then standing
between the teacher and the raw comments is that the code currently chooses not to return them.
One bug, one debug endpoint, one hurried CSV export, and it is gone.

Materialising the numbers means the teacher's request touches a document **that never contained
a name in the first place.** It is a privacy boundary that happens to look like a cache — and it
must still be rebuildable from the submissions, which stay the real record.

No comments are stored in it, ever. Text answers contribute only a count.

### 4. There is no ranking, and that is a design decision

No percentile, no rank, no "above school average" flag on
[`FeedbackAggregate`](FeedbackAggregate.java).

A school that ranks its teachers on student ratings has built a league table, and the field that
made it possible was always an innocent-looking one. Student ratings measure warmth and clarity
reasonably well and measure how much a child learned rather badly; a hard marker teaching a
difficult syllabus will sit at the bottom of that table for years.

Whether to make the comparison is a decision for a head reading a report, not a number this
model hands them by default.

### 5. `subjectType` is recorded once, on the topic

A submission says *which* member of staff it is about, and nothing more.
[`FeedbackTopic.subjectType`](FeedbackTopic.java) says whether that id is a member of staff, a
student or a department, and neither the submission nor the aggregate keeps a copy.

A copy would be a second field able to disagree with the first, with nothing to say which was
right — the same reason [`StockMovement`](../../inventory/StockMovement.java) derives direction
from `movementType` instead of storing it separately.

This is a **different case** from `FeeInvoice.sourceType`, where the type is stored beside the id
and should be: nothing else on that row knows what the source is. A submission always has its
topic, and the topic already said. **The report side stores the type**, correctly, for exactly
that reason — see [`../report/README.md`](../report/README.md).

The cost is that a topic's `subjectType` becomes **immutable once submissions exist** — flipping
one from `STAFF` to `STUDENT` would silently rewrite what every submission under it was ever
about.

### 6. Questions live on the topic, not the campaign

So every term asks the same thing and this December can be compared with last December. Questions
on the campaign would let somebody reword them each time, and then the two numbers are not
measuring the same thing while still looking comparable.

`questionCode` must never be renamed once submissions exist — the same rule as `headCode` and
`stopCode`. Rewording `questionText` **is** allowed, because
[`FeedbackAnswer`](embedded/FeedbackAnswer.java) keeps its own copy of the wording it was given.

## Feedback about students: built, with a warning

`FeedbackSubjectType.STUDENT` exists because it was asked for. Two things about it are worth
saying plainly rather than burying.

**Feedback about a child is not the same act as feedback about a member of staff.** A teacher
being criticised is an adult with a contract, a union, a probation process and thirty years of
adult life. A child being criticised anonymously by people they cannot see has no way to answer
and no process to appeal to.

So `FeedbackTopic.allowsAnonymousAboutStudents` **defaults to false.** A school can turn it on —
peer feedback on group work is real and useful — but it should have to do so deliberately.

**Most of what a school wants here already exists.** Before adding a topic about students, check
whether the thing being recorded is really:

| What it is | Where it already lives |
|---|---|
| A child did something wrong | [`StudentConductCase`](../../conduct/StudentConductCase.java) |
| A child did something excellent | [`StudentRecognition`](../../conduct/StudentRecognition.java) |
| A child is struggling with learning | [`SupportNeed`](../../support/SupportNeed.java) |

Those have due process, a named author and a right of reply. **This folder must not become a
second, weaker discipline log** where a child accumulates anonymous criticism that no case was
ever opened for and nobody had to justify.

## Deliberately left out

- **Sentiment analysis.** Deriving a mood from a comment is a model, not a field, and a wrong one
  attached to a person's record is worse than none.
- **Public visibility.** `FeedbackVisibility` stops at the subject and their manager. A school
  publishing teacher ratings is a decision with consequences no field should make easy.
- **Reply threads.** A rating drive with thirty replies per teacher is not a conversation, it is a
  second inbox nobody reads. The report side has `FeedbackReportMessage` because a report expects
  an answer; a submission does not.
- **An unmask log.** Revealing a confidential submitter is written to
  [`AuditEvent`](../../audit/AuditEvent.java). A second record here could disagree with it.
- **Generic surveys.** The sketched `SurveyDefinition` and `SurveyResponse` covered any
  questionnaire at all, including ones about nobody. That is a form builder. These four models are
  about feedback on a **subject**, which is what makes the anonymity and visibility rules
  meaningful.
- **`allowsUnsolicited` as a report channel.** The flag lets a structured form be filled in
  whenever somebody likes — a standing lesson-observation sheet, say — and that is a real use. It
  is **not** the way to build a direct channel to the head. Use
  [`../report`](../report/README.md).

## Rules the services must enforce

The six shared anonymity rules in [`../README.md`](../README.md) apply to everything here and are
not repeated.

**Topics and campaigns**

1. `defaultAnonymityMode` is one of `allowedAnonymityModes`; both lists are non-empty.
2. `questionCode` is unique inside a topic and never renamed once submissions exist.
3. `subjectType` is never edited once any submission points at the topic. It is the only record of
   what the feedback is about.
4. `ratingScaleMax` is not changed once submissions exist — it makes old averages incomparable.
5. Submissions are accepted only while a campaign is `OPEN`, only from `targetClassDocsIds`, and
   only from `allowedSubmitterTypes`.
6. Closing builds every aggregate. Publishing releases only the unsuppressed ones. A `CANCELLED`
   campaign releases nothing, ever.
7. `receivedResponseCount` is rebuildable from the submissions.

**Submissions**

8. `submitterFingerprint` is salted with a secret held outside the database, and is set only when
   the topic disallows repeat submission.
9. An anonymous submission about a `STUDENT` subject is refused unless
   `allowsAnonymousAboutStudents` is true on the topic.
10. Ratings fall within `1..ratingScaleMax`; required questions are answered; answers reference
    question codes that exist on the topic.
11. `subjectDocsId` is null when the topic's `subjectType` is `SCHOOL`, and non-null otherwise. The
    type itself is never copied onto a submission or an aggregate.
12. `ACTIONED` and `DISMISSED` both require `outcomeNote`. `ESCALATED` requires
    `escalatedToStaffDocsId` and `escalationNote`.
13. `WITHDRAWN` is unreachable for `ANONYMOUS`.
14. Unsolicited submissions are refused unless `allowsUnsolicited` is true.

**Aggregates**

15. Rebuilt from submissions, never incremented in place.
16. `responseCount` below the topic's minimum forces `suppressed` with a reason.
17. The threshold is applied to **every** breakdown — by submitter type, by class — not only to the
    total.
18. No comment text is ever written into an aggregate.
19. Nothing is released while its campaign is not `PUBLISHED`.
20. No aggregate endpoint reads submissions at all. That is the whole point of the collection.
