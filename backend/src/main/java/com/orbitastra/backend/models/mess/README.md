# mess — what is served, and who ate

A small package that does two jobs: tell families what their child is eating, and tell
the kitchen how much to cook.

It is deliberately **not** a billing system. See below.

## Relationship overview

```text
MessHall              where food is served
MessMealType          which meal, and its serving window
   |
   +--> MessMenu           what is served, per hall, per meal, per day
   |
   +--> MessAttendance     who ate
             ^
             |  built from an ACTIVE allocation
             |
       HostelAllocation    ../hostel/HostelAllocation.java
             |               carries monthlyMessFee — the charge lives there
             v
        FeeInvoice         ../finance/billing/FeeInvoice.java
```

### Models from other packages used here

| Model | Lives in | Used for |
|---|---|---|
| [HostelAllocation](../hostel/HostelAllocation.java) | `hostel` | who is a boarder, and where the mess charge lives |
| [Student](../student/Student.java) | `student` | the person eating |
| [Staff](../people/staff/Staff.java) | `people/staff` | the mess manager, and who marked attendance |
| [IdentificationMethod](../common/enums/IdentificationMethod.java) | `common/enums` | how a tap at the mess door was captured |
| [FeeCategory](../finance/enums/FeeCategory.java) | `finance/enums` | `MESS` |
| [FeeInvoice](../finance/billing/FeeInvoice.java) | `finance/billing` | where the monthly mess charge ends up |
| [HealthProfile](../health/HealthProfile.java) | `health` | a child's allergy alerts, compared against a menu |
| [AppModule](../identity/enums/AppModule.java) | `identity/enums` | the `MESS` permission |

## The collections

| Collection | Purpose |
|---|---|
| `mess_halls` | Where food is served. Most schools have one. |
| `mess_meal_types` | Which meals the school serves, and when. |
| `mess_menus` | What is served, per hall, per meal, per day. |
| `mess_attendance` | One child at one meal. |

## Attendance does not drive billing

**This is the decision that shapes the whole package.**

Mess charges are a **fixed monthly amount** on `HostelAllocation.monthlyMessFee`. A child
who skips breakfast is not refunded; a child who eats twice is not charged twice.

Per-meal billing sounds fairer and is a trap. It means every single meal has to be
captured accurately or a family is overcharged — so a card reader that misses a tap stops
being a rough headcount and becomes a billing dispute. A fixed charge with an approximate
count is the trade almost every boarding school actually makes.

So what is `MessAttendance` *for*?

1. **The kitchen needs numbers.** How much to cook tomorrow.
2. **A child missing several meals in a row is worth noticing.** It is often the first
   visible sign that something is wrong, and it shows up here before anywhere else.

## Meal types are a collection, not an enum

Breakfast, lunch and dinner feels universal until you look. One school serves a morning
snack and an evening one; a school with younger boarders adds bedtime milk; another state
runs different timings entirely.

`servingFrom` and `servingTo` are what let a card tap at the mess door be attributed to
the right meal without anybody picking from a list.

## The menu is words, not recipes

The reference sketch tied each dish to a recipe with ingredients, quantities and allergen
codes.

That belongs to kitchen stock management, which is a different job from telling a family
what is for lunch — and a kitchen that must maintain a recipe database before it can
publish a menu will publish **no menu at all**.

So `items` is a plain list of dish names, and `containsAllergens` is plain words. That
last one matters because a child's allergy lives in
[HealthProfile](../health/HealthProfile.java) as readable text too, and a warden
comparing the two needs something they can actually read.

## Deliberately left out

- **Kitchen stock, recipes and provisions.** `KitchenStockItem`,
  `KitchenStockTransaction` and `MealRecipe` from the sketches are all inventory: items,
  quantities on hand, reorder levels, batches. That is the inventory module's problem,
  and modelling half of it here would mean two stock systems.

  **That module now exists.** See [inventory](../inventory/README.md). Kitchen provisions
  are `InventoryItem` rows in a `KITCHEN` store, food cooked for a meal is a
  `CONSUMPTION` movement, and spoilage is `WASTAGE`. Recipes are still not modelled.
- **Per-meal billing.** See above. Add it only if a school genuinely wants to charge day
  scholars per meal, and design it then.
- **Day scholars eating in the mess.** `MessAttendance` is built from a hostel
  allocation, so a day scholar has no row and nowhere to be charged. Real at some
  schools; needs a mess subscription of its own.
- **Nutrition and calorie tracking.** The sketch had calories per serving. A school
  kitchen will not maintain it.
- **Feedback on the food.** Worth having, and it belongs with a general feedback module
  rather than as a field here.

## Rules the services must enforce

1. One menu per hall, per meal, per day.
2. A tap is attributed to the `MessMealType` whose serving window contains it.
3. One attendance row per child per meal per day.
4. A row is only created for a child with an `ACTIVE`
   [HostelAllocation](../hostel/HostelAllocation.java) that day.
5. A child on approved hostel leave is recorded `ON_LEAVE`, never `ABSENT` — the school
   knows where they are.
6. Nothing in this package writes to an invoice. The mess charge is billed from the
   hostel allocation.
7. Publishing a menu to families requires `published = true`; an unpublished menu is the
   kitchen's working draft.
