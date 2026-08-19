# inventory — every store in the school, and everything in them

Stationery, rice, vegetables, bedsheets, footballs, microscopes, floor cleaner, light
bulbs. All of it, in one system.

## The problem this has to get right

Your list spans **four things that behave differently**, and a design that treats them
alike gets all four wrong:

| | Example | What makes it different |
|---|---|---|
| **Consumable** | rice, chalk, detergent | issued and gone |
| **Non-consumable** | microscope, football, bedsheet | expected **back** |
| **Perishable** | milk, vegetables, eggs | goes **off** — needs expiry dates |
| **Issuable to a place** | room linen, class textbooks | goes to a room, not a person |

`InventoryItem.itemType` is what carries that difference, and it decides how everything
else behaves.

## Relationship overview

```text
InventoryCategory        the school's own grouping (may nest one level)
      ^
InventoryItem            the thing — NO quantity lives here
      |   itemType, unitOfMeasure, reorderLevel
      |
      +--> StockBalance      how much, in WHICH store   <- one row per item per store
      |         ^
      |         |  a running total of...
      |         |
      +--> StockMovement     the ledger. Append-only. The real record.
      |         |              transferGroupId ties the two halves of a transfer
      |         |
      |         +--> StockBatch    for perishables: lot + expiry + what's left
      |
      +--> StockIssue        only for NON_CONSUMABLE — who has it, did it come back
                |
                +--> FeeInvoice     ../finance/billing/FeeInvoice.java
                                    (finance bills a replacement charge)

InventoryStore           where stock physically is
```

### Models from other packages used here

| Model | Lives in | Used for |
|---|---|---|
| [Staff](../people/staff/Staff.java) | `people/staff` | store keeper, who issued, who approved |
| [Department](../people/organization/Department.java) | `people/organization` | a department receiving stock |
| [Student](../student/Student.java) | `student` | a student receiving stock |
| [HostelRoom](../hostel/HostelRoom.java) | `hostel` | a room receiving linen |
| [HostelBuilding](../hostel/HostelBuilding.java) | `hostel` | a hostel's own store |
| [SchoolClass](../academics/structure/SchoolClass.java) | `academics/structure` | a class receiving textbooks |
| [FeeInvoice](../finance/billing/FeeInvoice.java) | `finance/billing` | where a replacement charge is billed |
| [FeeCategory](../finance/enums/FeeCategory.java) | `finance/enums` | `FINE`, for replacement charges |
| [DocumentRecord](../documents/DocumentRecord.java) | `documents` | an item photograph |
| [AppModule](../identity/enums/AppModule.java) | `identity/enums` | the `INVENTORY` permission |
| [NumberSequenceType](../institution/enums/NumberSequenceType.java) | `institution/enums` | `STOCK_ISSUE` |

Named as precedent: [BookIssued](../library/BookIssued.java),
[StoredValueLedgerEntry](../finance/wallet/StoredValueLedgerEntry.java).

## The collections

| Collection | Purpose |
|---|---|
| `inventory_categories` | The school's own grouping of what it stocks. May nest one level. |
| `inventory_items` | One thing the school keeps. **No quantity.** |
| `inventory_stores` | One place stock is physically kept. |
| `stock_balances` | How much of one item is in one store. |
| `stock_batches` | One lot of a perishable, with its expiry. |
| `stock_movements` | The ledger. Every change. Append-only. |
| `stock_issues` | Something given out that is expected back. |

## Categories may nest one level, and store type is not a substitute

`parentCategoryDocsId` points at **another row in the same collection** — never at itself.
Null means top level.

```text
Kitchen Provisions        parentCategoryDocsId = null
  ├── Grains and Pulses   parentCategoryDocsId = Kitchen Provisions
  ├── Vegetables          parentCategoryDocsId = Kitchen Provisions
  └── Dairy               parentCategoryDocsId = Kitchen Provisions
```

**Why `InventoryStore.storeType` does not replace this.** It was briefly dropped on that
argument and restored the same day, because the argument was wrong twice over:

- **A school with one main store gets no grouping from the store at all.** `storeType` is a
  single value, and that is the common case for a smaller school — exactly where a flat list
  of thirty categories is hardest to use.
- **They are different axes.** A category says what a thing *is*; a store says where it *is
  kept*. Cleaning supplies may sit in three stores and still all be housekeeping spending.
  Neither can stand in for the other.

**Why not a separate `InventorySubCategory` model.** It moves the problem onto
`InventoryItem`, which would then have to point at either a category or a sub-category.
Carrying both lets them disagree, carrying only the sub-category forces a dummy one under
every category, and a flag to choose between them is worse than either. Items are used far
more than categories, so ambiguity there costs more than one nullable field here.

The tree is for grouping only — stock is never counted against a category.

## Quantity does not live on the item

**This is the most important decision in the package**, and it is where the old sketch
went wrong. It kept `stockQuantity` on `InventoryItem`.

That cannot answer the question a school actually has. "We have 70 kg of rice" is useless.
"50 kg in the kitchen store and 20 kg in the main store" is what lets you tell the kitchen
it is running out **while a sack sits in another building**.

So quantity lives on `StockBalance` — one row per item per store.

## The ledger is the truth; balances are for speed

`StockBalance` is a running total. `StockMovement` is the real record, and the balance must
always be rebuildable by adding up its movements. A recompute job has to exist for the day
a bulk operation half-fails.

Same rule `FeeInvoice` follows for `allocatedPaymentTotal`, and the wallet ledger for its
balance.

**Movements are never edited.** A stock ledger that can be tidied afterwards is worthless —
the entire point is that somebody can be asked where 20 kg of rice went and the answer
cannot have been changed. A mistake is a compensating row with an explanation.

`quantityAfter` on each row is what makes the ledger auditable by a person: a store keeper
reading down the page sees the running figure and can spot exactly which row the count went
wrong at, without adding from the beginning.

## Direction comes from the type, not a second field

`quantity` is always positive. Whether stock went up or down comes from `movementType`.

Two fields saying the same thing can disagree, and a row claiming to be a `RECEIPT` that
reduced stock would be impossible to explain. Same reasoning that kept a `direction` field
off this model and a visibility flag off `HealthAlert`.

## A transfer is two rows

`TRANSFER_OUT` and `TRANSFER_IN`, tied by `transferGroupId`.

One row cannot be right: the quantity leaves one store's balance and joins another's, so a
single row would have to belong to two balances at once.

## Wastage and adjustment are not the same loss

Both reduce stock. Kept apart deliberately:

- **`WASTAGE`** — a known loss with a reason. Milk soured in a power cut. A bat snapped.
- **`ADJUSTMENT_DECREASE`** — the count was wrong and **nobody can say where it went**.

A store whose adjustments are large has a problem. Merging the two hides exactly that, so
both need an approver and a reason.

## Batches, and why this batch number is worth capturing

`StockBatch` exists because one quantity cannot answer a kitchen's real question. *"We have
40 litres of milk"* is useless if 25 of them go off tomorrow.

Batches let the oldest be used first and only what has actually expired be thrown away. An
expired batch is **not deleted** — its remainder is written off through a `WASTAGE`
movement, so the loss stays on the record.

**Note the contrast with `ImmunizationRecord`,** where I argued a batch number was not
worth a field. The difference is real:

| | Vaccination batch | Stock batch |
|---|---|---|
| Who received the goods | a clinic, years ago | **this school, today** |
| Where the number comes from | a faded card a parent brought | printed on the carton in the store keeper's hand |
| Would the field be filled in? | rarely | every receipt |
| Who acts on a recall | the health authority | **the school** |

## Consumable issues stay cheap

Issuing chalk is **one `StockMovement`** and nothing else.

Issuing a microscope is a movement **plus** a `StockIssue`, because somebody has to give it
back. Putting return fields on every bag of rice to serve the microscope case would be the
wrong trade.

`quantityReturned` makes partial returns sayable — eight bedsheets back out of ten is the
normal end-of-term outcome, and a status alone could not express it.

