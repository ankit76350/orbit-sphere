# Daily timetable persistence contract

`DailyTimetable` stores the complete schedule of one school for one date. These
rules are mandatory when its repository and service are implemented.

## 1. Create and identify entries

- Generate an immutable MongoDB ObjectId for every embedded entry before
  insertion, for example with `new ObjectId().toHexString()`.
- Store it as the embedded entry's `_id`; MongoDB does not auto-generate `_id`
  values for embedded documents.
- Keep each embedded `_id` unique within its owning daily document.
- Reject duplicate embedded ObjectIds before writing.
- Use `schoolId + date` to locate the top-level document; the compound unique
  index guarantees one timetable document per school per date.

## 2. Update one entry atomically

For a small edit, do not load the daily document, modify the Java list, and call
`repository.save(...)`. Use a targeted `MongoTemplate` update with:

- query criteria for `schoolId`, `date`, `entries._id`, and the expected
  inherited `version`;
- `$set` only for the permitted `entries.$[entry].<field>` paths;
- an array filter matching `entry._id`;
- an atomic increment of the inherited `version`.

Conceptual update:

```text
match:
  schoolId = requested school
  date = requested date
  entries._id = requested entry ObjectId
  version = version received by the client

update:
  $set entries.$[entry].teacherDocsId = replacement teacher
  $inc version = 1

array filter:
  entry._id = requested entry ObjectId
```

Exactly one document must be modified. A zero result means the document/entry
does not exist or another request changed the version; return a not-found or
optimistic-lock conflict instead of retrying with stale data.

Use `$push` with a new unique embedded ObjectId to add one entry and `$pull` by
that `_id` to remove one entry. Full-document replacement is allowed only for an explicit
"replace complete day" operation and must still require the expected version.

## 3. Validate before writing

For the proposed final list, validate all of the following within the same
school and academic year:

- `startTime` is earlier than `endTime`;
- `classDocsId` belongs to the school and academic year;
- `sectionNo` exists in the selected `SchoolClass`;
- a `LESSON` has a valid `subjectCode` and `teacherDocsId`;
- breaks and assemblies do not require subject or teacher links;
- the same class/section has no overlapping periods;
- the same teacher has no overlapping lessons;
- the embedded `_id` is a valid ObjectId, immutable, and unique;
- `academicYear` is derived from `date`, not trusted from the request;
- no timetable is created on a configured holiday or weekly off.

Run conflict validation again immediately before the atomic write. If strict
cross-request conflict prevention is required, perform validation and update in
a MongoDB transaction or maintain a coordinator-level write lock for that
school/date.

## 4. Monitor BSON document size

MongoDB has a hard 16 MiB BSON document limit. Measure the stored document with
MongoDB's `$bsonSize` expression and publish the value as an application metric.

Recommended thresholds:

- warn when a daily document reaches 10 MiB;
- reject entry additions or full-day replacements that would exceed 12 MiB;
- never rely on the 16 MiB hard failure as normal validation.

Do not persist a derived `estimatedSize` field because it can become stale.
Measure the real BSON document after material changes and periodically scan the
largest daily timetable documents.

## 5. Keep the aggregate bounded

Only schedule information belongs in `DailyTimetable`:

- class, section, period and time;
- slot type, subject and assigned teacher;
- a small display label.

Attendance sessions and records, homework and submissions, exams, marks,
report cards, documents, and communication data remain in their own
collections. Store only their necessary references outside this aggregate.
