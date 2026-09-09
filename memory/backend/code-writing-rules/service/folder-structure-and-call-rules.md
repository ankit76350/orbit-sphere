# Backend Service Code Writing Rules

**Project-wide convention (set 2026-09-09):** how the code under
`backend/src/main/java/com/orbitastra/backend/services` is laid out, and what may call what.

Each module has its own service folder — for example `services/core`, `services/plans`.

## Folder structure

Each module contains:

| | |
|---|---|
| main service files | the main business logic |
| `utils/` | utility classes used by the main service |
| `helper/` | **only one helper file**, with the common helper methods both the main service and the utils use |

Example: `services/plans/helper/PlanValidator.java`

```
services/plans/
├── PlanDefinitionService.java          main service
├── PlatformSubscriptionService.java    main service
├── SchoolSubscriptionService.java      main service
├── utils/
│   ├── PlanDefinitionServiceUtils.java
│   ├── PlatformSubscriptionServiceUtils.java
│   └── SchoolSubscriptionServiceUtils.java
└── helper/
    └── PlanValidator.java              ONE file, shared by the services and the utils
```

## Method calling rules

**1. Main service → its own utils only.** A main service method may call its own module's utils
methods, and the methods on the module's single helper file.

**2. Utils → helper only.** A utils method may call helper methods. A utils method must **never**
call another utils method.

**3. Helper → independent.** Helper methods must not call other helper methods. Each helper
method stands on its own and is reusable.

**4. No cross-module utility calls.** A module's main service and utils use only their own
module's `utils/` and `helper/`, unless it is explicitly needed.

## Goal

Keep service code simple, modular and easy to understand:

```
Main Service  →  Utils  →  Helper
```

The dependency direction is never reversed, and never chained between utils or between helpers.

## What this rules out, and why it matters

- **A utils method calling another utils method** turns "what does this endpoint do" into a trail
  to follow. Each utils method should be readable on its own.
- **A helper method calling another helper method** is the same problem one level down, and the
  helper is the layer everything else depends on — a chain there is felt everywhere.
- **Merging `utils/` into the helper file** breaks the shape. The helper is for what is genuinely
  common to the whole module; per-service logic belongs in that service's own utils file. One
  giant helper is the thing this structure exists to avoid.

## Settled: what to do when two utils methods need each other

`validatePeriodStartIsTodayOrLater` needed `startOfTodayInSchoolZone`, and both were utils
methods — so one called the other, breaking rule 2.

**Fixed by moving the shared piece out of the module entirely**, into `common/time/Dates` as
`Dates.startOfTodayIn(zone)`. That is the third way out, and usually the right one: if two utils
methods need the same thing and it is not specific to this module, it belongs in `common/`. A
utils method calling `Dates` or `ApiException` is not "calling another utils method" — those are
shared infrastructure, and every utils method already calls them.

It also removed a duplication: `startOfTodayInSchoolZone` resolved a timezone with a UTC fallback
in exactly the same lines `Dates` already had privately, so a rendered date and a date comparison
could have disagreed about the zone.

The other two ways out, for when the shared piece *is* module-specific: inline it into the one
caller, or have the main service call it and pass the result in.
