# School ERP database architecture review

## 1. Scope and evidence

This review is based only on the repository contents inspected on 29 July 2026:

- the current backend models, enums, repositories, services, controllers,
  configuration, and tests under `backend/src`;
- the 29 enterprise workspaces and the current ERP modules declared under
  `frontend/src`;
- the 103 existing/planned MongoDB `@Document` classes outside `a_new`.

No traffic, tenant-count, regulatory-jurisdiction, hosting-region, retention,
RPO/RTO, or third-party provider assumptions were made. Those are explicit
architecture decisions still required from product owners.

The proposed target adds 228 persistent document models in 43 domain packages. They are design
artifacts: compilation proves Java/Spring compatibility, not production
correctness, authorization, migration safety, or adequate performance for an
unknown workload.

## 2. Immediate findings

### P0 — resolve before further API expansion

1. **Tenant isolation is not enforced by the current data-access layer.**
   `SchoolBase` supplies `schoolId`, but multiple services call unrestricted
   `findAll()` and `findById()`. A caller that knows an ID could cross a school
   boundary unless every controller independently prevents it. Tenant scope
   must come from the authenticated server context, and repository methods must
   require it.
2. **A live MongoDB credential is committed in
   `application-dev.properties`.** Rotate it immediately, remove it from the
   repository and Git history, and use environment/secret-manager injection.
   Restrict the replacement account by environment, database, network, and
   least privilege.
3. **The current application has no authentication/authorization dependency or
   server security configuration.** The planned `User` collection is not a
   security boundary. Add Spring Security/OIDC, method-level authorization,
   tenant resolution, deny-by-default permissions, and object/field scope
   enforcement before exposing the APIs.
4. **A test deletes application records.**
   `BackendApplicationTests.cleanDuplicateSubdomains()` connects through the
   active Spring profile and deletes duplicate schools. Delete or quarantine
   this migration test, require an isolated ephemeral test database, and make
   the test profile fail closed if a non-local connection is supplied.
5. **A migration mutates production data on every application startup.**
   `ProjectFieldNamingMigration` is triggered by `ApplicationReadyEvent`,
   drops an index, performs collection scans, and generates invoice/wallet/
   receipt numbers with a random two-digit suffix. Move this to a versioned,
   resumable, separately executed migration with checkpoints, dry-run output,
   backups, tenant-scoped uniqueness, and rollback evidence.
6. **Many current unique indexes are global when the business key is tenant
   scoped.** Examples include username, email, role, employee/admission/invoice/
   receipt/wallet numbers, template names, postal references, inventory and
   library identifiers, and transport codes. Recreate these as compound indexes
   beginning with the correct tenant/legal-entity/campus scope.

### P1 — redesign before implementing the planned collections

1. `School` extends `SchoolBase`, so a school record itself has a `schoolId`.
   The model conflates SaaS tenant, school group, legal entity, and campus.
2. `AcademicYear.name` is used as a reference in other documents. Mutable
   labels such as `2026-2027` must not be foreign keys.
3. The current base has no principal audit IDs, schema version, soft-deletion
   metadata, or optimistic locking, and uses time-zone-ambiguous
   `LocalDateTime`.
4. MongoDB has no foreign-key enforcement. Current string references can
   silently become cross-tenant or dangling references unless services validate
   `(tenantId, referencedId)` and deletion policies.
5. Several documents contain arrays that can grow without a safe upper bound:
   daily timetable entries, homework submissions, exam marks, PTM bookings,
   communication details, and similar histories. These create hot documents,
   write conflicts, and a path toward MongoDB's document size limit.
6. Financial state is modeled mainly as mutable totals. Fee payment, wallet,
   payroll, gateway, reconciliation, refund, reversal, settlement, and
   double-entry posting need immutable ledgers, idempotency keys, approval
   separation, and reconciliation references.
7. Government identity, visitor identity, contacts, health, counselling,
   safeguarding, payroll, camera, and communication payloads are not
   consistently classified or encrypted. URLs and provider payloads can expose
   credentials or personal data.
8. Date and time types are inconsistent. Some later models use `String` for
   dates/times (`OnlineClass`, `AiNote`, `CameraRecording`,
   `SecurityIncident`, and review records).
