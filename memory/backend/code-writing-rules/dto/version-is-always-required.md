# `version` Is Always Required on a Write

**Project-wide convention (set 2026-09-30).** Every request DTO that carries a `version` marks it
`@NotNull`. There is no such thing as an optional version any more.

```java
/** Optimistic check. Required since 2026-09-30 — leaving it out is 400 VALIDATION_FAILED. */
@NotNull Long version) {
```

## Why

A caller who cannot say what they read cannot be told their read was stale. While `version` was
optional, every write had two behaviours — checked, or last-write-wins — and the second one was the
default for anybody who simply did not send the field. That is the wrong default for a school ERP:
two clerks on one form silently overwrote each other, and nothing in the response said so.

One rule is also cheaper to document and to test than two. `409 CONCURRENT_MODIFICATION` is now
reachable on every write, and unreachable by accident on none.

## What it means in the service

`version` can never be null by the time a service sees it, because every `@RequestBody` in this
project is `@Valid` (measured 2026-09-30: 71 of 72, and the one exception carries no version). So
the guard is a plain comparison, **not** a null check:

```java
// right
if (!request.version().equals(cycle.getVersion())) {
    throw ApiException.conflict("CONCURRENT_MODIFICATION", ...);
}

// wrong — the null branch cannot be reached, and a mutation test will call it out
if (request.version() != null && !request.version().equals(cycle.getVersion())) {
```

## What it means on the frontend and in Postman

Every write has to send a version, so a screen that offers an empty version box is a screen that
cannot complete a write. The tester keeps the box **typed and always sent**: it is pre-filled from
the read, and typing an older number is how `409 CONCURRENT_MODIFICATION` is reached on purpose.
Clearing it now reaches `400 VALIDATION_FAILED`, which is a real refusal worth testing too — so the
box is never disabled. See [[never-disable-in-the-api-tester]].

Every Postman body for a write carries `"version": <n>` in its default body, not only in a
commented case.

## Scope

Applies to `version` on **requests**. A `version` on a *response* is an output and carries no
annotation.

Related: [[closed-sets-use-the-existing-enums]] for the same "one rule, stated on the DTO" shape.
