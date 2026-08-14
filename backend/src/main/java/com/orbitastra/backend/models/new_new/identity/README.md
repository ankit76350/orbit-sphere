# identity — who can sign in, and what they may do

These models answer two questions the rest of the system keeps asking and could
not answer before:

1. **Who is this?** Somebody signed in. Which staff member, parent or student are
   they?
2. **May they do this?** They are trying to approve a concession. Are they
   allowed?

Every other package already assumes this exists. `ConcessionRequest` says
*"links to the staff identity that decided it"*. `FeeInvoice` has
`voidedByDocsId`. `AidApplication` has `verifiedByDocsId` and `decidedByDocsId`.
None of those had anything to point at until now.

## Relationship overview

```text
Staff / Guardian / Student        (the person, already exists)
        ^
        |  personType + personDocsId
        |
UserAccount   (the login — one per person)
        |
        +--> roleDocsIds[]  ------------> Role
        |                                   +--> RolePermission[]
        |                                         module + actions + scope
        |
        +--> AuthSession[]   one per signed-in device
```

## The collections

| Collection | Purpose |
|---|---|
| `user_accounts` | One person's login, and the roles they hold. Never a person on its own — always points at a Staff, Guardian or Student. |
| `roles` | A named job and everything somebody doing it may do. |
| `auth_sessions` | One person signed in on one device. |

`RolePermission` is embedded in `Role` and has no collection of its own.

## How a permission check works

Somebody clicks "Approve" on a concession request:

```text
1. Read the session          -> which UserAccount?
2. Account status ACTIVE?    -> no: stop
3. Load the Roles named in account.roleDocsIds
4. Add all their RolePermission lines together
5. Is there a line for FEES_CONCESSIONS containing APPROVE?
      no: stop
6. Read that line's DataScope and narrow the query
      SCHOOL   -> any request in the school
      ASSIGNED -> only their assigned classes
      OWN      -> only their own records
7. Finally, the rule the concession model already states:
      the approver must not be the person who raised it
```

Step 5 is the whole reason `APPROVE` is a separate action from `CREATE` and
`EDIT`. A fee desk clerk gets `CREATE` on `FEES_CONCESSIONS`; the principal gets
`APPROVE`. Neither can do the whole thing alone, which is what every
maker-checker javadoc in `finance` has been promising.

## Three decisions worth knowing

**Role is a collection, not an enum.** The old `undone/user/Role` was a fixed
list — SUPER_ADMIN, PRINCIPAL, ACCOUNTANT and so on. Schools do not agree on
their own job titles. One has a Vice Principal, another has a Headmistress and a
Fee Desk Clerk, a third wants a Warden who sees hostel students only. A fixed
list forces all of them into the same few names. `systemManaged` protects the
roles the platform ships with, so a school cannot delete its way out of having an
administrator.

**Roles are a plain list on the account, not their own collection of grant
records.** A separate `user_role_assignments` collection was designed and then
dropped on purpose. The list keeps a permission check to a single read of the
account, and the account document alone says everything about what somebody may
do.

What that gives up is a per-role history. `updatedByDocsId` on the account still
says who last changed it, but not who added one particular role, when, or why. A
role also cannot end on a date by itself, so "acting head for two weeks" has to be
taken away by hand. If either of those turns out to matter, the answer is to bring
the assignment collection back rather than to grow this list into embedded
objects.

**Fees are four modules, not one.** `FEES_BILLING`, `FEES_PAYMENTS`,
`FEES_CONCESSIONS`, `FEES_AID`. A single FINANCE module could not express "may
raise bills and take money, but may not allow a discount", which is the most
common split a school actually asks for.

## Secrets

Two fields hold a one-way hash and nothing else:

- `UserAccount.passwordHash` — made with a slow algorithm such as bcrypt or
  argon2. The real password is never saved, never logged, never sent back out,
  not even to an administrator. Getting somebody back in means resetting it, not
  reading it.
- `AuthSession.refreshTokenHash` — a stolen copy of this collection cannot be
  used to sign in as anybody.