9. Automatic index creation is enabled in the active configuration. Production
   index changes need reviewed migrations, hidden/index-build staging where
   supported, and query-plan validation.
10. The current API and persistence naming is inconsistent (`schoolId`,
    `...DocsId`, human number fields, names used as references). Pick one
    reference convention and one business-number convention before public API
    stabilization.

## 3. Review of all current collection groups

The following table covers every existing or previously planned document. The
recommended replacement packages refer to models in `a_new`.

| Current models | Observed design gap | Target direction |
|---|---|---|
| `School` | Tenant, legal entity, campus, subscription, limits, branding, and configuration are one object; inherited `schoolId` is self-referential. | `saas.TenantAccount`, `saas.TenantSubscription`, `saas.TenantEntitlement`, `institution.OrganizationUnit`, versioned configuration. |
| `AcademicYear` | Display name can be used as a reference; overlap/current-year invariants are not expressed. | `institution.AcademicPeriod` with immutable IDs, hierarchy, dates, lock state, and scoped uniqueness. |
| `SchoolClass` | Grade/class/section concerns and subject/teacher assignments are combined; embedded assignments are hard to history-track. | `institution.AcademicStructureNode`, `academics.CourseOffering`, scheduled occurrences. |
| `DailyTimetable` | One school/day aggregate creates contention and unbounded entries; weak resource-conflict guarantees. | `ScheduleDefinition`, `ScheduleOccurrence`, `ScheduleConstraint`, `SubstitutionAssignment`, `ResourceBooking`. |
| `Attendance` | Attendance event, session, student result, presenter, and intervention state are not separated. | `AttendanceSession` plus one `StudentAttendanceRecord` per learner/session. |
| `Homework` | Embedded student submissions grow and contend; revisions, rubrics, attachments, accommodations, plagiarism, moderation, and resubmission are weak. | `LearningActivity`, `LearnerSubmission`, `AssessmentBankItem`, `GradebookRecord`. |
| `AcademicResult` | Mark entry, moderation, gradebook, publication, report card, appeal, and outcome mastery are compressed. | `AssessmentSession`, `AssessmentAttempt`, `GradebookRecord`, `ReportCard`. |
| `DisciplineLog` | A single mutable log/points record cannot model allegation, chronology, evidence, safeguarding escalation, actions, appeals, and positive recognition. | `conduct.StudentConductCase`, `ConductEvent`, `ConductAction`, `StudentRecognition`. |
| `MedicalRecord` | Student has a singular pointer while the repository permits multiple records; longitudinal profile and encounters are mixed. | `HealthProfile`, `ClinicalEncounter`, `MedicationAdministration`, `ImmunizationRecord`. |
| `Inquiry`, `Admission` | CRM, applicant person, application answers/evidence, reviews, offers, acceptance, and enrollment are not independent state machines. | `AdmissionCycle`, `AdmissionApplication`, `AdmissionReview`, `AdmissionOffer`, `PersonRecord`, `StudentEnrollment`. |
| `Student`, `Guardian`, `StudentAcademicRecord` | Identity/contact data is duplicated; guardian links are embedded; current placement and historical enrollment are coupled; custody and authorization need effective dating. | Canonical `PersonRecord`, `StudentProfile`, `StudentEnrollment`, `GuardianStudentRelationship`, `StudentLifecycleEvent`, `PickupAuthorization`. |
| `Staff` | Person, employment, position, credentials, campus assignment, compensation, and access identity are combined; employee number scope is unsafe. | `PersonRecord`, `EmploymentRecord`, `Position`, `StaffCredential`, `CompensationPlan`, identity assignments. |
| `ReviewCycle`, `StudentReview`, `TeacherReview`, `TeacherPerformanceReview` | String timestamps/statuses, overlapping review types, and no template/version/participant separation. | `PerformanceCycle`, `PerformanceAssessment`, `SurveyDefinition`, `SurveyResponse`; use scoped confidentiality. |
| `FeeInvoice`, `FeePayment`, `StudentWallet`, `WalletTransaction` | Mutable totals and globally unique numbers; allocation, reversal, settlement, chargeback, and accounting trail are incomplete. | `ReceivableInvoice`, `PaymentTransaction`, `PaymentAllocation`, `RefundTransaction`, `SettlementBatch`, `StoredValueAccount`, append-only `StoredValueLedgerEntry`, `JournalEntry`. |
| `AuditLog` | Audit coverage is not guaranteed and tamper evidence/export/data-access semantics are weak. | Append-only `audit.AuditEvent` with actor, tenant, correlation, outcome, target, changed-field metadata, hash chaining/storage controls. |
| `Announcement`, `CommunicationCampaign`, `CommunicationLog`, `MessageTemplate`, `Notification`, `BirthdayGreeting` | Preferences, endpoint verification, conversation, template version, provider retry/dead-letter, redaction, and retention are fragmented; raw payload logging is risky. | `DeliveryEndpoint`, `CommunicationPreference`, thread/message records plus integration message/outbox patterns. Birthday audiences should be derived; store delivery/event evidence, not duplicate birthdays. |
| `ComplianceTask`, `DpdpConsent`, `HolisticProgressCard`, `StudentGovernmentIdentity` | Consent uniqueness prevents multiple purpose/notice/guardian versions; identity can be plaintext; obligation/version/submission/data-quality/retention workflows are incomplete. | `ComplianceObligation`, `ComplianceSubmission`, `DataQualityIssue`, privacy suite, encrypted `PersonRecord`, `ReportCard`. |
| `DocumentApproval`, `DocumentSignature`, `DocumentTemplate`, `GeneratedDocument`, `IdCard` | Templates lack content/version/schema; URLs are source-of-truth; verification identifiers are globally unique; generation and signing are not auditable jobs. | `StoredObject`, `DocumentTemplateDefinition`, `DocumentGenerationJob`, `DocumentRecord`, `IssuedCredential`, workflow approvals. |
| `Exam`, `ExamMarksSheet` | Datesheet and all student marks are embedded; delivery, attempt, moderation, accessibility, publication lock, and appeals are weak. | `AssessmentSession`, `AssessmentAttempt`, `AccommodationPlan`, `GradebookRecord`, `ReportCard`. |
| `ConcessionPolicy`, `ConcessionRequest`, `FeeHead`, `FeeReminderLog`, `FeeStructure`, `PaymentGateway`, `UpiMandate` | Configuration versioning, effective dating, rule evaluation evidence, approval separation, secret references, mandate events, and ledger/account mapping are incomplete. | Keep fee configuration as versioned masters; use `AidAward`, billing/payment records, workflow, ledger accounts, and integration connections with vault references. |
| `CallLog`, `Complaint`, `PostalEntry` | Plain contact/address data, weak SLA/escalation, globally unique postal number, attachment URLs. | `FrontOfficeInteraction`, `ServiceCase`, `PostalRegisterEntry`, `StoredObject`, workflow tasks. |
| `GateEntryLog`, `OutPass`, `Visitor` | Visitor identity is plaintext; gate events lack provider idempotency; authorization/custody, appointment, badge, access scope, and reconciliation are incomplete. | `VisitorProfile`, `VisitAppointment`, `AccessMovement`, `StudentOutPass`, dismissal authorization and security events. |
| `GalleryAlbum`, `GalleryMedia` | Public URLs and mutable counters; no subject/guardian consent evaluation, moderation, accessibility text, storage lifecycle, or publication approval. | `MediaCollection`, `MediaItem`, `MediaPublicationDecision`, `StoredObject`, `ConsentRecord`. |
| `HostelBuilding`, `HostelRoom`, `HostelBed` | These are useful bounded masters but need campus/academic scope, effective capacity, maintenance blocking, and scoped codes. | Retain as facility masters; add `HostelStay`, `HostelLeaveRequest`, `HostelRollCall`, facility/resource links. |
| `InventoryCategory`, `InventoryItem`, `InventoryTransaction` | Global item codes and mutable quantity risk drift; no warehouse/location balance, reservation, batch/serial, valuation, or posting idempotency. | `InventoryMovement` as immutable source, `InventoryStockBalance` as rebuildable projection, facilities/assets/procurement links. |
| `LibraryCategory`, `LibraryBook`, `BookCopy`, `BookIssue` | ISBN/accession/barcode/category uniqueness is not consistently tenant/library scoped; policies, reservation queue, renewals/fines/lost flow need explicit state. | Retain bibliographic/copy/loan masters with tenant indexes; add `LibraryPolicy`, `LibraryReservation`; post fines through billing. |
| `MessHall`, `MessMealType`, `MessMenu`, `MessAttendance` | Menu, recipe, allergen, nutrition, stock consumption, meal entitlement, forecast, and wastage are disconnected. | Add `MealRecipe`, `KitchenStockItem`, immutable `KitchenStockTransaction`; relate attendance to meal/service session. |
| `SalaryStructure`, `SalaryRevision`, `PayrollRun`, `Payslip` | Hard-coded India components, no effective component catalogue/formulas, employee result snapshots, adjustments, approval/posting, payment, or filing evidence; payslip uniqueness omits tenant. | New `payroll` package with component definitions, compensation plans, immutable results, adjustments, payment instructions, and statutory filings. |
| `Driver`, `RouteAssignment`, `RouteStop`, `TransportAllocation`, `TransportAttendance`, `TransportRoute`, `TransportVehicle`, `VehicleLocation`, `VehicleLocationHistory` | Codes are frequently global; assignment validity, route versions/trips, boarding idempotency, geospatial/time-series storage, retention, and consent are incomplete. One attendance model imports the academic attendance enum. | Keep bounded fleet/route masters after re-indexing; add `TransportTrip`, `TransportBoardingEvent`, explicitly created time-series `TransportTelemetryPoint`. |
| `User`, `RolePermissionMapping` | Global username/email/role; one role per user; no field/object scopes, delegations, access reviews, MFA/passkeys, federation, service accounts, session control, or support access. | Full `identity` package. |
| `AiApprovedRemark`, `AiAuditEntry`, `AiUsage` | No use-case risk inventory, model/prompt/source version, evaluation, consent/age gates, cost detail, human review, appeal, incident, or kill switch. | Full `ai` package plus append-only audit and usage metering. |
| `AlumniProfile`, `AlumniEvent`, `AlumniDonation`, `JobPosting`, `MentorshipProgram` | Profile duplicates person data; RSVP is a counter; donation lacks allocation/accounting/receipt; mentorship lacks consent/safeguarding; jobs lack workflow. | Full `alumni` package linked to canonical people, payment, documents, and safeguarding. |
| `SavedReport`, `ScheduledReport` | Query/schema version, authorization snapshot, parameters, delivery, run history, retention, and failure evidence are incomplete. | `ReportDefinition`, `ReportSchedule`, `ReportExecution`; use a separate analytics/read-model plane. |
| `Camera`, `CameraAssignment`, `CameraGroup`, `CameraRecording`, `SecurityIncident` | Stream/recording URLs can expose secrets; dates are strings; retention, device health, evidence integrity, restricted access, consent, and case chronology are incomplete. | `SecurityDevice`, `SecurityEvent`, `SecurityCase`, `EvidenceMedia`, vault references and stored-object retention. |
| `OnlineClass`, `ClassRecording`, `AiNote` | Dates/times are strings; teacher/subject names duplicate masters; meeting/recording URLs can expose secrets; participant attendance, consent, artifact versions, and retention are absent. | `VirtualLearningSession`, `VirtualSessionParticipant`, `LearningSessionArtifact`, `StoredObject`, governed `AiRun`. |

