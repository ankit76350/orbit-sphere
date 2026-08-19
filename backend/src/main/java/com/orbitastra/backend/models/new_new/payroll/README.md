# payroll — what staff are paid, and the payslip that says so

Scope, decided on 2026-08-19: **a salary register and payslips.** The system holds each
person's salary structure, runs a monthly payroll, and produces payslips.

It does **not** own statutory compliance. See below — that boundary is the most important
thing in this file.

## Relationship overview

```text
SalaryComponent          the building blocks the school defines
      ^                    Basic, HRA, Transport Allowance, PF, TDS
      |
SalaryStructure          one person's pay, from one date until it changes
      |   +--> StructureComponent[]   the figure agreed for THEM
      |   versioned — never edited, so history is the chain
      |
      v
PayrollRun               one month, whole school
      |   DRAFT -> COMPUTED -> APPROVED -> PAID
      |
      +--> Payslip[]     one person, one month
              +--> PayslipLine[]   every line, names copied in
              +--> DocumentRecord  ../documents/DocumentRecord.java
              +--> BankAccount     ../finance/banking/BankAccount.java
```

### Models from other packages used here

| Model | Lives in | Used for |
|---|---|---|
| [Staff](../people/staff/Staff.java) | `people/staff` | the person being paid, and who approved |
| [StaffLeaveRequest](../people/leave/StaffLeaveRequest.java) | `people/leave` | unpaid leave days that reduce a month's pay |
| [BankAccount](../finance/banking/BankAccount.java) | `finance/banking` | the account salaries are paid from |
| [DocumentRecord](../documents/DocumentRecord.java) | `documents` | the printed payslip, and appointment letters |
| [AppModule](../identity/enums/AppModule.java) | `identity/enums` | the `PAYROLL` permission |
| [NumberSequenceType](../institution/enums/NumberSequenceType.java) | `institution/enums` | `PAYSLIP` |

Named as precedent: [FeeInvoiceLine](../finance/billing/embedded/FeeInvoiceLine.java),
[IssuedDocument](../documents/IssuedDocument.java).

## The collections

| Collection | Purpose |
|---|---|
| `salary_components` | One line that can appear on a payslip. Defined by the school. |
| `salary_structures` | One person's pay, from a date. Versioned, never edited. |
| `payroll_runs` | One month for the whole school. |
| `payslips` | One person, one month. |

`StructureComponent` and `PayslipLine` are embedded.

## Components are rows, not columns

**This is the decision the package turns on.**

The reference sketch had `basicSalary`, `hra`, `da`, `specialAllowance`, `pf`, `esi`,
`professionalTax` and `tds` as fixed columns — on the structure *and* again on the payslip.

A school that pays a **Warden Allowance**, a **Hostel Duty Allowance** or a **Bus Escort
Allowance** then has nowhere to put it, and adding one means changing the database. Every
school has at least one allowance nobody else has.

So the school creates `SalaryComponent` rows, and a salary is a list of them. Same reasoning
as `FeeHead` being a collection rather than a fixed list of fee types.

`ComponentCalculation` matters because most Indian school components are a **share of
basic**, not a figure. House rent allowance at 40% of basic means raising basic carries it
along, instead of somebody editing two numbers for every member of staff.

## Where this stops: statutory compliance

`SalaryComponent.statutory` marks provident fund, employees' state insurance, professional
tax and income tax — **so a payslip can group them and an accountant can be handed the
list.** Not because this system computes them.

It deliberately does not, and the reason is worth stating plainly: rates, wage ceilings and
state slabs change with legislation. **Getting a provident fund ceiling wrong is a
compliance failure, not a bug** — and if the platform computed it, the platform would own
that failure rather than the school's accountant.

So the amounts come from the school. What is deliberately absent:

- PF and ESI contribution rules with wage ceilings
- TDS computed across the year, with declarations and investment proofs
- Professional tax slabs by state
- Gratuity accrual
- Challans, returns, Form 16, filing deadlines

If a school later wants those, they are a compliance module of their own — not fields here.

## Three numbers, not one

| Field | Means | Who asks |
|---|---|---|
| `grossAmount` | earnings | the staff member, and the payslip |
| `netAmount` | gross minus deductions | the bank |
| `employerContributionAmount` | the school's own contributions | the budget |

`SalaryComponentType.EMPLOYER_CONTRIBUTION` is the one people forget. The school's half of
provident fund is a real cost but is **not** deducted from the staff member and does not
reduce take-home pay.

Counting it as a deduction understates what somebody is paid. Leaving it out understates
what the school spends. Reporting one as the other is how a budget goes wrong.

So: **net = earnings − deductions. Cost to school = earnings + employer contributions.**

## Structures are versioned; there is no separate revision model

A raise creates a **new** `SalaryStructure` and closes the old one with an end date.
Structures are never edited, because a raise in April must not change what March's payslip
meant.

That chain **is** the salary history.

The sketch had a separate `SalaryRevision` recording previous and new figures alongside a
mutable structure — the same history kept twice, in two places that can disagree, and the
revision copy is the one nobody updates. So `revisionType`, `revisionReason` and
`approvedByStaffDocsId` live **on the structure that resulted**.

