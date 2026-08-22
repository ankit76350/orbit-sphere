# procurement — buying things, and paying for them

## Status: models only

The **database models in this package were designed on 2026-08-20** and are finished. What
is here is the collections, the fields, the indexes and the rules the services will have to
enforce.

**No API has been designed for any of it yet, and that is on purpose.** Endpoints,
request and response shapes, permissions on each route and validation at the edge all come
**at the very end**, after every module's models are done. Nothing in this package should be
taken as a decision about how it will be called.

The reason is that an API written next to a model that is still moving gets rewritten every
time the model changes, and the model is the thing every other module reads. Designing the
models first and the endpoints last means each is settled once.

So the "the service checks that..." notes in the javadoc and the numbered rules at the bottom
of this file are **a specification waiting for an implementation**, not a description of code
that exists. They are written down now because the reasoning is fresh; the code that honours
them is a later job.

## What these models answer

These models answer five questions:

1. Who do we buy from, and are we still willing to?
2. What has a department asked for, and did anybody approve it?
3. What did we actually order, from whom, at what price?
4. What turned up, and did we keep all of it?
5. What do we owe, and has it been paid?

## Why this module exists

Before it, **money only flowed one way in the whole system.** `finance` is billing,
payments, concessions, aid and wallets — every one of them money coming *in*. `payroll` was
the only money going out, and it pays people. Nothing anywhere recorded the school **buying
things**, while the mess consumed rice, vegetables, eggs and milk every single day.

Two smaller symptoms of the same gap:

- [`StockMovement.supplierName`](../inventory/StockMovement.java) held a vendor as free
  text, so "Shree Traders", "Shree Traders, Dadar" and "shree trader" were three suppliers.
- [`InventoryItem.usualSupplierName`](../inventory/InventoryItem.java) did the same, and its
  comment said outright that proper vendor records "belong to a procurement module that is
  not built."

Both should now be read as the fallbacks they were always described as.

## Relationship overview

```text
Vendor                          the standing thing: who we buy from
   |                              +--> VendorBankAccount   embedded, one account
   |
   |     ProcurementRequest      a department asks; money is committed HERE
   |        +--> ProcurementRequestLine[]   what is wanted
   |        +--> ProcurementQuote[]         the prices compared
   |              |
   v              v
PurchaseOrder                   one vendor, one document, sent out of the building
   +--> PurchaseOrderLine[]       agreed rate; running received/accepted totals
   |        |
   |        v
GoodsReceipt                    what turned up, and what we kept
   +--> GoodsReceiptLine[]        ordered / received / accepted
            |
            +--> StockMovement (RECEIPT)   <-- the seam into inventory
            +--> StockBatch                <-- for anything with an expiry date
   |
   v
SupplierInvoice                 the bill, checked line by line against the above
   +--> SupplierInvoiceLine[]     billed rate vs ordered rate  <-- catches the money
   |
   v
VendorPayment                   money actually leaves
   +--> VendorPaymentAllocation[]  which bills this settled
   |
   +--> BankAccount (which of our accounts it left)
```

## Models named above from other packages

| Model | Package |
|---|---|
| [`StockMovement`](../inventory/StockMovement.java) | inventory — the stock ledger a receipt writes into |
| [`StockBatch`](../inventory/StockBatch.java) | inventory — created for anything with an expiry date |
| [`StockBalance`](../inventory/StockBalance.java) | inventory — the running total a receipt moves |
| [`InventoryItem`](../inventory/InventoryItem.java) | inventory — what is being bought |
| [`InventoryStore`](../inventory/InventoryStore.java) | inventory — where it is delivered |
| [`InventoryCategory`](../inventory/InventoryCategory.java) | inventory — what a vendor supplies |
| [`BankAccount`](../finance/banking/BankAccount.java) | finance — the school's own account a payment leaves |
| [`FeeInvoice`](../finance/billing/FeeInvoice.java) | finance — the mirror of `SupplierInvoice`, money in |
| [`FeePayment`](../finance/billing/FeePayment.java) | finance — the mirror of `VendorPayment` |
| [`PaymentAllocation`](../finance/billing/PaymentAllocation.java) | finance — the mirror of `VendorPaymentAllocation`, but a collection |
| [`FeeInvoiceLine`](../finance/billing/embedded/FeeInvoiceLine.java) | finance — the precedent for copying a name onto a line |
| [`Staff`](../people/staff/Staff.java) | people — who requested, approved, received, inspected, paid |
| [`Department`](../people/organization/Department.java) | people — who is asking for it |
| [`StaffBankAccount`](../people/staff/StaffBankAccount.java) | people — the collection-not-embedded counter-example |
| [`DocumentRecord`](../documents/DocumentRecord.java) | documents — scans of quotes, delivery notes, bills |
| [`NumberSequence`](../institution/NumberSequence.java) | institution — every number in this package |
| [`TransportAllocation`](../transport/TransportAllocation.java) | transport — the precedent for copying an agreed rate |
| [`ConcessionPolicy`](../finance/config/ConcessionPolicy.java) | finance — the standing-thing / dated-event precedent |