## 4. Target tenancy and organizational boundaries

```text
SaaS Tenant
├── Subscription / entitlements / usage / region
├── Legal entity
│   ├── Campus / branch
│   │   ├── Academic year and terms
│   │   ├── Affiliation programmes (CBSE, ICSE, State, IB, Cambridge)
│   │   ├── Grades / sections / departments / houses
│   │   └── Operational resources
│   └── Ledger / fiscal periods / bank / statutory registrations
└── Cross-campus identities, policies, integrations, and reporting
```

Rules:

- `tenantId` is mandatory on every customer-owned document. The server derives
  it from the authenticated identity; request bodies cannot set or change it.
- A tenant may contain multiple legal entities and campuses. A school is not
  automatically equal to any one of those concepts.
- Cross-campus data is explicit. Campus-scoped callers cannot query a
  tenant-wide collection without a tenant-level permission.
- Academic data references an immutable academic-period ID and optional
  programme ID. Labels may change without breaking relationships.
- Global platform collections (plan catalogue, region catalogue, provider
  catalogue) must be physically/logically separated from tenant collections
  and editable only by platform roles.

## 5. Frontend scope to target-model coverage

| Enterprise workspace | Primary target packages |
|---|---|
| Institution & Master Setup | `institution`, `saas` |
| Identity & Access Studio | `identity`, `audit`, `people` |
| Curriculum & Programmes | `academics`, `institution`, `documents` |
| Timetable & Resource Scheduler | `academics`, `facilities`, `people` |
| Learning & Assessment Platform | `academics`, `learning`, `documents`, `ai` |
| School Governance | `governance`, `documents`, `workflow` |
| General Accounting & Budget | `accounting`, `billing` |
| Procurement & Vendors | `procurement`, `inventory`, `accounting` |
| Facilities, Assets & Maintenance | `facilities`, `procurement`, `inventory` |
| Scholarships & Financial Aid | `aid`, `admissions`, `billing` |
| Recruitment & Onboarding | `people`, `workflow`, `documents`, `identity` |
| Staff Development & Cover | `people`, `academics`, `payroll` |
| Counselling & Wellbeing | `support`, `privacy`, `health` |
| Inclusion, SEN & CWSN | `support`, `academics`, `health` |
| Child Safeguarding | `support`, `documents`, `audit`, `legal` |
| Houses, Sports & Activities | `studentlife`, `facilities`, `conduct` |
| Trips & Excursions | `studentlife`, `transport`, `health`, `billing` |
| Career & University Guidance | `studentlife`, `alumni`, `documents` |
| Early Years & Daycare | `studentlife`, `health`, `dismissal`, `media` |
| Dismissal & Pickup | `dismissal`, `gate`, `transport` |
| Emergency & Crisis Management | `emergency`, `communication`, `gate`, `security` |
| IT Helpdesk & Devices | `it`, `identity`, `facilities` |
| Legal, Insurance & Contracts | `legal`, `documents`, `procurement` |
| Workflow & Form Builder | `workflow`, `documents`, `integration` |
| Integration Marketplace | `integration`, `identity`, `audit` |
| Privacy & Data Governance | `privacy`, `documents`, `audit`, `integration` |
| AI Governance & Evaluation | `ai`, `privacy`, `audit`, `saas` |
| SaaS Customer Operations | `saas`, `identity`, `audit`, `reporting` |
| Public Website & CMS | `cms`, `media`, `documents`, `admissions` |

