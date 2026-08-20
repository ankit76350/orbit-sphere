# feedback/report — anybody tells the head anything, any time

**Read [`../README.md`](../README.md) first.** The anonymity design is shared with
[`../campaign`](../campaign/README.md) and is not repeated here — including the three ways this
codebase will destroy anonymity by accident, all of which apply to both models below.

Models only, no API. See the parent README for what that means, and why it matters more here
than anywhere else in the system.

## What these models answer

1. Can anybody tell the head anything, at any time, without giving their name?
2. Where does it go — and where does it go when it is *about* the head?
3. Did somebody read it, and by when did they promise to?
4. Can the school ask a follow-up question of a person it cannot identify?
5. What was actually done about it?

## Relationship overview

```text
FeedbackReportChannel            one row per category the school accepts
   |   recipient + BACKUP recipient / which promises / acknowledgement clock
   |   who else may read it (a grievance committee, named person by named person)
   |
   v
FeedbackReport                   one thing somebody chose to tell the head
   |   subject + description, in their own words
   |   incidentDate / incidentTimeNote / incidentLocation, all optional
   |   aboutSubjectType + aboutSubjectDocsId, both optional -- "anything"
   |   requiresImmediateAttention  -> jumps the queue
   |   accessCodeHash              -> how an anonymous reporter ever comes back
   |   routedToStaffDocsId         -> resolved at submission, never read live
   |
   +--> FeedbackReportMessage[]    the two-way conversation, anonymity intact
          visibleToReporter separates a question from an internal note
```

## The collections

| Collection | Purpose |
|---|---|
| `feedback_report_channels` | Where reports of one category go, and who may read them. |
| `feedback_reports` | One thing somebody chose to tell the head, in their own words. |

`FeedbackReportMessage` is embedded and has no collection of its own.

## Models named here from other packages

| Model | Package |
|---|---|
| [`FeedbackAnonymityMode`](../enums/FeedbackAnonymityMode.java) | feedback — shared with the campaign side |
| [`FeedbackSubjectType`](../enums/FeedbackSubjectType.java) | feedback — shared with the campaign side |
| [`AuditedDocument`](../../base/AuditedDocument.java) | base — **the `createdByDocsId` hazard** |
| [`AuditEvent`](../../audit/AuditEvent.java) | audit — the second hazard, and where unmasking is recorded |
| [`AuthSession`](../../identity/AuthSession.java) | identity — the precedent for indexing a token hash without `schoolId` |
| [`PersonType`](../../identity/enums/PersonType.java) | identity — who is reporting, and optional here |
| [`UserAccount`](../../identity/UserAccount.java) | identity — the reporter, for `IDENTIFIED` only |
| [`Staff`](../../people/staff/Staff.java) | people — the recipient, the backup, every reader |
| [`Student`](../../student/Student.java) | student — what a report can be about |
| [`DocumentRecord`](../../documents/DocumentRecord.java) | documents — photographs and attachments |
| [`NumberSequence`](../../institution/NumberSequence.java) | institution — `reportNo` |
| [`FeeInvoice`](../../finance/billing/FeeInvoice.java) | finance — `sourceType`, the precedent for storing a type beside an id |
| [`ConductEvent`](../../conduct/ConductEvent.java) | conduct — where a child's behaviour is properly handled |

## Six decisions worth explaining

### 1. `accessCodeHash` is what makes anonymous reporting worth having

A truly anonymous reporter has **no login to come back to.** Without a code they can never learn
whether anything happened — so the channel is a black hole, and a black hole gets used once. One
term of silence and the school has a reporting page that nobody uses and a genuine belief that
nobody has anything to report.

So the reporter is shown a code at submission, printed once and never recoverable. They return
with it to read the status, answer a question, or add something they forgot. Only the hash is
stored, so somebody reading the database cannot use the codes to impersonate reporters.

It is indexed **without `schoolId`**, the same as
[`AuthSession.refreshTokenHash`](../../identity/AuthSession.java), because a code is looked up
before the school is necessarily known and must be unique everywhere.

**There is no "resend my code" path.** There is nobody to resend it to. That is a real cost of
true anonymity and belongs in the words shown before somebody chooses it.

### 2. The conversation matters more than it looks

