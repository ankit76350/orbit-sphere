# Undone Models Added (Frontend Gap Coverage)

**Date:** 2026-07-20
**Scope:** Added MongoDB model classes under `backend/.../models/undone/` for the frontend feature modules that had no backend model yet.
**Method:** Analyzed all 32 frontend modules (`frontend/src/modules/Mod*.jsx` + `App.jsx` registry + `mockData.js`) against existing `models/` (done) and `models/undone/`, then modelled only the genuine gaps — deliberately reusing existing models to avoid redundancy.

---

## What was created — 61 files, 10 new `undone/` folders (project compiles clean)

| Folder | Models | India specifics |
|---|---|---|
| `exams/` | `Exam` (+embedded datesheet), `ExamMarksSheet`, `PromotionBatch` (+`ExamTerm` enum) | — |
| `feeengine/` | `FeeHead`, `FeeStructure`, `ConcessionPolicy`, `ConcessionRequest`, `UpiMandate`, `PaymentGateway`, `FeeReminderLog` (+5 enums) | GST heads, UPI AutoPay mandate, Razorpay/Cashfree/Easebuzz |
| `payroll/` | `SalaryStructure`, `Payslip`, `SalaryIncrement`, `StatutoryFiling` (+3 enums) | PF/ESI/PT/TDS, PF ECR / ESI / Form-16 / 24Q |
| `compliance/` | `ApaarRecord`, `HolisticProgressCard`, `DpdpConsent`, `ComplianceTask` (+3 enums) | APAAR/PEN, NEP-HPC levels (Stream/River/Mountain/Sky), DPDP consent |
| `mess/` | `MessMenu`, `MessKitchenStock` | — |
| `communication/` | `CommBroadcast`, `PtmEvent`, `DiaryEntry`, `MessageTemplate` (+2 enums) | DLT SMS templates |
| `frontoffice/` | `CallLog`, `PostalEntry`, `Grievance` (+8 enums) | — |
| `aihub/` | `AiUsage`, `AiAuditEntry`, `AiApprovedRemark`, `AiGovernanceSetting` (+2 enums) | — |
| `reports/` | `SavedReport`, `ScheduledReport` (+3 enums) | — |
| `audit/` | `AuditLog` | cross-cutting `logAction()` trail |

## Deliberately NOT created (redundancy avoided — the user's explicit concern)
- **Feedback Desk** → already `staff/ReviewCycle` + `TeacherReview` + `StudentReview` + `TeacherPerformanceReview`.
- **Fee invoices/payments/wallet** → already `finance/`. Fee Engine only adds the *config layer* (heads/structures/concessions/mandates/gateways/dunning) that generates the existing `FeeInvoice`.
- **Certificate** (front office) → reuse `document/GeneratedDocument`.
- **Circular** (communication) → reuse `core/Announcement`.
- **AI SafetyAlert** → reuse `security/SecurityIncident`.
- **Library/Inventory** extras → `Book` + `InventoryItem` + `WalletTransaction` already cover them.

## Conventions used (match existing models)
`@Document`, Lombok `@Data/@Builder/@NoArgsConstructor/@AllArgsConstructor`, `@CreatedDate`/`@LastModifiedDate`, `@Indexed schoolId` (tenant scope), `academicYear` where year-scoped, enums in `enums/` subpackages, unique compound indexes where frontend implies uniqueness (e.g. `staffId+month` on `Payslip`, `examId+grade+subject` on `ExamMarksSheet`), cross-refs documented as `// references X.id` comments.

## Status / next step
These are pure model POJOs only — **no repositories/services/controllers/DTOs yet**, matching how the rest of `undone/` is staged. Next step when ready: scaffold repository → service → DTO → controller per module (suggested start: `exams` or `feeengine`). Applies the same validated-DTO + tenant-scoping foundation from [backend-foundation-review.md](backend-foundation-review.md).