Current frontend modules are also covered: admissions/SIS (`admissions`,
`people`), fees/wallet (`billing`, `accounting`), payroll (`payroll`), hostel
and mess (`hostel`, `mess`), library/transport/infirmary/security
(`library`, `transport`, `health`, `security`, `gate`), communication/PTM/diary
(`communication`), alumni/media/doc generation/virtual class
(`alumni`, `media`, `documents`, `learning`), feedback/reviews
(`feedback`, `people`), discipline (`conduct`), and reporting/AI
(`reporting`, `ai`).

## 6. Required user roles and permission model

Do not persist a fixed single-role enum on a user. `RoleDefinition` and
`AccessAssignment` support tenant-created roles and multiple scoped grants.
The role catalogue implied by the frontend includes:

- governance: group owner, trustee, governing-body/committee member, group and
  campus administrator;
- academic leadership: principal, vice-principal, academic/curriculum/IB/
  Cambridge coordinator, exam controller, head of department, grade leader,
  timetable coordinator;
- teaching: class/subject/substitute teacher, teaching assistant, early-years
  educator, coach;
- admissions and records: admission officer/counsellor, registrar, front desk,
  records officer;
- finance and people: finance controller, accountant, cashier, auditor, HR,
  recruiter, payroll and procurement officers;
