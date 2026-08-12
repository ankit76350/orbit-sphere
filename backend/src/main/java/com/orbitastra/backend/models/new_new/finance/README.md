# School finance model mapping

These models cover the money that moves between families and the school: what is
charged, what is collected, what is discounted, and how all of it lands in the
books.

They are **not** the SaaS subscription models. `plans/billing` bills schools for
the platform; this package bills parents for school fees. The two never share a
collection.

Every collection here extends `SchoolBase` and must be queried with `schoolId`.
Six of them extend `AcademicStudentSchoolBase` and additionally carry
`academicYear` and `studentDocsId`.

## Where these models came from

| Source | Contribution |
|---|---|
| `undone/a_new/billing` | invoice, payment, allocation, refund, settlement, wallet |
| `undone/a_new/accounting` | ledger, journal, fiscal period, bank, reconciliation, budget |
| `undone/a_new/aid` | scholarship programme, application, award |
| `undone/a_working/feeengine` | fee heads, structures, concessions, gateway, mandate, dunning |
| `models/finance` (legacy) | the four collections this package supersedes |

`undone/a_new` was a design sketch and does not compile; its base classes were
deleted in commit `4f31d20`. Nothing was copied from it directly. `tenantId`
became the inherited `schoolId`, `campusDocsId` and `legalEntityDocsId` were
dropped because this platform has no campus or legal-entity boundary, every
`String` status became a typed enum, and every globally unique business number
became a school-scoped compound index fed by `NumberSequence`.

## Relationship overview

```text
FeeHead  (what can be charged)
  |
  v
FeeStructure  (per class, per year, versioned)
  +--> FeeStructureLine[]   what is charged
  +--> FeeInstallment[]     when it is due
        |
        v
FeeInvoice  (one per student, per installment)
  +--> FeeInvoiceLine[]
  |       ^
  |       |  discount comes from exactly one of:
  |       +-- ConcessionRequest  (school discount, approved)
  |       +-- AidAward           (scholarship, funded)
  |
  |<---- PaymentAllocation ----> FeePayment
  |                                 |
  |                                 +--> RefundTransaction
  |                                 +--> SettlementBatch --> PaymentGateway
  |                                 +--> UpiMandate (auto debits)
  |
  +--> FeeReminderLog  (when unpaid)

StoredValueAccount  (wallet)
  +--> StoredValueLedgerEntry[]   append-only, sequenced

Everything above posts to:
JournalEntry --> JournalLine[] --> LedgerAccount
  within FiscalPeriod, matched by ReconciliationRun --> ReconciliationItem[]
  against BankAccount, planned by BudgetPlan
```

## What replaces the legacy `models/finance`

The legacy package is still wired to live repositories, services and
controllers, so it stays in place until a migration runs. `fee_invoices` and
`fee_payments` are reused as collection names, following how `new_new` already
supersedes `students` and `guardians`.

| Legacy | Replaced by | Why |
|---|---|---|
| `FeeInvoice` (7 fields, one `FeeType`) | `FeeInvoice` + `FeeInvoiceLine[]` | one bill can hold many charges, each with its own head, discount and tax |
| `FeeInvoice.discount` (a bare amount) | `ConcessionRequest`, `AidAward` | a discount now has an approver and a funding source behind it |
| `FeePayment.feeDocsId` (one invoice) | `PaymentAllocation` | one payment can clear several invoices, and several payments can clear one |
| `StudentWallet.balance` | `StoredValueAccount` + `StoredValueLedgerEntry` | a balance you cannot explain is a balance you cannot audit |
| `WalletTransaction` (mutable) | append-only ledger with `sequenceNo` | statements read in order, corrections are reversals |
| `@Indexed(unique = true)` on `invoiceNo` | `{schoolId, invoiceNo}` unique | two schools may both have `INV/2026/000001` |
| nothing | `JournalEntry` and the accounting package | fees never reached the books at all |

## config — fee configuration

### FeeHead — `fee_heads`

One thing the school can charge for. The reusable setting, not the charge.

| Field | Meaning |
|---|---|
| `headCode` | Stable key; must not be renamed once invoices exist. |
| `category` | Grouping for reports, such as `TUITION` or `FINE`. |
| `frequency` | How often it is normally charged. |
| `defaultAmount` | Starting value a structure line may override. |
| `taxable`, `taxRatePercent` | GST applicability and rate. |
| `concessionAllowed` | Whether a discount may touch this head. |
| `revenueLedgerAccountDocsId` | Income account to post to; null means placed by hand. |

