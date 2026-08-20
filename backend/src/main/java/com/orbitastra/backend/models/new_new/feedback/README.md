# feedback — what people say about staff, students and the school

## Status: models only

The **database models in this package were designed on 2026-08-20** and are finished. No API
has been designed for any of it, and that is deliberate: endpoints, request and response
shapes, per-route permissions and edge validation all come **at the very end**, after every
module's models are done.

The "the service checks that..." notes in the javadoc and the numbered rules at the bottom of
this file are a **specification waiting for an implementation**, not a description of code
that exists.

**This matters more here than anywhere else in the system.** The models below can describe
anonymity correctly and still be built into something that leaks it, because most of the
protection is in the write path and the query path rather than in the fields. Nothing in this
package is safe until the rules at the bottom are actually enforced. Do not ship a feedback
form that says "anonymous" before then.

## What these models answer

1. What kinds of feedback does the school collect, from whom, about whom?
2. What did the school promise the person who gave it?
3. What did they actually say?
4. What does the person it is about get to see — and when?

## Read this first: anonymity is not a field

The earlier sketch had `Boolean anonymous` on a survey definition. That single field is the
whole problem in miniature, and it is worth being precise about why.

### There are two different promises, and a boolean holds neither

| | ANONYMOUS | CONFIDENTIAL |
|---|---|---|
| Submitter stored? | **No** | Yes, encrypted |
| Can the head find out? | Never | Yes, one narrow role, audited |
| Can a follow-up question be asked? | No | Yes |
| Can it be withdrawn? | No — nobody can prove it was theirs | Yes |
| Good for | "Does this teacher shout?" | A complaint needing resolution |

Most systems say "anonymous" and build the right-hand column. Worse ones store the submitter
in plain sight and hide the column on the screen — and then somebody exports to a spreadsheet
and a child's name sits beside what they said about their teacher.

**A person told "anonymous" and later identified has been lied to,** and no amount of good
intention at the time repairs it. So
[`FeedbackAnonymityMode`](enums/FeedbackAnonymityMode.java) has three named values a service
must choose between, and the choice has to be shown to the submitter in words before they
type anything.

### Three ways this codebase will destroy anonymity by accident

All three are live risks in *this* repository, not general cautions.

**1. The base class records the author.** Every document extends
[`AuditedDocument`](../base/AuditedDocument.java), which has `createdByDocsId`. Saving an
anonymous submission through the ordinary auditing path writes the submitter's id into it —
silently, by a mechanism nobody looked at, in a field nothing on screen displays. For
`ANONYMOUS` submissions `createdByDocsId` and `updatedByDocsId` **must** be written as the
fixed sentinel `"ANONYMOUS"`, never left to the interceptor.

This is the single most important rule in the package, and it is the one most likely to be
missed, because everything looks correct on screen while being wrong in the database.

**2. The audit trail records the write.** An [`AuditEvent`](../audit/AuditEvent.java) saying
*"user 4471 created feedback_submission 8812 at 10:03"* deanonymises the submission
completely, whatever this document contains. Anonymous writes must be audited **without an
actor and without the document id**. The school may know a submission happened, or who was
logged in — never both together.

**3. The duplicate check can be brute-forced.** `submitterFingerprint` is a hash so nobody can
read it back. But a school has five hundred students: hashing every id against one campaign
takes a laptop a fraction of a second. **The fingerprint is only anonymous if the hash
includes a secret the database does not contain** — a key in application config or a key
store, never in a collection, never in a backup that travels with the data. Without that, the
field is a name in a thin disguise.

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
FeedbackSubmission               one piece of feedback
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

## Models named above from other packages