- student support: counsellor, psychologist, SENCO, special educator,
  safeguarding officer, nurse, doctor;
- operations: facilities/asset/store/mess/transport/hostel/library/security
  roles, driver and bus attendant;
- platform: IT/helpdesk, privacy, compliance, AI, integration, SaaS operations,
  support engineer, billing administrator;
- community/external: student, guardian, authorized pickup person, applicant,
  alumnus, vendor, external auditor and government/inspection reader.

Every permission check needs:

`action + resource type + tenant + optional campus/year/class/subject/route/
hostel scope + relationship rule + field classification + record state`.

Maker-checker separation is mandatory for payments/refunds, journal posting,
payroll, admissions decisions, marks publication, certificates, access grants,
AI deployments, privacy fulfilment, and destructive tenant operations.

## 7. Missing workflows now represented by the target

- enquiry → application → review → offer/waitlist → acceptance → enrollment;
- academic-year freeze → structure copy → promotion/retention → balance carry
  forward → publish;
- timetable constraint collection → generation → conflict resolution →
  approval → targeted notification;
- attendance session → learner records → validation → guardian notification →
  acknowledgement/intervention;
- assessment plan → delivery/attempt → mark → moderation → lock → publish →
  appeal/correction;
- fee assignment → invoice → payment → allocation → settlement →
  reconciliation → refund/chargeback/reversal;
