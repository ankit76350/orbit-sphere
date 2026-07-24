# MongoDB Document ID Naming

**Convention (set 2026-07-22):** If a field stores the MongoDB ObjectId of another
document, its name must end with `DocsId`.

## Required naming

- Use `counselorDocsId`, not `counselorId` or `counselorDocId`.
- Use `studentDocsId`, not `studentId` or `studentDocId`.
- Use `inquiryDocsId`, `admissionDocsId`, `walletDocsId`, and
  `medicalRecordDocsId` for the same reason.
- Apply the same name in the model, DTO, repository, service, controller, API
  tester, Postman examples, and API responses.
- When renaming a stored MongoDB field, add an idempotent migration so existing
  documents keep their reference. Keep the old request name as a JSON alias when
  backward compatibility is useful.

## Exceptions

- A document's own MongoDB primary key remains `id`.
- The shared tenant field remains `schoolId`, as defined by `BaseDocument`.
- Business identifiers that are not MongoDB ObjectIds keep their domain names,
  such as `admissionNo`, `employeeNo`, `rollNo`, and `studentNo`.
- IDs belonging only to embedded values, and not separate MongoDB documents, may
  remain `id`.

This convention applies to all new code and to MongoDB reference fields whenever
an existing feature is changed.