`NOT_RETURNED` matters for the same reason `ConductActionStatus.NOT_COMPLETED` does: with
only `ISSUED` and `RETURNED`, everything lost would sit as `ISSUED` forever, so the list of
things gone would look identical to the list of things in use.

## Replacement charges take the route already built

`replacementCharge` + `feeInvoiceDocsId`, billed under `FeeCategory.FINE`. Exactly what a
library fine and a conduct fine already do. Whether it has been paid is finance's answer —
not copied here.

## This is where the mess kitchen stock went

When `mess` was designed, `KitchenStockItem`, `KitchenStockTransaction` and `MealRecipe`
were left out with the note *"that is the inventory module's problem, and modelling half of
it here would mean two stock systems."*

This is that module. Kitchen provisions are `InventoryItem` rows in a `KITCHEN` store, food
cooked for a meal is a `CONSUMPTION` movement, and spoilage is `WASTAGE`.

## Deliberately left out

- **Procurement.** Vendors, purchase orders, quotations, goods-receipt notes, supplier
  invoices — `undone/a_new/procurement` has seven models for it. A `RECEIPT` movement
  records what arrived, with `supplierName` and `supplierReference` as plain text, which is
  enough to run a store. Buying properly is its own module.
- **Individually tracked assets.** Thirty microscopes are a quantity of thirty here, not
  thirty asset tags with service histories and depreciation. `undone/a_new/facilities` has
  `AssetRegisterItem` and `MaintenanceWorkOrder` for that, and it is a genuinely different
  problem: an asset is about one object's life, not about how many there are.
- **Recipes and meal costing.** `MealRecipe` ties dishes to ingredient quantities. Worth
  having one day; it needs the kitchen to maintain a recipe database first.
- **Stock valuation reports.** Every batch carries its `unitRate` and every receipt its
  own, so FIFO valuation is computable. The report is a report, not a model.
- **Posting purchases to accounts.** Stock bought is money out, and the bookkeeping models
  were deleted on 2026-08-12. When they come back, a `RECEIPT` is where the posting hooks
  in.
- **Telling somebody stock is low.** `reorderLevel` makes the condition detectable.
  Sending the message is `notification`, designed last. Do not add a `alertSentAt` field.

## Rules the services must enforce

**Setup**

1. `requiresBatchTracking` must be true for every `PERISHABLE` item.
2. An item's `unitOfMeasure` is never changed once movements exist.
3. A category or store still in use is never deleted; a store still holding stock cannot be
   closed.
4. A category's parent chain must not loop back on itself. A category that is its own
   grandparent would hang any report walking the tree.

**The ledger**

5. `quantity` is always positive. Direction comes from `movementType`.
6. Stock never goes below zero. An issue larger than `quantityAvailable` is refused, not
   allowed to go negative.
7. `StockBalance` must always be rebuildable from `StockMovement`, and a recompute job
   exists.
8. `quantityAvailable` always equals `quantityOnHand` minus `quantityReserved`.
9. Movements are never edited or deleted. A mistake is a compensating row with an
   explanation in `remarks`.
10. A transfer writes both a `TRANSFER_OUT` and a `TRANSFER_IN` with one `transferGroupId`,
   in a single operation.
11. `WASTAGE`, `ADJUSTMENT_INCREASE` and `ADJUSTMENT_DECREASE` all require a `reason` and
    an `approvedByStaffDocsId`.

**Batches**

12. A batch-tracked item's movements must name a `stockBatchDocsId`.
13. The batches of one item in one store must add up to that balance's `quantityOnHand`.
14. The oldest unexpired batch is drawn on first.
15. An expired batch is never issued; its remainder is written off as `WASTAGE`.

**Issuing**

16. Only a `NON_CONSUMABLE` item opens a `StockIssue`. A consumable issue is a movement
    alone.
17. `quantityReturned` never exceeds `quantityIssued`.
18. A return writes a `RETURN` movement back into the store it came from.
19. Writing off as `NOT_RETURNED` requires an approver and a reason.
20. A replacement charge is billed by finance under a `FINE` head. This package never
    touches an invoice directly.