- vacancy → candidate → interview → verification → offer → onboarding →
  probation confirmation;
- payroll input freeze → calculate → exception review → approve → post →
  payment → statutory filing;
- requisition → sourcing/bids → approval → purchase order → receipt/inspection
  → three-way match → payment;
- safeguarding concern → immediate safety triage → investigation/referral →
  protection actions → controlled review/closure;
- counselling/inclusion referral → assessment/consent → plan → service
  delivery → outcomes/review;
- dismissal plan/change → guardian verification → handover → reconciliation;
- emergency alert → command activation → accountability → response →
  reunification → after-action review;
- privacy request → identity verification → discovery → review/redaction →
  fulfilment → evidence;
- AI use case → privacy/risk review → evaluation → deployment approval →
  monitoring/human review → incident/retirement;
- tenant trial/contract → provision → migrate → operate/meter → renew/suspend →
  export/offboard/delete.

## 8. Reports, dashboards, and analytics

`ReportDefinition`, `ReportSchedule`, and `ReportExecution` describe controlled
reporting jobs. Do not run large dashboards directly against operational
collections. Publish domain events/outbox records into denormalized read models
or a warehouse and retain drill-through IDs.

Required dashboard families:

- executive: enrollment, retention, attendance, attainment, finance,
  workforce, capacity, risk and safeguarding summaries;
- admissions: source, conversion, SLA, waitlist, yield, intake capacity and aid;
- academic: mastery, growth, distribution, missing work, interventions,
  moderation and predicted outcomes;
- finance: collection, aging, concession, settlement, reconciliation, budget,
  trial balance, P&L, balance sheet and cash flow;
- people: headcount, workload, absence, turnover, payroll, credentials,
  recruitment and training compliance;
- campus: route, boarding, hostel, library, food, health, facilities, assets,
  procurement, inventory, gate and emergency performance;
- assurance: affiliation/accreditation readiness, evidence expiry, submissions,
  audit actions, consent, privacy requests, retention and data quality;
- SaaS: activation, subscription, entitlement use, usage/margin, reliability,
  support access, migrations, backups, storage and AI cost.

Every report definition must store authorization requirements, parameter
schema, query/read-model version, output classification, retention, masking,
delivery policy, and a reproducible execution snapshot.

## 9. Practical AI capabilities and required controls

### Suitable early releases

- document OCR/classification and human-verified extraction for admissions,
  employee onboarding, invoices, identity evidence and certificates;
- data-quality duplicate suggestions and field anomaly review;
- permission-aware natural-language search and analytics over approved read
  models;
- curriculum-grounded lesson/unit plans, differentiated resources, rubrics,
  question drafts and teacher-approved feedback;
- timetable conflict resolution and substitute recommendations;
- communication drafting, translation, tone/accessibility checks and template
  personalization with human approval;
- bank/gateway reconciliation suggestions and finance anomaly queues;
- support-ticket classification, routing, response suggestions and knowledge
  retrieval.

### Later, after outcome validation

- learner tutoring with age gates, guardian/school policy, curriculum grounding,
  source citations, no hidden grading, and teacher escalation;
- early-warning and intervention recommendations using explainable features,
  bias evaluation, human decision ownership and appeal;
- assessment item analysis, misconception clustering, moderated marking
  assistance and predicted outcomes;
- route ETA/optimization, maintenance prediction, meal/stock forecasting and
  staffing/cover forecasting;
