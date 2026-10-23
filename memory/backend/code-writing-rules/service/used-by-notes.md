# Every utils and helper method says who uses it

**Project-wide convention (set 2026-09-09):** every method in a `utils/` or `helper/` file carries
a note at the end of its comment listing the methods that call it.

## The format

```java
    /**
     * Role key -> the role, for one school. Keys are what the seeder checks for.
     *
     * Used by:
     * - completeProvisioning()
     */
    public static Map<String, Role> forSchool(String schoolId) {
```

With several callers, one per line:

```java
     * Used by:
     * - changePlan()
     * - createDraft()
     * - createSubscription()
     * - updateDraft()
```

- The last thing in the comment, after any `@return` or `@param`.
- Method names with `()`, no arguments.
- Listed alphabetically, so adding one is a one-line change rather than a reshuffle.

## Why

A utils or helper method is called from somewhere else by definition, and the file it lives in
does not say where. Without the note, "can I change this?" means searching the whole module
first, and the honest answer to "who breaks if I change this signature" is unknown.

It also shows up problems while you are reading rather than later:

- **One caller** means the method probably should not exist — put that logic inline in the method
  that uses it, under its own `//! step N`.
- **A caller in another module** is a rule 4 problem, visible the moment you write the note.
- **A caller that is itself a utils method** breaks rule 2. The note is where you notice.

See the folder structure and call rules beside this file.

## Keep it true

A stale note is worse than none, because it will be believed. When a call site is added or
removed, the note changes with it. It is one line, and it is the only record of the answer.

## Where it stands

All **45** public methods across `services/core` and `services/plans` — the six utils files and
the two helper files — carry one.
