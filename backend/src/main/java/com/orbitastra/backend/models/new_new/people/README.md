# People domain model mapping

## Package organization

```text
people/
├── staff/          Staff profile, government identity, address and contact
├── organization/   Department and Position definitions
├── employment/     Employment and position-assignment history
├── credential/     Qualifications, licences and compliance credentials
├── leave/          Leave policies, balances and requests
├── recruitment/    Vacancies, candidate applications and interviews
├── onboarding/     New-employee joining cases and checklists
├── performance/    Performance cycles, criteria and assessments
├── development/    Training and professional-development history
└── reviews/draft/  Older review drafts retained only for later cleanup
```

`people` is the parent business domain. Each child package owns one cohesive
workflow and keeps its embedded values and enums beside that workflow.

## Core relationship

```text
Staff
├── EmploymentRecord[]
│   ├── positionDocsId -> Position.id
│   └── managerDocsId  -> Staff.id
├── StaffCredential[]
├── StaffLeaveBalance[]
│   └── StaffLeaveRequest[]
├── StaffGovernmentIdentity[]
│   └── evidenceDocumentDocsId -> DocumentRecord.id
├── StaffAddress (embedded)
└── EmergencyContact (embedded)

Department
└── Position[]
    └── EmploymentRecord[]

LeaveType
└── StaffLeaveBalance[]
    └── StaffLeaveRequest[]

Position
└── JobVacancy[]
    └── RecruitmentApplication[]
        ├── RecruitmentInterview[] (embedded)
        ├── resultingStaffDocsId -> Staff.id
        └── OnboardingCase
            ├── staffDocsId            -> Staff.id
            ├── employmentRecordDocsId -> EmploymentRecord.id
            └── OnboardingTask[] (embedded)

PerformanceCycle
├── PerformanceCriterion[] (embedded)
└── PerformanceAssessment[]
    └── subjectStaffDocsId -> Staff.id

Staff
└── StaffDevelopmentRecord[]
```

All top-level documents extend `SchoolBase`. Every lookup and reference check
must therefore include `schoolId`.

## Staff — `staff`

Stores the employee's stable personal and contact profile. `employeeNo` is
generated with `NumberSequenceType.EMPLOYEE_NUMBER` and is unique within one
school.

Staff does not store:

- salary or compensation;
- login roles or permissions;
- department/designation strings;
- joining or separation history;
- plaintext Aadhaar, PAN, passport, or other government identity numbers.

Those concerns belong to payroll, identity/access, Position/EmploymentRecord,
and StaffGovernmentIdentity respectively.

## EmploymentRecord — `employment_records`

Stores one employment or position period. A staff profile can have multiple
records over time, but only one may have `current = true`.

When a staff member changes position or rejoins:

1. set the previous record's `current` to false;
2. set its `effectiveUntil`;
3. create the new current record.

These writes should be performed transactionally.

## Department — `staff_departments`

Stores school-defined organizational units such as Academics, Finance, HR, and
Transport. Departments may form a hierarchy through `parentDepartmentDocsId`,
and `headStaffDocsId` optionally identifies the department head.

## Position — `staff_positions`

Defines an approved job position inside one Department. EmploymentRecord assigns
a Staff member to the Position. Position stores approved headcount; filled
headcount is calculated from current EmploymentRecord documents to avoid
counter drift.

## StaffCredential — `staff_credentials`

Stores qualifications, licences, certificates, background checks, and other
staff compliance evidence. Credential numbers can be encrypted and searched
using a keyed lookup hash. Expiry reporting uses verification status and
`validUntil`.

## LeaveType — `staff_leave_types`

School-configurable leave policy such as sick, casual, earned, or unpaid leave.
It defines the default annual allowance, paid/unpaid behavior, and carry-forward
limit.

## StaffLeaveBalance — `staff_leave_balances`

One balance exists for each:

```text
schoolId + staffDocsId + leaveTypeDocsId + academicYear
```

The system stores `AcademicYear.name`, never AcademicYear.id. Available leave is
calculated as:

```text
allocatedDays
+ carriedForwardDays
+ adjustmentDays
- usedDays
- pendingDays
```

