# notification — to be built last

This package is empty on purpose. There are no models here yet.

**Decided on 2026-08-14: this is the last thing we design.** Every other module —
transport, documents, hostel, mess, library, payroll and the rest — gets designed
before this one. Nobody should pick this up early, even though several other
packages are waiting on it.

When it is finally built, this is where it lives, and this file says what it has
to cover so nobody builds a smaller version of it somewhere else first.

## Why last, and what that costs

Building it last is a choice, not an oversight. The trade is worth writing down
so the decision can be revisited on purpose rather than by accident.

**What we get:** every module that sends messages will already exist by the time
this is designed. Instead of guessing what attendance alerts, fee reminders, exam
results and trip permissions each need, we will be able to look at all of them and
design once against the real list.

**What it costs:** several things are stuck until then.

- Chasing unpaid fees. `FeeReminderLog` and `ReminderChannel` were removed from
  `finance` on 2026-08-12 and parked here, so there is no dunning at all until
  this exists.
- Every module built in the meantime will want to send something and will not be
  able to. Each of those is a place that has to be revisited afterwards.

**The one rule that must hold in the meantime:** when a module needs to send a
message and this package is still empty, say it is deferred. Do not add a
`sendEmail` helper, a `notifications` collection, or a `reminderSentAt` field to
get around it. Those are the small local versions this package exists to prevent,
and each one is harder to remove than it was to add.

## What it will handle

One system for every message the application sends to anybody.

There are two separate lists to keep in mind. One is what the message is about.
The other is how it reaches the person. The system has to handle all of both.

### What the message is about

- fee payment reminders and chasing unpaid fees
- attendance alerts to parents
- exam, result and report card messages
- homework and timetable updates
- admission and inquiry follow-ups
- staff messages, such as leave approvals
- anything else the application needs to send

### How the message is sent

Every kind of notification, with nothing left out:

- push notifications to the mobile app
- email notifications
- in-app notifications, the ones a user sees inside the app in their bell or
  message list
- SMS text messages
- WhatsApp messages
- printed letters sent home, where a school still needs them
- any other way of sending that comes later

Both lists will grow. That is the point of having one system: a new kind of
message, or a new way of sending it, should be a new row, not a new set of models.

### Keep the two apart

The same message often has to go out more than one way. A fee reminder might be a
push notification, an email and an in-app message all at once, and an SMS later if
none of them were opened.

So the design must not tie a message to one way of sending it. What the message
says is one thing. How it is sent is another. Getting this wrong early is what
forces a rewrite later, when somebody asks for the same reminder by email as well.

## The rule

**Every notification goes through the notification system and the notification
collection.** No part of the application sends its own messages or keeps its own
record of what was sent.

This matters because the same questions come up for every kind of message, and we
only want to answer them once:

- which way was it sent, and what do we do if that way fails
- when should it go out, and in whose time zone
- who is allowed to receive it
- has this person asked not to be sent this kind of message
- was it delivered, and was it read
- do not send the same thing twice

If each part of the application solves these on its own, we end up with several
half-finished answers that behave differently, and a parent who has muted one
thing still gets messaged by another.

## Why fee reminders are not in finance

`FeeReminderLog` and `ReminderChannel` were written inside
`finance/dunning/` and removed on 2026-08-12.

Chasing an unpaid fee is a notification job, not a billing job. Finance already
knows who owes money and how much; deciding how to tell them, through which
channel, how often, and when to stop belongs here. Building it inside finance
would have meant finance quietly inventing its own notification system, which is
exactly what this package is meant to prevent.

The old version is in git history if it is useful as a starting point. The ideas
worth keeping from it:

- one row per student per year, not one row per reminder sent
- reminders get stronger each time instead of repeating the same message
- a way to pause chasing a family that has agreed a date to pay, with a note
  saying why

## Rough shape (not decided)

Nothing below is settled. It is only here so the next person has somewhere to
start arguing from.

```text
NotificationTemplate    what a message looks like, per type and per language
Notification            one message for one person, saying what it is about
  └── NotificationDelivery[]   one row for each way it was sent, and what
                               happened to that attempt
NotificationPreference  what a person has agreed or refused to be sent, and
                        which ways they are happy to be reached
```

The split between `Notification` and `NotificationDelivery` is the part worth
getting right. One reminder to one parent is one `Notification`. Sending it by
push, by email and later by SMS is three deliveries under it. Read that way, "was
the parent told" and "did the email bounce" are two different questions with two
different answers, which is what we want.

Things to think about when the time comes:

- one message goes out several ways at once, and by another way later if none of
  them worked
- somebody may need the same message in more than one language
- a school may want its own wording for a standard message
- a person may be happy with email but not SMS, so preferences are per way of
  sending, not just per message type
- in-app notifications also need a read or unread state and an unread count
- messages sent in bulk must not be written one row at a time in a slow loop
- a record of what was sent has to be kept for disputes, so rows are added and
  never edited afterwards

## Until then

If a task needs notifications and this package is still empty, say it is deferred
rather than modelling it here or somewhere else. This whole system is designed in
one go, or not at all.

## When to start

When there is no module left to design. That is the signal, and it was chosen
deliberately on 2026-08-14.

Before writing the first model, do one pass over the finished packages and write
down every message each one needs to send, to whom, and in what circumstances.
Waiting until last is only worth it if that list actually gets used — otherwise
we have paid the cost of waiting and taken none of the benefit.

Keep a running note of the places that had to say "deferred" along the way. Those
are the first things to connect once this package exists.

## Sketches waiting here

Two sets of models elsewhere in `undone` belong to this package and are kept for whenever it
is built:

- `undone/a_working/feeengine/FeeReminderLog` and `enums/ReminderChannel` — fee dunning. The
  live copies under `finance/dunning` were deleted on 2026-08-12; these are the design
  reference. Chasing an unpaid bill is a message, and messages live here.
- `undone/a_working/celebrations/BirthdayGreeting` — a greeting somebody sends, or the system
  sends on their behalf. Its `autoGenerated` and `sentAt` fields are the giveaway: it is a
  notification with a nice subject line, not its own module. Its `anonymous` flag should be
  read against `feedback/enums/FeedbackAnonymityMode` before it is trusted — a boolean cannot
  hold that promise, which is the whole argument in `feedback/README.md`.
