# feedback — what people tell the school

## Status: models only

The **database models in this package were designed on 2026-08-20** and are finished. No API
has been designed for any of it, and that is deliberate: endpoints, request and response
shapes, per-route permissions and edge validation all come **at the very end**, after every
module's models are done.

The "the service checks that..." notes in the javadoc and the numbered rules in each
sub-README are a **specification waiting for an implementation**, not a description of code
that exists.

**This matters more here than anywhere else in the system.** The models can describe anonymity
correctly and still be built into something that leaks it, because most of the protection is in
the write path and the query path rather than in the fields. **Do not ship a form that says
"anonymous" until the rules below are actually enforced.**

## Two halves, and they are different acts

```
feedback/
  enums/                    the two things both halves share
  campaign/    THE SCHOOL ASKS      ->  see campaign/README.md
  report/      THE PERSON DECIDES   ->  see report/README.md
```

| | [`campaign`](campaign/README.md) | [`report`](report/README.md) |
|---|---|---|
| Who starts it | The school opens a drive | **Anybody, any hour, unprompted** |
| What is asked | Fixed questions, ratings | **A subject line and their own words** |
| About what | One type, declared on the topic | **Anything at all** |
| How many | One per person per campaign | As many as somebody needs to send |
| Goes to | A coordinator, then aggregated | **Straight to the principal** |
| Produces | Comparable numbers | **An answer** |
| Ends when | The drive is published | The thing is dealt with |

A student rating *"explains clearly: 4 out of 5"* and a student saying *"the railing on the
stairs is coming off the wall"* have almost nothing in common — except that the school must not
be able to work out who said it.

Serving both from one model means either forcing a free-form report through a
question-and-rating structure, or loosening the campaign model until its numbers stop being
comparable. So there are two, in two folders, sharing only what genuinely is shared.

## What lives at this level, and why

Only two enums, both used by both halves:

| Type | Used by |
|---|---|
| [`FeedbackAnonymityMode`](enums/FeedbackAnonymityMode.java) | every model in both folders |
| [`FeedbackSubjectType`](enums/FeedbackSubjectType.java) | `FeedbackTopic.subjectType`, `FeedbackReport.aboutSubjectType` |

They sit here rather than being copied into each folder because **the anonymity promise must
mean exactly the same thing on both sides.** Two enums with the same values in two packages is
how one of them quietly gains a fourth value, or how a service starts treating `ANONYMOUS` on
a report as something weaker than `ANONYMOUS` on a submission.

Everything else is specific to one half and lives in that half's `enums/` or `embedded/`.

## Read this first: anonymity is not a field

This applies to **both folders, without exception.** It is the reason they are one package.

The earlier sketch had `Boolean anonymous` on a survey definition. That single field is the
whole problem in miniature.

### There are two different promises, and a boolean holds neither

| | ANONYMOUS | CONFIDENTIAL |
|---|---|---|
| Submitter stored? | **No** | Yes, encrypted |
| Can the head find out? | Never | Yes, one narrow role, audited |
| Can a follow-up question be asked? | Only via the report access code | Yes |
| Can it be withdrawn? | No — nobody can prove it was theirs | Yes |
| Good for | "Does this teacher shout?" | A complaint needing resolution |

Most systems say "anonymous" and build the right-hand column. Worse ones store the submitter
in plain sight and hide the column on the screen — and then somebody exports to a spreadsheet
and a child's name sits beside what they said about their teacher.

**A person told "anonymous" and later identified has been lied to,** and no amount of good
intention at the time repairs it. So [`FeedbackAnonymityMode`](enums/FeedbackAnonymityMode.java)
has three named values a service must choose between, and the choice has to be shown to the
person in words before they type anything.

### Three ways this codebase will destroy anonymity by accident

All three are live risks in *this* repository, not general cautions. All three apply to both
folders.

**1. The base class records the author.** Every document extends
[`AuditedDocument`](../base/AuditedDocument.java), which has `createdByDocsId`. Saving an
anonymous row through the ordinary auditing path writes the submitter's id into it — silently,
by a mechanism nobody looked at, in a field nothing on screen displays. For `ANONYMOUS` rows
`createdByDocsId` and `updatedByDocsId` **must** be written as the fixed sentinel
`"ANONYMOUS"`, never left to the interceptor.

This is the single most important rule in the package, and the one most likely to be missed,
because everything looks correct on screen while being wrong in the database.

**2. The audit trail records the write.** An [`AuditEvent`](../audit/AuditEvent.java) saying
*"user 4471 created feedback_submission 8812 at 10:03"* deanonymises the row completely,
whatever the document itself contains. Anonymous writes must be audited **without an actor and
without the document id**. The school may know a submission happened, or who was logged in —
never both together.

**3. Hashes of small populations can be brute-forced.** `submitterFingerprint` on a submission
and `accessCodeHash` on a report are both hashes so nobody can read them back. But a school has
five hundred students: hashing every id against one campaign takes a laptop a fraction of a
second. **A fingerprint is only anonymous if the hash includes a secret the database does not
contain** — a key in application config or a key store, never in a collection, never in a
backup that travels with the data. An access code avoids this only by being long and randomly
generated rather than derived from anything.

### Rules that apply to both folders

1. `ANONYMOUS` rows write `createdByDocsId` and `updatedByDocsId` as `"ANONYMOUS"`. The
   auditing interceptor is never allowed to fill them.
2. `ANONYMOUS` writes are audited without an actor **and** without the document id.
3. `submitterUserAccountDocsId` is non-null **only** for `IDENTIFIED`;
   `encryptedSubmitterReference` **only** for `CONFIDENTIAL`. A row carrying more identity than
   its mode allows is rejected, not silently trimmed.
4. Revealing a confidential submitter requires its own permission and writes an `AuditEvent`
   every time, **including failed attempts**.
5. A timestamp to the second identifies whoever was logged in at that minute. Staff screens show
   the date.
6. The mode offered must be one the topic or channel allows, and the words shown to the person
   must match what the mode actually does.

## Where this stops and other modules start

| If it… | It belongs to |
|---|---|
| …was asked for by the school and gets counted | [`campaign`](campaign/README.md) |
| …somebody chose to send, in their own words | [`report`](report/README.md) |
| …is about a child's behaviour, with due process | [`conduct`](../conduct/README.md) |
| …is a child's difficulty with learning | [`support`](../support/README.md) |
| …alleges harm to anybody | out of the system; escalated to a person |

**Neither folder investigates anybody.** An allegation that a child was struck needs a process
with an accused who can answer, and that is deliberately not built here — the same boundary
[`support`](../support/README.md) drew when safeguarding was left out. Both halves have an
`ESCALATED` state whose only job is to record that something was handed to a person and who
took it.

## Two permissions

`AppModule` gained **`FEEDBACK`** for the campaign side and **`FEEDBACK_REPORTS`** for the
report side.

They are separate because they are not the same secret. A head of department may reasonably
read teaching-feedback summaries for their own team. They must never be able to open a child's
report about a colleague. `FEEDBACK_REPORTS` is the narrowest permission in the platform.

## Notifications are not here

"Your report was acknowledged", "the drive closes on Friday", "a report needs your attention
today" — none of it is in either folder, and nothing records whether a message went out.

That belongs to `notification`, which by decision on 2026-08-14 is designed **last**. Do not add
a `notifiedAt` field to get around it. This is a real gap on the report side in particular: an
acknowledgement clock nobody is told about is only half a promise.
