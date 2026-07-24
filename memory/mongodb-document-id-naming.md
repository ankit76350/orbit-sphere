# MongoDB Document ID Naming

**Project-wide convention (set 2026-07-22, expanded 2026-07-24):**

- If a field stores the MongoDB ObjectId of another document, its name must end
  with `DocsId`.
- If a field stores a human-readable/business identifier, its name must end
  with `No`, not `Id`.
- If a field stores a person's or entity's display label rather than an
  identifier, name it explicitly with `*Name`; do not mislabel a name as
  `*DocsId`.
- Apply these rules everywhere in the project, including models, embedded
  values, DTOs, repositories, services, controllers, tests, clients, mock data,
  Postman examples, persistence migrations, and documentation.

## Required naming

- Use `counselorDocsId`, not `counselorId` or `counselorDocId`.
- Use `studentDocsId`, not `studentId` or `studentDocId`.
- Use `inquiryDocsId`, `admissionDocsId`, `walletDocsId`, and
  `medicalRecordDocsId` for the same reason.
- Use `employeeNo`, `admissionNo`, `studentNo`, `rollNo`, `apaarNo`, and
  similar `*No` names for human-readable identifiers.
- Apply the same name in the model, DTO, repository, service, controller, API
  tester, Postman examples, and API responses.
- When renaming a stored MongoDB field, add an idempotent migration so existing
  documents and browser-local data keep their values. Legacy names may appear
  only inside these migration maps or explicit rejection tests, not as active
  API aliases.

## Exceptions

- A document's own MongoDB primary key remains `id`.
- The shared tenant field remains `schoolId`, as defined by `BaseDocument`.
- Business identifiers that are not MongoDB ObjectIds use a domain-specific
  `*No` name.
- IDs belonging only to embedded values, and not separate MongoDB documents, may
  remain `id`.

This convention applies to all new code and to MongoDB reference fields whenever
an existing feature is changed.
