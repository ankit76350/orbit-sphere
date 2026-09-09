# Backend Repository Code Writing Rules

**Project-wide convention (set 2026-09-09):** how the code under
`backend/src/main/java/com/orbitastra/backend/repositories` is laid out.

## Module structure

Create a separate folder for each module — for example `repositories/core`,
`repositories/plans`.

Inside each module, create a separate folder for **each document/table**:

- `repositories/plans/plandefinition`
- `repositories/plans/schoolsubscription`
- `repositories/plans/subscriptionhistory`

Folder names are the document name in lower case with no separators, the same style as the DTO
folders (`academicyear`, `schoolsubscription`).

## Repository files

Each document/table folder holds that document's repository files:

```
repositories/plans/plandefinition/
├── PlanDefinitionRepository.java
├── PlanDefinitionRepositoryCustom.java
└── PlanDefinitionRepositoryImpl.java
```

Follow the same structure for every document/table. A document with no custom query has just the
one file — `subscriptionhistory/SubscriptionHistoryRepository.java` — and still gets its own
folder.

## Goal

```
Module  →  Document/Table  →  Repository files
```

**Do not place repository files directly inside the module folder.**

## The one thing to be careful of when moving these

Spring Data finds a custom fragment **by name and package**: `XRepositoryImpl` has to sit in the
same package as `XRepository`. If it does not, the app still compiles and still starts — it only
fails when somebody calls that method. So when a repository moves, move all three files together,
and check a call actually works rather than trusting the build.

Recursive component scanning is what makes the subfolders fine: there is no explicit
`@EnableMongoRepositories`, so Boot scans down from the app package and finds them all.

## Known open point

`repositories/identity` and `repositories/institution` still have their files directly in the
module folder, which this rule forbids:

```
identity/RoleRepository.java, RoleRepositoryCustom.java, RoleRepositoryImpl.java
institution/NumberSequenceRepository.java, NumberSequenceRepositoryCustom.java, NumberSequenceRepositoryImpl.java
```

They should become `identity/role/` and `institution/numbersequence/`. Not done yet — noted so it
is a decision rather than an oversight. `core` and `plans` already follow the rule.
