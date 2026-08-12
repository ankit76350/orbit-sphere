# Code Comment & Log Style — keep it simple

**Convention (set 2026-07-21, restated 2026-08-12):** Write every code comment and log message
in plain, everyday language that a beginner can follow. Say plainly what the code is doing at
that point.

## Rules
- Avoid jargon/fancy words like *materialise, de-duplicate, resolve, assemble, persist,
  idempotent, denormalised*. Use plain verbs: **check, save, look up, make sure, skip, create,
  update, remove**.
- Logs describe what is happening right now as a readable sentence, e.g.
  `"Making sure the admission number is not already used"` — not `"5c: admissionNo unique"`.
- Keep step-by-step logging for multi-step flows (it helps trace the sequence). Keep the
  `[methodName]` prefix on each log line so it's clear which function is running.
- Comments explain the *why* in one plain sentence.

## This covers class javadoc too (added 2026-08-12)
The rule is not only for `//` comments. Class and field javadoc on the models must read the
same way. Keep the sentences short and ordinary. Say what the thing is and why it is there.

Do not write clever or literary lines. These were all rewritten out of the finance models
because they sounded good but were hard to read:

| Instead of | Write |
|---|---|
| "the one that gets missed silently eats a family's allowance" | "if we forget to put it back, the student loses discount they should have got" |
| "a balance you cannot explain is a balance you cannot audit" | "if we only keep a total, nobody can check where it came from" |
| "the invoice lines are the only record that cannot drift" | "the invoice lines are always right, so we add them up instead of keeping a separate total" |

Rule of thumb: if a sentence sounds like a line from an article, rewrite it as something you
would actually say out loud to a new developer.

## Example (from `StudentService.createStudent`)
```
[createStudent] Step 1: Checking the request has the required details
[createStudent] Step 2: Preparing the student's guardians
[buildDedupedLinks] Guardian 1 of 2: looking up person by name 'Priya Sharma' ...
[findOrCreate] 'Priya Sharma' is new, so created a new guardian (id ...)
[persistStudent] 5c: Making sure admission number 'ADM-2026-0004' is not already used
[persistStudent] 5d: Saved the student (id=...) with 2 guardian(s)
```

Applies to the whole backend, going forward.
