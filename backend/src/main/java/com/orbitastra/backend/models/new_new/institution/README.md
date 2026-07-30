# Institution collection model mapping

This package stores school-level institutional configuration that does not
belong inside the School tenant root.

```text
School
  |
  | School.id = SchoolBase.schoolId
  |
  +--> AffiliationProgramme[]
  |
  +--> NumberSequence[]
```

Both collections extend `SchoolBase`. Every query and referenced operation must
therefore include `schoolId`.

## AffiliationProgramme — `affiliation_programmes`

Represents one education-board affiliation or programme offered by one school.
A school can have multiple programmes, for example:

```text
School
├── CBSE Secondary Programme
├── IB Diploma Programme
└── Cambridge IGCSE Programme
```

| Field | Meaning and mapping |
|---|---|
| `schoolId` | Inherited link to `School.id`. |
| `board` | Standard board family from EducationBoard. |
| `boardName` | Full legal/display name, especially for state, national, or other boards. |
| `programmeCode` | Stable school-scoped business key. |
| `programmeName` | Human-readable programme name. |
| `affiliationNumber` | Identifier issued by the education board. |
| `affiliationValidFrom` | First date of validity. |
| `affiliationValidUntil` | Last date of validity. |
| `mediumOfInstruction` | Programme language such as `ENGLISH`. |
| `status` | Current AffiliationStatus. |
| `gradeCodes` | Optional stable grade codes covered by this programme. |

`gradeCodes` is useful when one school uses different programmes for different
grades:

```text
CBSE programme -> GRADE_1 ... GRADE_10
IB programme   -> GRADE_11, GRADE_12
```

If the school applies one programme generally or grade mapping is not configured
yet, `gradeCodes` remains empty.

The compound indexes enforce:

- one `programmeCode` per school;
- one board-affiliation-number combination per school;
- efficient status and expiry reporting.

## NumberSequence — `number_sequences`

Generates human-readable, school-scoped numbers without duplicate allocation.

```text
ADMISSION_INQUIRY     -> INQ/2026/000001
ADMISSION_APPLICATION -> APP/2026/000001
ADMISSION_OFFER       -> OFFER/2026/000001
STUDENT_ADMISSION     -> ADM/2026/000001
FEE_INVOICE           -> INV/2026/000001
```

| Field | Meaning and mapping |
|---|---|
| `schoolId` | Inherited link to `School.id`. |
| `sequenceType` | Business number being generated. |
| `scopeKey` | Counter scope such as `GLOBAL`, `2026`, `2026-2027`, or `2026-07`. |
| `prefixTemplate` | Text/token template before the number. |
| `suffixTemplate` | Optional text/token template after the number. |
| `nextValue` | Next unused numeric value. |
| `paddingWidth` | Minimum numeric width; `1` with width `6` becomes `000001`. |
| `resetPolicy` | Never, calendar-year, academic-year, or monthly reset behavior. |
| `lastResetAt` | UTC time of the last executed reset. |

The unique identity is:

```text
schoolId + sequenceType + scopeKey
```

Allocation must use one atomic MongoDB `findAndModify` operation with `$inc`.
The operation returns the previous `nextValue` as the allocated value while
persisting the incremented counter. Reading and updating in separate calls can
issue the same number to concurrent requests.

Stored business numbers such as `applicationNo`, `offerNo`, and `invoiceNo`
remain immutable after allocation.

## Validation responsibility

The persistence models keep only required constraints for:

- board, programme code, programme name, and affiliation status;
- sequence type, scope, next value, padding width, and reset policy.

Request DTOs and services validate:

- date ordering and affiliation expiry;
- conditional board names;
- programme-code and grade-code formats;
- existence of configured grade codes;
- positive sequence values and padding widths;
- valid prefix/suffix tokens;
- reset timing and authorization;
- tenant ownership of every operation.

MongoDB indexes and collection validators should be deployed through controlled
database migrations.
