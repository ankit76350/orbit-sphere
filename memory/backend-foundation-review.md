# Backend Foundation Review

**Date:** 2026-07-18
**Scope:** Spring Boot 4.1 + MongoDB backend (`backend/`), reviewed before building more SaaS modules.
**Goal:** make the base foundation strong now, so early choices don't become expensive rewrites post-launch.

---

## Verdict

Code quality and domain modeling are genuinely good, but the SaaS foundation has three cracks
(**exposed credentials, no auth, no tenant isolation**) that get much more expensive to fix after
more modules and real data exist. Fix them now while there are ~20 not-yet-built modules and no
production data.

---

## ✅ What's done well (keep doing this)

- **Clean layering** — Controller → Service → Repository is consistent; `@RequiredArgsConstructor` DI; no business logic in controllers.
- **Money is `BigDecimal`** everywhere in finance (`FeeInvoice`, wallets) — correct, avoids the classic float-rounding bug.
- **Auditing** via `@EnableMongoAuditing` + `@CreatedDate` / `@LastModifiedDate`.
- **Thoughtful modeling** — `Student` vs `StudentAcademicRecord` (current vs history) split; N+1-avoidance batch loader in `StudentService`; guardian many-to-many with dedup.
- **Package-by-feature** layout with an `undone/` staging area for planned modules.
- Real `GlobalExceptionHandler` with typed exceptions.

---

## 🔴 Critical — fix before writing any more features

### 1. Live database credentials committed to git
`backend/src/main/resources/application-dev.properties` contains a real MongoDB Atlas password in plaintext, tracked in the repo (and in git history forever).

- [ ] Rotate that password now — assume it's compromised.
- [ ] Move secrets to env vars: `spring.data.mongodb.uri=${MONGODB_URI}`.
- [ ] Add the properties file to `.gitignore`; commit an `application-dev.properties.example` with placeholders.
- [ ] Note: `application.properties` uses `spring.mongodb.uri` (wrong key) vs `spring.data.mongodb.uri` (correct) — verify dev connects to the intended DB.

### 2. No authentication or authorization — every endpoint is public
No `spring-boot-starter-security` in `pom.xml`; `User` / `Role` / `RolePermission` live under `models/undone/`. Anyone who can reach the server can read/write every school's data.

- [ ] Add Spring Security + JWT auth.

### 3. No tenant isolation (the SaaS-defining issue)
Endpoints take `schoolId` from the URL path / request body (e.g. `GET /api/students/school/{schoolId}`), so School A can read School B's data by changing an id; `getAllStudents()` returns every tenant's students.

- [ ] `schoolId` must come from the authenticated token, never the client.
- [ ] Put `schoolId` in JWT claims at login → request-scoped `TenantContext` → all queries auto-scoped by it.

---

## 🟠 Important — do soon (cheap now, painful later)

- [ ] **Input validation absent** — add `spring-boot-starter-validation`, then `@Valid` / `@NotBlank` / `@NotNull` on write paths.
- [ ] **Mass-assignment risk** — controllers bind domain entities directly (`@RequestBody Student`), letting clients set `id`, `createdAt`, `status`, `schoolId`. Introduce request/response **DTOs** for every write endpoint (extend the existing `dto/` package).
- [ ] **Transactions on money paths** — only 3 `@Transactional` total. Fee payment → wallet debit → invoice update must be atomic (MongoDB multi-doc txns work on Atlas replica sets).
- [ ] **Pagination** — `getAll*()` uses `findAll()`; return `Page<T>` with `Pageable`.
- [ ] **500 handler leaks internals** — `GlobalExceptionHandler` returns raw `ex.getMessage()`; log detail + return generic message + trace id.

---

## 🟡 Later (fine to defer, but plan for)

- [ ] API versioning (`/api/v1/...`).
- [ ] OpenAPI/Swagger via `springdoc`.
- [ ] Tests — only the default `BackendApplicationTests` exists; cover money + tenant logic first.
- [ ] Structured logging + `/actuator/health`.
- [ ] Soft-delete / archival (currently hard-deletes students + records — schools usually need retention for compliance).

---

## Recommended sequence

1. Rotate credentials → env vars → gitignore.
2. Spring Security + JWT auth.
3. `schoolId` from token + `TenantContext` (auto-scope all queries).
4. DTOs + validation on write paths.

Pause new `undone/` modules until 1–3 are done — every module inherits this foundation.