Half of what arrives in a channel like this **cannot be acted on as written.** *"A teacher was
shouting at a child in the corridor"* needs somebody to ask which corridor, which day, roughly
what time. Without a way to ask, the school guesses or files it, and the reporter learns that
speaking up achieves nothing.

[`FeedbackReportMessage.visibleToReporter`](embedded/FeedbackReportMessage.java) keeps a question
meant for them apart from a note the recipient wrote to a colleague. Both are text on the same
report, and **that one field is all that stops an internal opinion reaching the person it is
about.** It must be honoured on every reporter-facing path, including a full-report export.

`authorSide` is an enum rather than "staff id is null means the reporter", because an anonymous
reporter has no id and the null would then mean two different things.

### 3. A report about the principal must not go to the principal

`FeedbackReportChannel.backupRecipientStaffDocsId` looks like belt-and-braces and is not.

A channel that promises to hear anything **will** eventually be used to report the person who
runs the school. If that report lands in their inbox, the reporter is worse off than if the
channel had never existed — they have identified themselves as a problem to the one person who
can act on it.

So the backup is `@NotBlank`: there has to be somewhere for it to go, decided in advance rather
than in the moment. And `routedToStaffDocsId` is resolved at submission and **stored**, never read
live through the channel. Reading it live is exactly how the report reaches the person it is
about — and it would also silently re-route every historical report the day the principal
changes.

`routingNote` records why it went to the backup, so the decision is visible rather than looking
like a mistake.

### 4. `ACKNOWLEDGED` is a state, and leaving it out kills the channel

**A person who reports something and hears nothing concludes the channel does not work, and
never uses it again** — and tells others not to bother. One school year of silence is enough to
make a speak-up channel worthless, and no policy repairs it afterwards.

So acknowledgement is a state with a clock on it, separate from anything being decided. *"We have
read this and somebody is looking at it"* is a different message from *"here is what we did"*, it
arrives days earlier, and it is the one that keeps the channel alive.

`acknowledgementDays` on the channel and `acknowledgementDueBy` on the report make an unanswered
report a **measurable failure with a date on it** rather than a matter of opinion. "How many
reports did we leave unanswered last term" becomes a question with an answer, and somebody is
accountable for it.

`AWAITING_REPORTER` exists for the same reason from the other direction: once the school has
asked a question, the report is not stalled by the school any more, and a queue that cannot show
that makes the office look slow for something it is waiting on.

### 5. One clear question instead of a severity scale

`requiresImmediateAttention` asks **is somebody in danger now?** — not "rate the severity from
one to four".

A frightened reporter cannot calibrate a scale, and a number they guess at is worse than no
field, because it will be trusted for triage. One question with an obvious answer does the single
thing triage actually needs: this jumps the queue. `urgentByDefault` on the channel does the same
for whole categories, so harassment and safety reports are flagged the moment they arrive without
the reporter having to know to tick a box.

### 6. `aboutSubjectType` IS stored here, and that is not a contradiction

It was dropped from `FeedbackSubmission` and kept here. The test is the same one either way:
**does anything else on the row already know?**

| | Knows the type? | So |
|---|---|---|
| `FeedbackSubmission` | Yes — its topic declares it | drop the copy |
| `FeedbackReport` | No — the reporter chose, no config knows | store it |

This is the [`FeeInvoice.sourceType`](../../finance/billing/FeeInvoice.java) case, where a
polymorphic pointer genuinely needs its type beside it.

Both fields are also **optional**, because "anything" includes things with no record in the
system — a broken railing on the stairs, a rumour, a policy the reporter thinks is unfair. So is
`submitterType`: unlike on a campaign submission, a reporter who does not want to say what they
are should not have to, and on a quiet channel the role alone can narrow it to a handful of
people.

## `OTHER` is in the category list on purpose

It breaks the usual rule against catch-all values, and it is right here. The promise being made
is *"tell us anything"*, and a person with something to say who cannot find a category that fits
will either force it into the wrong one or say nothing at all. The subject line carries what the
category could not.

A channel with many `OTHER` reports is telling the school its category list is wrong. That is
useful information, not a defect.

## What must leave this folder