## The collections

| Collection | Purpose |
|---|---|
| `vendors` | One business the school buys from. Set up once, used for years. |
| `procurement_requests` | A department asking to buy something, before any money is committed. |
| `purchase_orders` | The order actually placed with one vendor. |
| `goods_receipts` | What turned up, and what the school kept. |
| `supplier_invoices` | A bill the vendor sent, and what is still owed on it. |
| `vendor_payments` | Money leaving the school and reaching a vendor. |

`VendorBankAccount`, `ProcurementRequestLine`, `ProcurementQuote`, `PurchaseOrderLine`,
`GoodsReceiptLine`, `SupplierInvoiceLine` and `VendorPaymentAllocation` are embedded and
have no collections of their own.

## Five decisions worth explaining

### 1. The request is the control, and it is the model a simpler design would skip

Without `ProcurementRequest`, the first record of a purchase is the purchase order — which
means the first record of a purchase is the school **already being committed to it.** Money
is agreed to at the request, not at the vendor's door.

It also answers a question nothing else can: *what did a department ask for and not get?* A
kitchen that has asked for a new mixer four times and been refused four times is something a
head should be able to see. So `REJECTED` is a state, not a deletion.

### 2. Three quotes on the request, not a tendering module

The earlier sketch had `SourcingEvent` and `VendorBid` as two separate collections. That is a sealed-bid process, and **a school does not run one.** What a school
does is ring three shops, or send a photo of the list on WhatsApp, and write down what each
said.

So `ProcurementRequest.quotes` is an embedded list, compared once at approval and never
queried again. Keeping the *losing* quotes is the whole point: one price proves nothing,
three prices with a choice made between them proves something. And `selectionNote` means
choosing the dearer vendor is an explained decision rather than a suspicious one.

`vendorDocsId` on a quote may be null. The shop that quoted highest and didn't get the order
should not have to be created as a vendor — only the winner will ever be paid.

### 3. Ordered, received and accepted are three different numbers

`GoodsReceiptLine` carries all three, and each pair says something different:

| Comparison | What it means | The complaint |
|---|---|---|
| ordered vs received | 200 kg ordered, 185 kg came | The vendor didn't send enough |
| received vs accepted | 40 kg came, 34 kg kept | The vendor sent rubbish |

Those are two completely different arguments, and a school that cannot tell them apart
cannot make either one. The rejected quantity is the difference, so it isn't stored.

**Only `acceptedQuantity` becomes stock.** Rejected goods were never the school's in any
sense a store balance should reflect — they go back on the lorry they came on.

### 4. The bill is checked line by line, and that is where the money is

`SupplierInvoiceLine` holds what the vendor *claims* — `billedQuantity`, `billedUnitRate` —
next to what was agreed: `orderedUnitRate` and `acceptedQuantity`.

A vendor bills 63 a kilogram when the order said 61.50, on two hundred kilograms. Or bills
for the full 200 kg when 15 went back damp. **Neither is caught by looking at the bill
total**, because the bill total is always internally consistent — the vendor added it up
correctly. It is only caught line by line, against the order and the delivery.

A school without this pays whatever the bill says, because the alternative is a clerk
holding three pieces of paper side by side and doing the arithmetic fourteen times.

`rateVariance` and `quantityVariance` are stored as zero when they agree, not left null, so
that null can keep meaning *nobody has checked.*

### 5. `VendorPayment` is the sixth model in a package described as five

It is here because without it, `SupplierInvoice.amountPaid` is a number somebody types with
nothing behind it. Every running total in this system has to be rebuildable from the records
that caused it, and a paid figure with no payment record breaks that rule at the one place
it matters most — where money leaves.