### FeeStructure — `fee_structures`

The full set of charges one class pays in one year, plus the due dates.

| Field | Meaning |
|---|---|
| `academicYear` | `AcademicYear.name`, never its document id. |
| `structureCode` + `structureVersion` | Stable key and version; unique per year. |
| `classDocsId` | Null means the structure applies to every class. |
| `status` | Only `ACTIVE` may produce invoices. |
| `lines[]` | Embedded `FeeStructureLine` — what is charged. |
| `installments[]` | Embedded `FeeInstallment` — when it is due. |

A mid-year fee change creates `structureVersion + 1` and marks the old version
`SUPERSEDED`. Invoices already issued are never touched.

### ConcessionPolicy / ConcessionRequest — `concession_policies`, `concession_requests`

The policy is the rule; the request grants it to one student. Nothing comes off a
student's fees until a request is `APPROVED`, and the approver must not be the
person who raised it. Amounts are copied onto the request at approval time, so
editing the policy later cannot change a discount already granted.

`concessionPolicyDocsId` null means a one-off discount with no standing policy —
still approved, with `reason` as the only record of why.

## billing — invoices, payments, refunds

### FeeInvoice — `fee_invoices`

One bill for one student for one installment or one-off charge.

| Field | Meaning |
|---|---|
| `invoiceNo` | School-scoped, from `NumberSequence` type `FEE_INVOICE`. |
| `sourceType` + `sourceDocsId` | What raised the bill — a structure, a hostel stay, a library fine, a trip. |
| `classDocsId`, `sectionNo` | Placement as it stood on the billing date. |
| `subTotal` … `grandTotal` | Header totals; must equal the sum of the lines. |
| `allocatedPaymentTotal` | Sum of active allocations; rebuildable, never authoritative. |
| `outstandingAmount` | `grandTotal` minus paid and written off. |
| `lines[]` | Embedded `FeeInvoiceLine`, with head name and tax rate copied in. |

An issued invoice is never edited down or deleted. Void it before any payment
lands, or issue a reversing invoice that points back through
`reversalOfInvoiceDocsId`.

Because `sourceType` exists, hostel, transport, mess, library and activity
charges all bill through this one collection instead of each growing billing
models of their own.

### FeePayment — `fee_payments`

Money received, deliberately not tied to a single invoice.

| Field | Meaning |
|---|---|
| `paymentNo` | Allocated when the payment is first recorded. |
| `receiptNo` | Allocated only once the payment succeeds, so failures burn no receipt. |
| `unallocatedAmount` | Money held but not yet placed against a bill. |
| `idempotencyKey` | Stops a retried gateway callback saving the payment twice. |
| `gatewayProvider` + `gatewayPaymentReference` | Unique per provider where present. |
| `settlementBatchDocsId` | Set once the gateway pays it out to the bank. |

### PaymentAllocation — `payment_allocations`

The record that joins money to bills, and the reason part payments and advance
payments work at all.

| Field | Meaning |
|---|---|
| `feePaymentDocsId` + `feeInvoiceDocsId` | The two sides being joined. |
| `allocationSequence` | Attempt counter, so a reversal and a re-allocation do not clash. |
| `status` | `ACTIVE` counts; `REVERSED` was cancelled; `REVERSAL` did the cancelling. |

Never edited, never deleted. Undoing one writes a second row and leaves both.

### RefundTransaction — `refund_transactions`

Money leaving the school — the highest-risk operation here, so it is always its
own record with its own approval. `requestedByDocsId` and `approvedByDocsId`
must differ. Always points at the `FeePayment` the money came in on, so it can
never send back more than was received.

## wallet — money held for families

### StoredValueAccount — `stored_value_accounts`

| Field | Meaning |
|---|---|
| `ownerType` + `ownerDocsId` + `accountType` | Unique per school; one wallet of each type per owner. |
| `availableBalance` | Spendable now. |
| `heldBalance` | Set aside for something already agreed. |
| `lastLedgerSequence` | Number given to the newest entry; the next entry is this plus one. |

Wallet money is a liability, not income. It becomes income only when used to pay
an invoice.

### StoredValueLedgerEntry — `stored_value_ledger_entries`

Append-only. `sequenceNo` is unique per wallet with no gaps, `balanceAfter` is
stored per entry so a statement line needs no running sum, and
`idempotencyKey` plus the unique reference index mean one source record produces
exactly one entry of a given type. If the account balance and the entries ever
disagree, the entries win.