| Model | Package |
|---|---|
| [`AuditedDocument`](../base/AuditedDocument.java) | base — **the `createdByDocsId` hazard** |
| [`AuditEvent`](../audit/AuditEvent.java) | audit — the second hazard, and where unmasking is recorded |
| [`PersonType`](../identity/enums/PersonType.java) | identity — reused as the submitter type, not re-invented |
| [`UserAccount`](../identity/UserAccount.java) | identity — the submitter, for `IDENTIFIED` only |
| [`Staff`](../people/staff/Staff.java) | people — a subject, and every reviewer and coordinator |
| [`Student`](../student/Student.java) | student — a subject, when the school allows it |
| [`Department`](../people/organization/Department.java) | people — a subject |
| [`SchoolClass`](../academics/structure/SchoolClass.java) | academics — the campaign audience and the submitter's class |
| [`AcademicTerm`](../academics/structure/AcademicTerm.java) | academics — which term a drive belongs to |
| [`AcademicYear`](../core/AcademicYear.java) | core — `academicYear` is its `name` |
| [`DailyTimetable`](../academics/timetable/DailyTimetable.java) | academics — resolves which teachers a student rates |
| [`DocumentRecord`](../documents/DocumentRecord.java) | documents — attachments |
| [`NumberSequence`](../institution/NumberSequence.java) | institution — `referenceNo` |
| [`StudentRecognition`](../conduct/StudentRecognition.java) | conduct — where praise for a child already lives |
| [`StudentConductCase`](../conduct/StudentConductCase.java) | conduct — where a complaint about a child already lives |
| [`ConductEvent`](../conduct/ConductEvent.java) | conduct — the dated incident behind a case |
| [`SupportNeed`](../support/SupportNeed.java) | support — the module that also deliberately excluded safeguarding |

## The collections

| Collection | Purpose |
|---|---|
| `feedback_topics` | One kind of feedback, and all the rules for it. Set up once. |
| `feedback_campaigns` | One drive to collect it, open between two dates. Optional. |
| `feedback_submissions` | One piece of feedback somebody gave. |
| `feedback_aggregates` | The numbers for one subject in one campaign. What a subject reads. |

`FeedbackQuestion`, `FeedbackAnswer`, `FeedbackQuestionAggregate` and `FeedbackRatingBucket`
are embedded and have no collections of their own.

## Five decisions worth explaining

### 1. `minimumResponsesToReveal` is what makes anonymity hold

An average built from three responses in a class of five is **not anonymous arithmetic.** The
teacher can work out who said what, and if two of the three were kind, they know exactly who
the third was.

So nothing reaches the subject until the topic's threshold is met. Five is a reasonable floor.
A small school may have classes that never produce a releasable result, and that is the honest
outcome rather than a problem to configure away.

**The threshold applies to every breakdown, not only the total.** Thirty responses overall
with three from Class VI-A means the per-class view must be suppressed even though the total
passes. `submitterType` and `submitterClassDocsId` are kept because feedback cannot be read
without them — "students say one thing, parents another" is the finding — but they are
**quasi-identifiers**, and a breakdown by a quasi-identifier is a new, smaller cohort with its
own threshold.

### 2. Aggregate vs full comments: the teacher will go looking

A teacher seeing *"your average was 4.2 from thirty-one responses"* learns something useful and
cannot go hunting. **A teacher reading thirty-one individual anonymous comments will try to
work out who wrote the unkind one** — and in a class they teach every day, they will often be
right.

That is not a hypothetical. It is the ordinary human response to being criticised anonymously
by people you can name. So `SUBJECT_AGGREGATE` is the safe default for anything students or
parents say about staff, and `SUBJECT_FULL` is a deliberate decision for feedback that was
never anonymous to begin with.

### 3. `FeedbackAggregate` is a collection, and it contradicts my own rule

Everywhere else in this system I have argued that a report is a report and a derivable total
should be derived. This one is materialised anyway, and **the reason is not performance.**

If a teacher's screen computed the average on the fly, that request path would have to open
thirty anonymous submissions belonging to children they teach. The only thing then standing
between the teacher and the raw comments is that the code currently chooses not to return
them. One bug, one debug endpoint, one hurried CSV export, and it is gone.

Materialising the numbers means the teacher's request touches a document **that never
contained a name in the first place.** It is a privacy boundary that happens to look like a
cache — and it must still be rebuildable from the submissions, which stay the real record.

No comments are stored in it, ever. Text answers contribute only a count.

### 4. There is no ranking, and that is a design decision

No percentile, no rank, no "above school average" flag on
[`FeedbackAggregate`](FeedbackAggregate.java).

A school that ranks its teachers on student ratings has built a league table, and the field
that made it possible was always an innocent-looking one. Student ratings measure warmth and
clarity reasonably well and measure how much a child learned rather badly; a hard marker
teaching a difficult syllabus will sit at the bottom of that table for years.