Same call as dropping `UserRoleAssignment` for a role list, and `finePaid` for the invoice.

## Computed and approved are different states

`PayrollRunStatus` separates them, and this is not bureaucracy:

- **COMPUTED** — the figures are worked out. Safe to run again, as often as you like. Which
  matters, because somebody always finds a missing allowance on the first pass.
- **APPROVED** — a person has agreed to them. Now they must not move.

One status covering both would mean **every recalculation silently re-approved itself**.

Once `PAID`, the run is closed for good. A mistake found afterwards is corrected in the next
month, because a bank statement now shows what actually went out and rewriting the paper
would not change that.

## `WITHHELD` lets one person be held without holding the school

A bank account that will not verify, an unsettled advance, somebody who left mid-month.

Without a per-payslip status, holding one person back would mean holding the whole month's
payroll for two hundred people.

## Everything is snapshotted onto the payslip

Component names, types and rates are **copied onto `PayslipLine`**, not read back through
the structure.

A payslip reprinted in 2030 must come out exactly as it did, even after the school renamed
an allowance or stopped paying it. Same rule `FeeInvoiceLine` follows for the fee head name,
and `IssuedDocument` for its template.

`ratePercent` is kept on the line so a member of staff asking *"why is my house rent
allowance this figure"* sees the working, not just the answer.

## Ad-hoc lines, and where unpaid leave shows up

`PayslipLine.adHoc` marks a line that was **not** on the structure: a bonus, overtime, a
deduction for unpaid leave, recovery of an advance. These happen every month and cannot live
on a standing structure.

Marking them means a payslip can be read against the structure and the differences
explained, instead of the two quietly disagreeing.

**Unpaid leave** comes from the staff leave records. `Payslip.unpaidLeaveDays` records the
count and the reduction appears as an ad-hoc deduction line — so the staff member sees what
was taken and why, rather than a smaller number with no explanation.

Every ad-hoc line carries a `reason`. A deduction somebody cannot explain is the one thing
guaranteed to reach the head's office.

## Salary is the most private thing here

More private than anything in a personnel file. `AppModule.PAYROLL` is held well apart from
`STAFF`, because **a head of department needs a colleague's timetable and must never see
their pay.**

A staff member may always read their own payslips.

## Added to the shared enums

- `AppModule.PAYROLL`
- `NumberSequenceType.PAYSLIP`

## Deliberately left out

- **Statutory computation and filing.** See above. The single most important boundary here.
- **Bank payment files.** `PayrollPaymentInstruction` in the sketch submitted transfers to a
  provider and tracked settlement. `Payslip.paymentReference` records what the bank did;
  actually talking to a bank is an integration, not a model.
- **Contract and appointment terms.** Notice period, probation, working hours. Those belong
  with `EmploymentRecord` in `people`, which already exists.
- **Loans and advances as a running account.** `PayslipLine` can recover an instalment as an
  ad-hoc deduction, but a loan with a balance, a schedule and interest is its own small
  ledger. Add it when a school asks.
- **Arrears calculated automatically.** A backdated increment produces arrears; here that is
  an ad-hoc line with a reason. Computing it across months is a rules problem worth doing
  once somebody needs it.
- **Emailing payslips.** `notification`, designed last. Do not add a `sentAt` field.

## Rules the services must enforce

**Access**

1. Everything here requires the `PAYROLL` module, except a staff member reading their own
   payslips.
2. Salary figures never appear in any list served by the `STAFF` module.

**Components and structures**

3. Exactly one component per school has `isBasicPay = true`.
4. A percentage component carries a rate; a fixed one carries an amount. Never both, never
   neither.
5. A component used by any structure is never deleted.
6. Structures are never edited. A change creates a new one and closes the old with an
   `effectiveTo`.
7. One `current` structure per member of staff, and no overlapping date ranges.
8. `monthlyGrossAmount`, `monthlyNetAmount` and `monthlyCostToSchool` must always be
   rebuildable from `components`.
9. Every structure carries an approver. A salary nobody approved is a salary nobody will
   answer for.

**Running payroll**

10. One run per month per school.
11. Computing is repeatable and safe. Only `DRAFT` and `COMPUTED` runs may be recomputed.
12. The approver is never the person who computed the run.
13. Nothing is paid before `APPROVED`.
14. A `PAID` run is never reopened. Corrections go into the next month.
15. Every member of staff with active employment and a current structure gets a payslip.
16. Run totals must always be rebuildable from the payslips.

**Payslips**

17. Lines must add up to `grossAmount`, `deductionAmount` and `employerContributionAmount`,
    and `netAmount` equals gross minus deductions.
18. Component names, types and rates are copied onto the lines at computation time and never
    read back through the structure.
19. Every `adHoc` line carries a `reason`.
20. A payslip is never edited once its run is `APPROVED`.
21. Unpaid leave days come from the staff leave records, and the reduction appears as a
    visible ad-hoc deduction line rather than a silently smaller figure.