Its allocations are **embedded**, and that is the one deliberate difference from
[`PaymentAllocation`](../finance/billing/PaymentAllocation.java) on the fees side. A vendor
payment settles a handful of bills, always read together with the payment. A fee allocation
is queried constantly from the invoice direction by hundreds of parents at once and has to
be a document. Here the traffic runs the other way, so an index into the array does the job.

## Two vendors, one bank account

`VendorBankAccount` uses the same three fields as
[`StaffBankAccount`](../people/staff/StaffBankAccount.java) — encrypted value, lookup hash,
masked version. The **lookup hash earns its place here more than anywhere else in the
system.**

Two vendors sharing a bank account is the commonest way a school is defrauded: somebody adds
a second vendor under a different name, paying into the account they already control, and
splits orders between the two so neither looks large. The hash makes that an automatic check
the moment the second vendor is saved, instead of something an auditor might notice a year
later.

It is **embedded** rather than its own collection, unlike `StaffBankAccount`. There is no
group of people who should see a vendor but not their bank details — the whole package sits
behind `PROCUREMENT`. `StaffBankAccount` is separate for the opposite reason: a head of
department has business with a colleague's record and none at all with their account number.

## Two permissions, not one

`AppModule` gained **`PROCUREMENT`** and **`PROCUREMENT_PAYMENTS`**, and this is the more
important of the platform's two permission splits.

A store keeper raises requests and signs for deliveries. Releasing money to a vendor is
somebody else's job. One `PROCUREMENT` module would let the person who orders the goods also
pay for them — which is the arrangement every audit is looking for.

The same reasoning splits `SupplierInvoiceStatus` into `VERIFIED` and `APPROVED`. Verifying
is clerical: do these figures match the order and the delivery? Approving is authority: yes,
pay it. Collapsing them means whoever checks the arithmetic also releases the money.

## States that exist because a failure needs somewhere to live

This is the same rule as `NOT_COMPLETED`, `NOT_RETURNED` and `MISSED` elsewhere in the
system. Four of them here:

| State | Without it |
|---|---|
| `PurchaseOrderStatus.SHORT_CLOSED` | An order where 15 kg never arrived sits at `PARTIALLY_RECEIVED` forever, looking exactly like an order still on its way. |
| `SupplierInvoiceStatus.DISPUTED` | A bill the school refuses to pay ages quietly in the payables list and turns up in a report as money owed. |
| `VendorStatus.BLACKLISTED` | A vendor who cheated the school looks identical to one who simply closed. |
| `VendorPaymentStatus.FAILED` | A bounced transfer looks like a cancelled one, and nobody knows which needs trying again. |

`ProcurementUrgency.EMERGENCY` belongs on the same list. It exists so that buying first and
approving afterwards is a **countable** thing rather than something people do quietly. A
school with many emergency purchases has a planning problem and should be able to see it.

## Direct purchase is allowed, and made to explain itself

`GoodsReceipt.purchaseOrderDocsId` may be null. A cook who buys vegetables at the market
because the delivery failed has bought something the school owns, and refusing to record it
because there was no order would leave the store balance simply **wrong.**

So `directPurchaseReason` is required instead. Stock appearing with no order and no
explanation is the shape most quiet losses take, and a school with many of these should be
able to count them.

## Nothing is deleted, and an issued order is frozen

An `ISSUED` purchase order is **not editable.** Changing what it says after it has gone out
means the school and the vendor are holding two different pieces of paper, and the argument
that follows cannot be settled from either. A change means cancelling and raising a new
order, so it has a date and a reason on it — the same rule an issued
[`FeeInvoice`](../finance/billing/FeeInvoice.java) follows.

Cancelling an accepted `GoodsReceipt` reverses its stock with **compensating rows**, never by
deleting the movements. That is the stock ledger's own rule, and this package does not get an
exemption from it.

## Deliberately left out

- **Statutory tax compliance.** `SupplierInvoice.taxDeductedAmount` is here because it
  changes what actually leaves the bank, which is arithmetic the school cannot do without.
  The platform files **nothing**, issues no certificates and holds no returns. This is the
  same boundary [`payroll`](../payroll/README.md) draws: the school's accountant owns
  compliance, and the platform owns the register.
- **Formal tendering.** See decision 2. The sketched `SourcingEvent` and `VendorBid` were
  rejected rather than deferred — they solve a government procurement problem a school does
  not have, and the quotes on a request cover what a school actually does.