- safeguarding or camera-assisted safety signals only as human-reviewed leads,
  never autonomous accusations or disciplinary decisions;
- career/course recommendations with transparent criteria and opt-out;
- policy/compliance evidence mapping and change-impact summaries.

The `ai` package records the use case, risk tier, approved model deployment,
prompt/source versions, each run, evaluation, human review and incidents.
Production implementation must additionally enforce PII minimization,
permission-aware retrieval, consent/age gates, provider retention/residency,
output filtering, rate/cost limits, kill switches, red-team tests and appeals.

Do not store full prompts/outputs indiscriminately. Store hashes and redacted
traces by default; preserve content only where a declared purpose, retention
rule, access policy, and encryption control permit it.

## 10. Integrations

`IntegrationConnection` stores configuration and a secret-manager reference;
`ExternalIdentifier` maps remote IDs; `IntegrationJob` tracks sync executions;
`IntegrationMessage` provides idempotent inbox/outbox/dead-letter processing;
`WebhookSubscription` holds signing configuration references.

Integration families required by the frontend:

- SAML 2.0, OpenID Connect, SCIM, Google Workspace and Microsoft Entra ID;
- OneRoster, LTI, QTI, Google Classroom, Teams, Moodle/Canvas and plagiarism
  providers;
- UDISE+, APAAR/PEN, DigiLocker, CBSE/state-board and other configured
  education exports;
- UPI/payment gateways, virtual accounts, mandates, bank feeds;
- Tally, Zoho Books, QuickBooks, Xero or other accounting exports;
- DLT SMS, WhatsApp Business, email, FCM/APNs and IVR;
- biometric/RFID, GPS/AIS-140, mapping, ONVIF/RTSP, barcode/POS and IoT;
- S3-compatible storage, Drive/OneDrive, eSignature, SFTP, webhooks and BI.

Provider-specific payloads belong in versioned adapters, not core aggregates.
All inbound operations need provider-event idempotency; outbound operations
need an outbox, retry policy, dead-letter state, signature validation and
reconciliation.

## 11. Security, privacy, and compliance design

- Use envelope encryption with per-environment KMS and documented key rotation.
  Use keyed HMAC blind indexes only for equality search on normalized sensitive
  values. Never use unsalted hashes for low-entropy IDs/phones.
- Passwords must use an adaptive password hash through the identity provider or
  Spring Security. Passkeys/MFA secrets and recovery codes require dedicated
  encrypted storage.
- Do not store gateway keys, meeting host tokens, camera stream credentials,
  webhook secrets or database passwords in MongoDB plaintext; store vault
  references.
- Enforce signed, short-lived object access. Malware-scan uploads, validate
  media types, generate safe derivatives, and log access/download/export.
- Mark counselling, health, custody, payroll, safeguarding, government
  identity and legal evidence as restricted fields with purpose-based access
  and immutable audit.
- Make retention configurable by tenant, jurisdiction, category and lifecycle.
  Legal hold must override purge. Deletion jobs need evidence and retry state.
- Treat DPDP, POCSO, RTE/EWS, UDISE+, board affiliation/accreditation, tax and
  labor requirements as versioned `ComplianceObligation` configurations.
  Product configuration must be reviewed for each jurisdiction; a model name
  does not itself create legal compliance.
- Add consent notice/version/purpose/subject/guardian/verification/withdrawal
  semantics. Consent is not a single boolean.
- Maintain tested backups, point-in-time recovery, per-tenant export/restore,
  documented RPO/RTO, disaster exercises, vulnerability management, SBOM,
  SAST/DAST and security monitoring.

## 12. Indexing and MongoDB scaling rules

1. Prefix tenant-owned query indexes with `tenantId`. Add campus/year only when
   they are part of the actual query and uniqueness scope.
2. Avoid separate low-cardinality indexes on every scope field. The base
   annotations are a starting point; the final index set must be derived from
   repository query shapes and `explain()` results.
3. Use partial unique indexes for optional identifiers so missing values do not
   collide. Normalize keys before persistence.
4. Replace growing embedded child arrays with child collections. Embed only
   bounded, atomic value lists such as journal lines, invoice lines, or a small
   approved role-permission set.