Same reasoning in both places: keep enough to check a value somebody hands us,
and not enough to produce it ourselves. `BankAccount` does the same thing with
account numbers.

`AuthSession.refreshTokenHash` is unique **without** `schoolId`, unlike almost
every other index in `new_new`. A token has to be unique everywhere, not just
inside one school.

## Not tied to an academic year

`UserAccount`, `Role` and the rest sit on `SchoolBase`, not
`AcademicStudentSchoolBase`. A teacher signing in during April 2027 uses the same
account they used in 2026. Making accounts academic-year-scoped would force every
person in the school to be re-created each April, which is the same reason
`UpiMandate` stayed off the academic year.

## Deliberately left out

- **Two-factor devices.** `UserAccount.twoFactorRequired` is a flag with nothing
  behind it yet. Modelling passkeys, authenticator apps and recovery codes is a
  package of its own, and it should be designed when 2FA is actually built rather
  than guessed at now.
- **Sign in with Google / Microsoft.** Needs a place to store which outside
  account is linked to which login. Not built, because no school has asked yet.
- **Access reviews.** Periodically asking a head to confirm their staff still
  need their access. This is a compliance feature for large organisations and
  would be noise in a school.
- **Support engineer access.** Letting the platform's own staff into a school's
  data for a support ticket, with approval and a time limit. Real, but it belongs
  with the platform side in `plans`, not here.

## Known limit: one account is one school

`UserAccount` extends `SchoolBase`, so an account belongs to exactly one school.
That is right for staff and for almost every parent.

It is wrong in two cases. A parent with children at two schools on this platform
needs two logins. And a platform operator who supports many schools cannot be
modelled here at all — that is a different kind of account and belongs with the
SaaS side in `plans`, not with a school's own users.

Both are worth solving later. Neither is worth complicating every permission
check for today.

## What this replaces

`models/undone/user` — `User`, `Role`, `RolePermission`, `RolePermissionMapping`,
`AccessLevel`, `AppModule`.

| Old | New | Why |
|---|---|---|
| `User.role` (one enum value) | `UserAccount.roleDocsIds[]` | one person can be both a teacher and a hostel warden |
| `Role` (fixed enum) | `Role` collection | schools name their own jobs |
| `AccessLevel` (NONE/VIEW/OWN/FULL) | `PermissionAction` + `DataScope` | VIEW/OWN/FULL mixed "what may I do" with "how much may I see", and could not express APPROVE at all |
| `RolePermissionMapping` (role → permissions) | `Role.permissions[]` | the permissions belong to the role; a second collection added a join for nothing |
| `User.referenceDocsId` (untyped) | `personType` + `personDocsId` | the old field was a bare id with a comment saying it might be a staff, student, parent or driver |
| nothing | `AuthSession` | there was no way to sign somebody out |

## Rules the services must enforce

The models carry only structural constraints. Everything below lives in the
service and DTO layer.

**Accounts**

1. At least one of `normalizedEmail` and `normalizedPhone` must be present.
2. Email is lower-cased and trimmed, phone is put into international form, before
   either is saved or looked up.
3. The person named by `personType` and `personDocsId` must exist in the same
   school.
4. A password is checked against the school's password rules before hashing, and
   the plain value never leaves the method that hashes it.
5. `failedLoginCount` goes back to zero on any successful sign-in.
6. The last account able to manage user access for a school may not be
   suspended, closed, or have that role taken away.

**Roles**

7. Every id in `roleDocsIds` must name a role in the same school that is
   `active` at the time it is added.
8. Nobody adds a role to their own account.
9. A `systemManaged` role is never edited or deleted.
10. A role still named by any account's `roleDocsIds` is never deleted.
11. A role's permission list must not have two lines for the same module.
12. Where two of a person's roles say different things about one module, the
    wider one wins. Roles only ever add permissions; nothing takes one away.

**Sessions**

13. Changing a password ends every session on every device.
14. A session that started before `UserAccount.passwordChangedAt` is treated as
    closed, whatever its own `expiresAt` says.
15. Suspending or closing an account ends its sessions.
16. `schoolId` comes from the session, never from the request body, and every
    query includes it.