Whether to make the comparison is a decision for a head reading a report, not a number this
model hands them by default.

### 5. `subjectType` is recorded once, on the topic

A submission says *which* member of staff it is about, and nothing more.
[`FeedbackTopic.subjectType`](FeedbackTopic.java) says whether that id is a member of staff, a
student or a department, and neither the submission nor the aggregate keeps a copy.

A copy would be a second field able to disagree with the first, with nothing to say which was
right — the same reason [`StockMovement`](../inventory/StockMovement.java) derives direction
from `movementType` instead of storing it separately.

This is a **different case** from `FeeInvoice.sourceType`, where the type is stored beside the
id and should be: nothing else on that row knows what the source is. A feedback submission
always has its topic, and the topic already said.

The cost is that a topic's `subjectType` becomes **immutable once submissions exist** —
flipping one from `STAFF` to `STUDENT` would silently rewrite what every submission under it
was ever about. It should have been immutable regardless.

### 6. Questions live on the topic, not the campaign

So every term asks the same thing and this December can be compared with last December.
Questions on the campaign would let somebody reword them each time, and then the two numbers
are not measuring the same thing while still looking comparable.

`questionCode` must never be renamed once submissions exist — the same rule as `headCode` and
`stopCode`. Rewording `questionText` **is** allowed, because
[`FeedbackAnswer`](embedded/FeedbackAnswer.java) keeps its own copy of the wording it was
given, the same way [`FeedbackAggregate`](FeedbackAggregate.java) does.

## Feedback about students: built, with a warning

You asked for feedback about students, so `FeedbackSubjectType.STUDENT` exists. Two things
about it are worth saying plainly rather than burying.

**Feedback about a child is not the same act as feedback about a member of staff.** A teacher
being criticised is an adult with a contract, a union, a probation process and thirty years of
adult life. A child being criticised anonymously by people they cannot see has no way to
answer and no process to appeal to.

So `FeedbackTopic.allowsAnonymousAboutStudents` **defaults to false.** A school can turn it on
— peer feedback on group work is real and useful — but it should have to do so deliberately.

**Most of what a school wants here already exists.** Before adding a topic about students,
check whether the thing being recorded is really:

| What it is | Where it already lives |
|---|---|
| A child did something wrong | [`ConductEvent`](../conduct/ConductEvent.java) → [`StudentConductCase`](../conduct/StudentConductCase.java) |
| A child did something excellent | [`StudentRecognition`](../conduct/StudentRecognition.java) |
| A child is struggling with learning | [`SupportNeed`](../support/SupportNeed.java) |

Those have due process, a named author and a right of reply. **This module must not become a
second, weaker discipline log** where a child accumulates anonymous criticism that no case was
ever opened for and nobody had to justify.

## What must leave this module

Some of what arrives here is not feedback. It is an allegation that somebody was hurt.

`FeedbackSubmissionStatus.ESCALATED` records that it **went somewhere else** and who took it,
and that is all this package does with it. There is no investigation, no case file, no
findings.

That is on purpose, and the reason is structural rather than squeamish: **an anonymous
accusation is the wrong foundation for a disciplinary process.** A member of staff facing
dismissal is entitled to know what is alleged and to answer it, and a system that collects
allegations under a promise of permanent anonymity has collected evidence that can never be
used fairly. Building the process here would produce exactly that.

Staff misconduct has no home in this system yet. It needs one, it needs to be designed as a
disciplinary process with an accused who can respond, and **it must not be reached by adding
fields to this module.** This is the same boundary [`support`](../support/README.md) drew when
safeguarding was deliberately left out.

## Where this stops and other modules start

| If it… | It belongs to |
|---|---|
| …is evaluative, often solicited, often counted | **here** |
| …expects an answer and a resolution | `frontoffice` — `Complaint` is still sketched there |
| …is about a child's behaviour | [`conduct`](../conduct/README.md) |
| …alleges harm to anybody | out of the system; escalate to a person |

The overlap with `frontoffice` is real and unresolved. A parent saying *"the bus driver was
rude"* is both feedback and a complaint. This module can hold it — `allowsUnsolicited`,
`ESCALATED`, `outcomeNote` are all there — but when `frontoffice` is designed, the two need one
boundary drawn deliberately rather than two half-systems that both nearly work.

## Deliberately left out