The inherited optimistic-lock version must be used when approving concurrent
leave requests.

## StaffLeaveRequest — `staff_leave_requests`

Links Staff, LeaveType, and StaffLeaveBalance. `requestNo` is generated with
`NumberSequenceType.STAFF_LEAVE_REQUEST`.

Submitting, approving, rejecting, or cancelling a request must update the
balance consistently. Approval should reserve or consume days and cancellation
should release them using a MongoDB transaction or equivalent atomic workflow.

## StaffGovernmentIdentity — `staff_government_identities`

Stores encrypted domestic or international government identities. The original
identity number is never stored as plaintext.

- `encryptedIdentityNumber` is used for authorized recovery.
- `identityNumberLookupHash` is a keyed hash used for exact duplicate checking.
- `maskedIdentityNumber` is safe for normal UI display.

The encryption key must be resolved through the school's key-vault/KMS
reference.

## JobVacancy — `staff_job_vacancies`

Stores an approved requirement to recruit one or more employees for a Position.
`vacancyNo` is generated with `NumberSequenceType.JOB_VACANCY`. The workflow is:

```text
DRAFT -> PENDING_APPROVAL -> APPROVED -> OPEN -> CLOSED/FILLED
```

The service must calculate how many candidates were hired from the linked
RecruitmentApplication records. It must not maintain a separate filled counter
that can drift from the real data.

## RecruitmentApplication — `staff_recruitment_applications`

Stores one candidate's application for one JobVacancy. `applicationNo` is
generated with `NumberSequenceType.RECRUITMENT_APPLICATION`. Interview history
is embedded because it belongs only to that application.

Candidate name, phone, email, address, and other personal data are stored in
`encryptedCandidateProfile`. `candidateLookupHash` is a keyed hash used to
prevent duplicate applications without exposing those values. After the
candidate is hired, `resultingStaffDocsId` links the application to Staff.

## OnboardingCase — `staff_onboarding_cases`

Tracks the joining checklist after Staff and EmploymentRecord are created. It
can optionally link to RecruitmentApplication. This link is optional because a
school may directly add a staff member without using recruitment.

OnboardingTask is embedded because each task belongs to exactly one onboarding
case. The service derives the overall onboarding status from required tasks and
enforces that linked Staff and EmploymentRecord belong to the same school and
person.

## StaffDevelopmentRecord — `staff_development_records`

Stores training, workshops, certifications, conferences, mentoring, coaching,
and other professional-development activity for one Staff member. It keeps
approval, completion, cost, learning hours, certificate evidence, learned skill
codes, and an optional impact evaluation.

## PerformanceCycle — `staff_performance_cycles`

Defines one performance-review period and its scoring criteria. It stores
`AcademicYear.name`, never AcademicYear.id. PerformanceCriterion is embedded
because the criterion configuration is owned by and versioned with its cycle.

## PerformanceAssessment — `staff_performance_assessments`

Stores one respondent's assessment of one Staff member within one
PerformanceCycle. Respondents may be the staff member, manager, peer, student,
parent, or a school-defined category.

For anonymous feedback, the respondent's document id is omitted and only a
keyed `respondentLookupHash` is saved. That hash prevents duplicate submissions
without exposing identity. Access to raw assessments and aggregate results must
be controlled separately by role and cycle policy.

## Embedded values

`StaffAddress` and `EmergencyContact` are embedded in Staff. They do not have
their own collection ids or tenant fields.

Phone numbers should be normalized to international format and emails to
trimmed lowercase form before persistence.

## Validation responsibility

The persistence models contain only essential required constraints. Request DTOs
and services validate formats, date ordering, allowed employment transitions,
tenant ownership, identity encryption, evidence access, and conditional fields.

The existing `ReviewCycle`, `TeacherPerformanceReview`, `TeacherReview`, and
`StudentReview` files are isolated under `people/reviews/draft`. They remain
older draft models. `PerformanceCycle` and `PerformanceAssessment` replace the
first two for staff performance. The other two belong to a later
feedback/student design review. They have not been deleted because deletion
should happen only after explicitly approving that cleanup.