`HARASSMENT_OR_BULLYING` and `SAFETY_CONCERN` are in the category list even though **nothing here
investigates either of them.** They are listed so they are recognised the moment they arrive,
routed within the hour rather than the week, and escalated deliberately instead of sitting in
`OTHER` until somebody reads down the queue. Receiving something properly and handling it are two
different jobs; this folder does the first.

`ESCALATED` records that a report was handed to a person and who took it. There is no
investigation, no case file, no findings.

That is structural rather than squeamish: **an anonymous accusation is the wrong foundation for a
disciplinary process.** A member of staff facing dismissal is entitled to know what is alleged and
to answer it, and a system that collects allegations under a promise of permanent anonymity has
collected evidence that can never be used fairly. Building the process here would produce exactly
that.

Staff misconduct has no home in this system yet. It needs one, designed as a process with an
accused who can respond, and **it must not be reached by adding fields to this folder.**

## A recommendation for `frontoffice`

`FeedbackReport` covers what the sketched `frontoffice/Complaint` was for, and covers it better,
because the anonymity and the routing actually work.

**When `frontoffice` is designed, its `Complaint` should not be built.** A parent saying "the bus
driver was rude" is a `TRANSPORT_CONCERN` report, and two systems that both nearly handle it is
worse than one that does.

What `frontoffice` should keep is the **walk-in and telephone log** — who came to the desk, who
rang, what about, who dealt with them. That is a genuinely different record: it is about the
school's front desk doing its job, not about somebody choosing to raise something with the head.

## Deliberately left out

- **Investigation of any kind.** See above. This is the most important omission in the folder.
- **A unique constraint on the reporter.** Somebody may need to report several things, and an
  index that blocked a legitimate second report would be worse than the abuse it prevented. Rate
  limiting is a service concern.
- **Severity scales.** See decision 5.
- **An unmask log.** Revealing a confidential reporter is written to
  [`AuditEvent`](../../audit/AuditEvent.java). Two records of who unmasked somebody is worse than
  one.
- **Auto-routing by keyword.** Reading a description and guessing the category would put a
  harassment report in the facilities queue often enough to matter. The reporter picks the
  category; the school fixes its list when `OTHER` fills up.
- **Notifications.** The acknowledgement clock has nothing to ring. See the parent README — this
  is a real gap here, not a tidy deferral.

## Rules the services must enforce

The six shared anonymity rules in [`../README.md`](../README.md) apply to everything here and are
not repeated.

**Channels**

1. `recipientStaffDocsId` and `backupRecipientStaffDocsId` are two different people, and both must
   exist.
2. `defaultAnonymityMode` is one of `allowedAnonymityModes`; both lists are non-empty.
3. `HARASSMENT_OR_BULLYING` and `SAFETY_CONCERN` channels must allow `ANONYMOUS`. The people who
   most need those channels will not use them otherwise.
4. `additionalReaderStaffDocsIds` is a list of named people, never resolved from a role. A report
   the reporter believed three people could see must not quietly become readable by a fourth.
5. No channel is deactivated while reports on it are still open.

**Reports**

6. A report whose `aboutSubjectDocsId` is the channel's recipient is routed to the backup, with
   `routingNote` saying so. Checked at submission, not at read time.
7. `routedToStaffDocsId` is stored at submission and never re-derived from the channel.
8. The access code is generated from a cryptographic random source, long enough not to be
   guessable, shown **exactly once**, and only its hash is stored. There is no resend path.
9. A `visibleToReporter = false` message is never returned on any reporter-facing path, including
   a full-report export.
10. `acknowledgementDueBy` is set from the channel at submission. Reports past it still at
    `SUBMITTED` are the overdue queue, and somebody is answerable for it.
11. `requiresImmediateAttention`, or an `urgentByDefault` channel, flags the report ahead of the
    queue the moment it arrives.
12. `ACTIONED` and `DISMISSED` require `outcomeNote`; `ESCALATED` requires
    `escalatedToStaffDocsId` and `escalationNote`.
13. Attachments are refused when the channel sets `allowsAttachments` to false — for some
    categories an attachment identifies the reporter more reliably than it helps.
14. Follow-up messages are refused when `allowsFollowUpConversation` is false.
15. There is no uniqueness check on the reporter. Abuse is rate-limited in the service.
