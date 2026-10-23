# Database Call Markers & Two-Step Writes

**Project-wide convention (set 2026-09-03):** two rules that go together, so every place the code
touches the database is easy to find and easy to read.

## Rule 1 — mark every database call

Put a `// TODO:` line directly above every call that reads or writes the database. Say what it
does in two or three plain words: the action, then the thing.

```java
// TODO: read school
School school = schools.findById(schoolId)
        .orElseThrow(() -> ApiException.notFound("SCHOOL_NOT_FOUND", "..."));
```

The words to use:

| The call | Marker |
|---|---|
| `findById`, `findBy…` | `// TODO: read school` |
| a paged `search` | `// TODO: search schools` |
| `existsBy…` | `// TODO: check subdomain exists` |
| `countBy…` | `// TODO: count number sequences` |
| `save` of a **new** object | `// TODO: insert plan` |
| `save` of an object **read from the database first** | `// TODO: update plan` |
| `deleteBy…` | `// TODO: delete holiday` |
| `findAndModify` — reads and writes in one trip | `// TODO: read and update number sequence` |

**Insert and update are different words on purpose.** They both call `save()`, so the code cannot
tell you which one is happening; the marker can. It is the difference between making a row and
changing somebody's existing one.

The marker goes above the **statement**, not above the line the call happens to sit on. When a
statement wraps, keep the marker at the top of it:

```java
// TODO: read subscription
Optional<SchoolSubscription> subscription =
        subscriptions.findBySchoolIdAndCurrentIsTrue(schoolId);
```

## Rule 2 — every write is two steps

**Build the object in one step. Save it in another.** Never build inside the call that saves.

```java
// no
mongo.insert(NumberSequence.builder().schoolId(schoolId).nextValue(1L).build());

// yes
NumberSequence row = NumberSequence.builder()
        .schoolId(schoolId)
        .nextValue(1L)
        .build();

// TODO: insert number sequence
mongo.insert(row);
```

This also rules out a save hidden inside a `return` or a ternary — give it its own line:

```java
// no
return missing.isEmpty() ? 0 : roles.saveAll(missing).size();

// yes
if (missing.isEmpty()) {
    return 0;
}

// TODO: insert roles
return roles.saveAll(missing).size();
```

**Why:** you can see what is about to be written before it is written, the marker has somewhere
to sit, and a debugger can stop on the object with the values still in front of you. A builder
folded into the save gives none of that.

## Applied

Applies to the whole backend, going forward.

**Audited and re-applied 2026-09-15.** The claim above had gone stale: ten writes had the builder
folded into `save(...)`, and six markers said `create` where the table says `insert`. Five were in
`people` (written that day) and five predated it — `AcademicTermService`, `GradingSchemeService`,
`NumberSequenceService` and `SchoolPlatformServiceUtils` (twice). One was worse than a builder in a
save: `closed = EmploymentResponse.fromRecord(employments.save(previous))` hid a write inside a
call, which the "no save in a return or a ternary" line already forbids.

All fixed and re-verified. **A claim like "all N calls comply" rots** — the check is two greps:

```bash
# a builder folded into a write
grep -rnE '\.(save|saveAll|insert)\(\s*\w*\.?builder\(\)' services/ repositories/
# a marker using the wrong word
grep -rn '// TODO: create ' services/ repositories/
```

Related: [[code-comment-and-log-style]] — the same plain-language rule covers the marker text.
