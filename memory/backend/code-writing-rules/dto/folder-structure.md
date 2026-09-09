# Backend DTO Folder Structure Rules

**Project-wide convention (set 2026-09-09):** how the code under
`backend/src/main/java/com/orbitastra/backend/dto` is laid out.

## The structure

```
dto
 └── module
      └── feature/document
           ├── request
           └── response
```

Create a separate folder for each module, then a separate folder for each document/feature inside
it, then split the DTOs into `request/` and `response/`:

```
dto/core/academicyear/
├── request/
│   ├── AcademicYearCreateRequest.java
│   ├── AcademicYearDatesRequest.java
│   ├── GenerateWeeklyOffRequest.java
│   ├── HolidayCalendarRequest.java
│   └── HolidayRequest.java
│
└── response/
    ├── DayStatusResponse.java
    ├── HolidayCalendarResponse.java
    ├── HolidayView.java
    ├── WeeklyOffGenerateResponse.java
    └── WorkingDaysResponse.java
```

Folder names are lower case with no separators — `academicyear`, `plandefinition`,
`schoolsubscription` — the same style as the repository folders.

## Rules

1. Module → feature/document → request/response → DTO files.
2. **Do not place DTO files directly inside the module folder.**
3. Keep request and response DTOs in separate folders.
4. Use the same structure for every module.
5. Keep the DTOs for one feature/document together.

## Which side a file goes on

`*Request` goes in `request/`, `*Response` in `response/`. A file named neither — a nested view
type like `HolidayView` or `PlanFeatureView` — goes on the side that **uses** it, which in
practice has always been `response/`.

Before moving a folder, check whether anything on one side references a type that will land on
the other: inside one package that reference needs no import, and after the split it does. When
`plans/subscription` was split there were none, which is what made it safe.

## Where it stands today

All 53 DTOs follow the rule — nothing sits loose in a module folder:

```
dto/core/academicyear/{request,response}
dto/core/platform/{request,response}
dto/core/profile/{request,response}
dto/plans/plandefinition/{request,response}
dto/plans/subscription/{request,response}
```

## Two open points from the example

**1. `platform` and `profile` — module or feature?** The rule's own example lists `core`,
`platform`, `plans` and `profile` as top-level modules, but the code has `platform` and `profile`
as features inside `core` (`dto/core/platform/`, `dto/core/profile/`), which matches
`services/core` and `controllers/core`. Left as features under `core`. Say if they should be
promoted to modules.

**2. `holiday` and `workingdays` as their own feature folders.** The example mentions
`dto/core/holiday/` and `dto/core/workingdays/`, but also lists `HolidayRequest`,
`DayStatusResponse` and `WorkingDaysResponse` inside `academicyear/`. Today they are all in
`academicyear/`, because a holiday and a working-day count both belong to an academic year and
the endpoints for them hang off `/academic-years/{name}/...`. Splitting them into three feature
folders is a decision, not done.