5. Explicitly create time-series collections for transport telemetry and
   high-frequency device measurements. TTL belongs on raw telemetry/session/
   transient-message data only, never business/audit/financial records.
6. Candidate high-volume collections include audit events, access movements,
   security events, telemetry, attendance records, communication/integration
   messages, workflow events, AI runs, gradebook records and ledger entries.
   Partition/archive them by measured workload.
7. A shard key cannot be finalized without workload and cardinality evidence.
   `tenantId` must be present; whether it is hashed, combined with time, or
   zoned by residency depends on tenant sizes, range queries and region policy.
   Very large tenants may require a dedicated database/cluster.
8. Do not use cross-collection joins in latency-critical paths. Maintain
   versioned read models and consciously duplicate stable display snapshots
   where historical accuracy or query cost justifies it.
9. Use MongoDB transactions only for short, bounded multi-document invariants.
   Use idempotent orchestration and outbox/saga patterns for long workflows.

## 13. Database constraints and validation

Java annotations are not enough. Before activating a collection, define:

- JSON Schema required fields, type rules, enum/code validation, sensible
  bounds, currency scale, non-empty arrays and allowed conditional fields;
- service invariants such as date order, non-overlapping effective periods,
  debit equals credit, invoice line sums, non-negative balances/capacities,
  one active enrollment per scope, and valid workflow transitions;
- tenant-aware reference validation and delete/archival policy;
- immutable fields after publication/posting and optimistic-version checks;
- idempotency key ownership/expiry and duplicate-event behavior;
- normalized key generation for email/phone/usernames/codes;
- currency as ISO code plus `Decimal128` monetary values with domain-defined
  scale and rounding;
- phone/language/country/time-zone/locale codes using defined standards.

DTO validation belongs at the API boundary. Persistence validators protect
against background jobs, scripts and future services bypassing those DTOs.

## 14. Safe implementation and migration order

### Phase 0 — stop the risks

1. Rotate and purge committed credentials.
2. Isolate tests with Testcontainers or a dedicated ephemeral database; remove
   the deleting test.
3. Disable startup data mutation and production auto-index creation.
4. Add authentication, server tenant context and tenant-scoped repository
   primitives.

### Phase 1 — foundation

1. Create tenant, organization, campus, academic-period and number-sequence
   records.
2. Backfill `tenantId` and immutable reference IDs into existing records.
3. Introduce identity/person/relationship models and field encryption.
4. Add append-only audit and outbox/inbox infrastructure.
5. Build indexes through a versioned migration tool and verify query plans.

### Phase 2 — core SIS and finance

1. Migrate student/guardian/staff into person + profile + relationship +
   enrollment/employment records.
2. Split timetable, homework submissions, exam attempts and attendance into
   bounded aggregates.
3. Introduce invoice/payment allocation/refund/settlement, wallet ledger and
   general ledger; reconcile old totals to new postings.
4. Dual-write behind feature flags, compare counts/totals/hashes, then switch
   reads. Keep rollback until reconciliation is signed off.

### Phase 3 — operational modules

Adopt workflow/documents/integration first, then procurement, facilities,
inventory, payroll, health, hostel, mess, transport, gate/dismissal,
communication and compliance. Migrate one bounded context at a time.

### Phase 4 — analytics and AI

Build governed read models/warehouse feeds, report execution, AI use-case
approval/evaluation and cost metering. Do not train or ground AI on unrestricted
operational collections.

For every migration record: source/target counts, rejected records, checksum,
tenant checkpoint, duration, software/schema version, operator, backup, and
rollback result. Never reuse business-number generators based on random
suffixes; allocate numbers atomically through `NumberSequence`.

## 15. Decisions still required

The code cannot determine these without product/business input:

- shared database versus database-per-tenant thresholds and regulated-region
  placement;
- exact jurisdictions and retention schedules;
- financial accounting/tax/statutory payroll variants by country;
- canonical education interoperability versions and government portal
  contracts;
- maximum tenant, student, telemetry, media, and message volumes;
- RPO/RTO, uptime tiers, archive periods and restore granularity;
- which AI use cases are permitted by age, programme, tenant and jurisdiction.

Finalize these as architecture decision records before freezing collection
validators, shard keys, retention, or provider-specific fields.