## accounting — the books

| Collection | Purpose |
|---|---|
| `ledger_accounts` | Chart of accounts as a tree; group headings have `postingAllowed = false`. |
| `fiscal_periods` | Finance periods, separate from the academic year, opened and closed for posting. |
| `journal_entries` | Debits and credits; immutable once `POSTED`. |
| `bank_accounts` | Encrypted number, lookup hash, masked display — never plain text. |
| `reconciliation_runs` | One attempt at matching a bank statement to the books. |
| `reconciliation_items` | One statement line each; own collection because a month can run to thousands. |
| `budget_plans` | Planned income and spend per account per period, versioned. |

`JournalEntry` is where the whole module converges. Its unique index on
`{schoolId, sourceType, sourceDocsId, idempotencyKey}` is what stops one business
event from posting to the books twice, however many times a webhook fires or a
batch job is re-run. `totalDebit` must equal `totalCredit` before a post is
allowed.

## aid — scholarships and funded help

`AidProgramme` differs from `ConcessionPolicy` in one way that matters: it has a
fund. `budgetAmount`, `awardedAmount` and `utilizedAmount` stop the school
promising more help than it set aside, which a plain discount rule cannot do.

`AidApplication` keeps household finances in `encryptedHouseholdAssessment`
rather than as plain fields, so fee-desk users working on invoices never see a
family's income in a list or an export. Verification and decision are separate
people.

`AidAward` is what an invoice line points at. Suspending an award never rewrites
bills that already used it.

## gateway — online collection

| Collection | Purpose |
|---|---|
| `payment_gateways` | One provider connection per school; secrets held as vault key names only. |
| `upi_mandates` | A parent's standing permission to debit, capped at `maximumDebitAmount`. |
| `settlement_batches` | One gateway payout, explaining why 100000 collected arrives as 97876. |

No API key or webhook secret is ever stored in the database. `credentialVaultKey`
and `webhookSecretVaultKey` hold only the names used to look them up.

## dunning — chasing unpaid fees

`FeeReminderLog` holds one row per student per year, not one per reminder. It
exists so reminders escalate along `ReminderChannel` instead of repeating the
same WhatsApp message. `pausedUntil` and `pauseReason` matter as much as the
stage: a family that has agreed a date must stop being chased until then.

## Rules the services must enforce

The models carry only structural constraints (`@NotBlank`, `@NotNull`, index
uniqueness). Everything below lives in the service and request-DTO layer.

**Arithmetic**

1. Invoice line totals must sum to the header totals; `outstandingAmount` equals
   `grandTotal - allocatedPaymentTotal - writtenOffTotal`.
2. Journal `totalDebit` must equal `totalCredit` before a post is allowed, and
   each line carries an amount in exactly one of `debit`/`credit`.
3. Active allocations against a payment must never exceed its amount, nor exceed
   the target invoice's outstanding amount.
4. A refund must never exceed `FeePayment.amount - FeePayment.refundedAmount`.
5. Wallet balances must never go negative, and
   `availableBalance + heldBalance` must match the newest ledger entry.
6. `AidProgramme.awardedAmount` must never exceed `budgetAmount`;
   `AidAward.utilizedAmount` must never exceed `awardAmount`.
7. Fee-structure installment `sharePercent` values must sum to 100 when shares
   are used.

**Immutability**

8. Never edit a `POSTED` journal entry, an issued invoice's amounts, or any
   ledger entry. Correct by reversal, which always leaves both rows.
9. Never reuse or reassign a business number. `receiptNo` is allocated only on
   success.
10. Version fee structures and budgets rather than editing ones already in use.

**Separation of duties**

11. The raiser and the approver must be different people for
    `ConcessionRequest`, `RefundTransaction`, `BudgetPlan`, journal approval, and
    `AidApplication` verification versus decision.

**Scope and idempotency**

12. `schoolId` comes from the authenticated session, never from the request body,
    and every query includes it.
13. Both sides of an allocation, and every document a journal line references,
    must belong to the same `schoolId`.
14. Every gateway callback and batch job must supply an `idempotencyKey`.
15. `academicYear` is resolved server-side against the school's `AcademicYear`
    documents, never trusted from a request; `sectionNo` is verified to exist in
    the referenced `SchoolClass`.
16. Posting requires an `OPEN` (or `SOFT_CLOSED`, for accountants) fiscal period
    whose dates contain the accounting date, and a `LedgerAccount` with
    `postingAllowed = true`.