- **Sentiment analysis.** Deriving a mood from a comment is a model, not a field, and a wrong
  one attached to a person's record is worse than none.
- **Public visibility.** `FeedbackVisibility` stops at the subject and their manager. A school
  publishing teacher ratings is a decision with consequences no field should make easy.
- **Reply threads.** Asking a follow-up question is possible for `CONFIDENTIAL` and
  `IDENTIFIED` feedback and impossible for `ANONYMOUS`, which makes a general threading model
  half-useless. When `frontoffice` settles the complaint boundary, the conversation belongs on
  whichever side owns resolution.
- **An unmask log.** Revealing a confidential submitter is written to
  [`AuditEvent`](../audit/AuditEvent.java). A second record here could disagree with it, and
  two records of who unmasked somebody is worse than one.
- **Notifications.** "Your feedback was acted on", "the drive closes on Friday" — none of it is
  here, and nothing records whether a message went out. `notification` is designed **last** by
  the decision of 2026-08-14.
- **Surveys that are not feedback.** The sketched `SurveyDefinition` and `SurveyResponse`
  covered any questionnaire at all, including ones about nobody. That is a generic form
  builder. These four models are about feedback on a **subject**, which is what makes the
  anonymity and visibility rules meaningful.

## Rules the services must enforce

**Anonymity — every one of these is load-bearing**

1. `ANONYMOUS` submissions write `createdByDocsId` and `updatedByDocsId` as `"ANONYMOUS"`.
   The auditing interceptor is never allowed to fill them.
2. `ANONYMOUS` writes are audited without an actor **and** without the document id.
3. `submitterFingerprint` is salted with a secret held outside the database, and is set only
   when the topic disallows repeat submission.
4. `submitterUserAccountDocsId` is non-null **only** for `IDENTIFIED`;
   `encryptedSubmitterReference` **only** for `CONFIDENTIAL`. A submission carrying more
   identity than its mode allows is rejected, not silently trimmed.
5. A submission's mode must be one of `FeedbackTopic.allowedAnonymityModes`, and the campaign's
   mode must be too.
6. Revealing a confidential submitter requires its own permission and writes an `AuditEvent`
   every time, including failed attempts.
7. No query path returns a submitter reference to a caller holding only the subject's
   permission — and no aggregate endpoint reads submissions at all.
8. A reviewer's screen shows `submittedAt` as a date. A timestamp to the second identifies
   whoever was logged in at that minute.

**Topics and campaigns**

9. `defaultAnonymityMode` is one of `allowedAnonymityModes`; both lists are non-empty.
10. `questionCode` is unique inside a topic and never renamed once submissions exist.
11. `ratingScaleMax` is not changed once submissions exist — it makes old averages
    incomparable.
11a. `subjectType` is never edited once any submission points at the topic. It is the only
    record of what the feedback is about.
12. Submissions are accepted only while a campaign is `OPEN`, only from `targetClassDocsIds`,
    and only from `allowedSubmitterTypes`.
13. Closing builds every aggregate. Publishing releases only the unsuppressed ones. A
    `CANCELLED` campaign releases nothing, ever.
14. `receivedResponseCount` is rebuildable from the submissions.

**Submissions**

15. An anonymous submission about a `STUDENT` subject is refused unless
    `allowsAnonymousAboutStudents` is true on the topic.
16. Ratings fall within `1..ratingScaleMax`; required questions are answered; answers reference
    question codes that exist on the topic.
17. `subjectDocsId` is null when the topic's `subjectType` is `SCHOOL`, and non-null for every
    other subject type. The type itself is never copied onto a submission or an aggregate.
18. `ACTIONED` and `DISMISSED` both require `outcomeNote`. `ESCALATED` requires
    `escalatedToStaffDocsId` and `escalationNote`.
19. `WITHDRAWN` is unreachable for `ANONYMOUS`.
20. Unsolicited submissions are refused unless `allowsUnsolicited` is true.

**Aggregates**

21. Rebuilt from submissions, never incremented in place.
22. `responseCount` below the topic's minimum forces `suppressed` with a reason.
23. The threshold is applied to **every** breakdown — by submitter type, by class — not only to
    the total.
24. No comment text is ever written into an aggregate.
25. Nothing is released while its campaign is not `PUBLISHED`.