- **Vendor ratings.** A number out of five that one person sets and nobody maintains tells a
  reader nothing. The honest version of "this vendor is trouble" is `SUSPENDED` or
  `BLACKLISTED` with a reason written down.
- **Budgets.** "Has this department spent its allocation?" is a real question and a real
  model, and it needs a budget head structure that does not exist yet. Until then
  `ProcurementRequest.estimatedTotalAmount` is what an approver reacts to.
- **Posting purchases to accounts.** The bookkeeping models were deleted on 2026-08-12. When
  they return, an accepted `GoodsReceipt` and a completed `VendorPayment` are where the
  postings hook in. This replaces the note left in
  [`inventory/README.md`](../inventory/README.md), which said a `RECEIPT` movement was the
  hook — the receipt is the better one, because it knows the price and the vendor.
- **Asset registers and service histories** — **built on 2026-08-21**, in
  [`facilities`](../facilities/README.md). The loop now closes:
  [`AssetRegisterItem`](../facilities/AssetRegisterItem.java) carries `vendorDocsId`,
  `purchaseOrderDocsId` and `goodsReceiptDocsId`, so accepting a `GoodsReceipt` for a
  `NON_CONSUMABLE` item can create the asset rows rather than somebody typing thirty
  microscopes twice. A repair billed by a vendor points back the other way, from
  [`MaintenanceWorkOrder`](../facilities/MaintenanceWorkOrder.java) to a `SupplierInvoice`.
- **Notifications.** "Tell the vendor the order is issued", "warn accounts that a bill is
  due" — none of it is here, and nothing records whether a message went out. `notification`
  is designed **last** by the decision of 2026-08-14. Do not add a `notifiedAt` field to get
  around it.

## Rules the services must enforce

**Vendors**

1. A vendor's `vendorCode` is unique inside the school and is never renamed once orders
   exist.
2. A second vendor whose `bankAccount.accountNumberLookupHash` already exists is refused, or
   at minimum requires a senior confirmation.
3. `gstin` is unique inside the school when present. Two rows with one GSTIN is the same
   business entered twice.
4. `BLACKLISTED` and `SUSPENDED` both require `statusReason`.
5. A vendor with open orders or unpaid bills is never deleted.

**Requests**

6. A `SUBMITTED` request has at least one line; `REJECTED` carries a reason.
7. At most one quote is `selected`, and a selected quote dearer than the cheapest carries a
   `selectionNote`.
8. Status follows the lines' `orderedQuantity`. It is never set by hand.

**Orders**

9. The vendor must be `ACTIVE` at the moment of issue.
10. Line totals add up to the header totals.
11. An `ISSUED` order is never edited. A change means cancel and re-raise.
12. An order is not cancelled once goods have been received against it — it is
    `SHORT_CLOSED`, with a reason.
13. Status follows the lines' received and accepted quantities, never set by hand.

**Receipts**

14. `acceptedQuantity` is never more than `receivedQuantity`, which is never negative.
15. Accepting less than was received requires `rejectionReason`.
16. A batch-tracked item requires `batchNumber` and `expiryDate`.
17. Accepting writes one `RECEIPT` [`StockMovement`](../inventory/StockMovement.java) per
    line in a single operation, and fills in `stockMovementDocsId`. Nothing moves while
    `DRAFT`.
18. Cancelling reverses stock with compensating rows, never by deleting movements.
19. A null `purchaseOrderDocsId` requires `directPurchaseReason`.
20. The parent order's `receivedQuantity` and `acceptedQuantity` totals stay in step with
    these lines and must be rebuildable from them.

**Bills and payments**

21. `vendorInvoiceNo` is unique per vendor, so one bill cannot be entered and paid twice.
22. A bill is not `APPROVED` while any line variance is unexplained.
23. The person who approves is not the person who verified.
24. `DISPUTED` carries a reason. A disputed bill is excluded from overdue reports.
25. Payments are only made against `APPROVED` or `PARTIALLY_PAID` bills, never `DISPUTED`.
26. Allocations add up to the payment `amount`, and no allocation exceeds the named bill's
    `outstandingAmount`.
27. `amountPaid` and `outstandingAmount` on every bill are rebuildable from the allocations
    against it, and completing a payment updates every bill it touches in one operation.
28. `FAILED` and `CANCELLED` payments both carry a reason.
