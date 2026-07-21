# Code Comment & Log Style — keep it simple

**Convention (set 2026-07-21):** Write every code comment and log message in plain, everyday
language that a beginner can follow. Say plainly what the code is doing at that point.

## Rules
- Avoid jargon/fancy words like *materialise, de-duplicate, resolve, assemble, persist,
  idempotent, denormalised*. Use plain verbs: **check, save, look up, make sure, skip, create,
  update, remove**.
- Logs describe what is happening right now as a readable sentence, e.g.
  `"Making sure the admission number is not already used"` — not `"5c: admissionNo unique"`.
- Keep step-by-step logging for multi-step flows (it helps trace the sequence). Keep the
  `[methodName]` prefix on each log line so it's clear which function is running.
- Comments explain the *why* in one plain sentence.

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
