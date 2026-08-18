# documents — files the school keeps, and papers the school issues

Two different jobs live here, and telling them apart is the whole shape of this
package.

**Files somebody gave the school.** A parent uploads an income certificate; a
staff member uploads a degree. `DocumentRecord` holds those, and it is only
storage — a key, a filename, a size.

**Papers the school gives out.** A bonafide certificate, a transfer certificate,
an ID card. These are made by the school, from a template, with a number on them,
and somebody outside may need to check they are real. That is everything else in
this package.

## Relationship overview

```text
DocumentRecord      the stored file — used by both jobs
   ^        ^
   |        |
   |        +---------------------------+
   |                                    |
DocumentTemplate  --- versioned ---> IssuedDocument      (the certificate)
   |   wording, placeholders,          documentNo + verificationCode
   |   requiresApproval                dataSnapshotJson
   |                                        ^
   |                                        |
   +--------------------------------->  DocumentRequest  (somebody asked)
   |                                        parent or office
   |
   +---------------------------------> IdCard            (the card)
                                          photo, rfidNumber, replaces chain
```

## The collections

| Collection | Purpose |
|---|---|
| `document_records` | One stored file. Storage only, no meaning of its own. |
| `document_templates` | The wording and layout for one kind of paper, versioned. |
| `document_requests` | Somebody asking for a paper, and somebody agreeing. |
| `issued_documents` | A certificate that has actually been given out. |
| `id_cards` | One identity card, with its photo, chip and replacement history. |

## DocumentRecord — the storage primitive

Everything else here ends up pointing at one of these. It stores only the private
object key and the least metadata needed to show and download a file. Ownership is
established by whichever business model holds the `DocumentRecord.id`; the record
does not repeat that relationship.

```text
Business model
  └── evidenceDocumentDocsIds / photoDocumentDocsId / documentRecordDocsId
        └── DocumentRecord.id
              ├── originalFileName
              ├── mediaType
              ├── sizeBytes
              └── objectKey -> private S3 / Blob / MinIO object
```

`objectKey` is the provider-side key and **never a public URL**. Storage provider,
bucket, region, credentials and signing configuration are backend configuration,
not data. The backend issues a short-lived signed URL only after checking, in
order:

1. the requesting user's permission;
2. the `schoolId` tenant boundary;
3. permission to reach the business record that holds the `DocumentRecord.id`.

Typical upload:

```text
Generate a school-scoped objectKey
  -> issue a short-lived upload URL for that key
  -> the provider confirms the upload
  -> validate the uploaded object
  -> create the DocumentRecord holding the objectKey
  -> attach DocumentRecord.id to the business model
```

## Three decisions you asked about

### 1. Issued documents are verifiable

`IssuedDocument.verificationCode` is what makes a certificate worth more than an
emailed PDF. An employer or another school types it into a public page and finds
out whether the document is real and still stands.

**The code is stored in plain text**, which is a deliberate departure from how
this codebase treats passwords and bank account numbers. The reasoning:

- It is printed on the paper. It was never a secret.
- It is a lookup key, not proof of anything by itself.
- Hashing it would mean the school could never reprint the same certificate, and
  reprinting a lost TC is an ordinary request.

What protects it instead is that it is **long and random, never counted upwards**,
and that the public page gives back only enough to answer the question: the
holder's name, the type of document, the date it was issued, and whether it still
stands. Nothing about marks, fees, address or family may ever appear there.

`documentNo` is the separate, sequential number from `NumberSequence`. It is the
office's own reference. It is not what the public check uses, precisely because
counting upwards makes it guessable.

### 2. Parents can request documents

`DocumentRequest` exists because `identity` now supports a guardian login, so a
parent can ask for a bonafide certificate and follow what happens to it.

**The person asking and the person the paper is about are different fields.** A
mother asks; the certificate is about her son. `requestedByType` /
`requestedByDocsId` and `holderType` / `holderDocsId` are two separate pairs, and
confusing them is the mistake this shape prevents.

**Not every issued document starts with a request.** The office can print a
certificate on the spot, and a bulk run of four hundred ID cards is not four
hundred requests. `DocumentTemplate.requiresApproval` decides whether a request is
needed — a bonafide certificate usually is not, a transfer certificate usually is.

`APPROVED` and `ISSUED` are separate states on purpose. Approved means the head
said yes; issued means the paper exists and was handed over. A request sitting at
approved for a week is a queue somebody needs to look at, and that would be
invisible if the two were one state.

### 3. ID cards are in this package but are their own model

They share templates, printing and storage with certificates, so they belong here.
They are **not** an `IssuedDocument`, and `DocumentType` deliberately has no
`ID_CARD` value.

A certificate is a statement about the past that never changes once given. A card
is a thing somebody carries: it has a photo, it expires, it gets lost, it gets
replaced, and it opens gates. Forcing both into one model would leave most fields
empty most of the time.

**`IdCard.rfidNumber` closes a gap transport already had.** `transport` has
`BoardingCaptureMethod.RFID_CARD`, which assumed something issued the card that
gets tapped. This is that something. A card that is not `ACTIVE` must stop being
accepted immediately, so the reader checks the status rather than trusting the
card.

## Templates are versioned, never edited

A template with documents issued from it is never changed. A reword makes
`templateVersion + 1` and marks the old one `SUPERSEDED` — the same rule
`FeeStructure` follows for a mid-year fee change.

This matters because **a transfer certificate reprinted in 2030 must come out word
for word as it did in 2026**, and it will not if somebody reworded the template in
between. So `IssuedDocument` also copies in `templateKeySnapshot` and
`templateVersionSnapshot` rather than reading them through the link.

## The snapshot that stops silent lies

`IssuedDocument.dataSnapshotJson` keeps the values that were put into the
template.

Without it, a reprint would rebuild the certificate from **today's** data. A
transfer certificate issued when a child was in Class VIII would quietly reprint
saying Class X. The paper would look official and be wrong, and nobody would
notice.

Same reasoning as `FeeInvoiceLine` keeping its own copy of the fee head name, and
`FeeInvoice` keeping `classDocsId` as it was on the billing date.

## Where the wording lives

`TemplateRendererType` decides:

- `HTML` — wording sits inline in `bodyTemplate`. The easy case, and the one a
  school can edit for itself.
- `DOCX` / `PDF_FORM` — an uploaded file, named by `templateFileDocsId`. These are
  designed in Word or a PDF editor and cannot sensibly be typed into a text box.

`placeholderKeys` lists what the template expects to be given, so the school can
be told which placeholder is missing **before** four hundred certificates come out
with a blank in the middle.

## A lost card stays lost

The old row is set to `LOST` rather than being edited into the replacement. A card
somebody else may be holding has to be on record as no longer valid, and editing
it into the new card would erase exactly that.

The replacement is a new row pointing back through `replacesCardDocsId`, so the
chain of replacements reads in order. `issueNumber` counts how many cards the
person has had — a child on their fourth card in a year is a conversation, and a
school charging for replacements needs a count to charge from.

Only one card per person may be `ACTIVE`, which the unique index enforces.

## Added to NumberSequenceType

Two values, both needed by this package:

- `DOCUMENT_REQUEST` — for `DocumentRequest.requestNo`
- `ID_CARD` — for `IdCard.cardNo`

`CERTIFICATE` was already there and had nothing using it. `IssuedDocument.documentNo`
is what it was reserved for.

## Known duplication to clean up later

`DocumentRequesterType` (GUARDIAN / STUDENT / STAFF) and `DocumentRequestStatus`
overlap with `finance`'s `RequesterType` and `ApprovalStatus`.

They are duplicated on purpose rather than imported, because `documents` should
not depend on `finance` for a maker-checker enum. The right fix is to move the
shared ones into `common`, which already holds `Gender` and `GuardianRelation`.
That is a small refactor across two packages and is worth doing once a third
package needs the same thing — not before.

## Notifications are not here

"Tell the parent their certificate is ready" is the obvious next thought, and it
is not in this package. Nothing here records whether a message went out.

That belongs to `notification`, which by decision on 2026-08-14 is designed
**last**. Do not add a `notifiedAt` field here to get around it.

## Deliberately left out

- **Digital signatures.** The old sketch had a `DocumentSignature` model holding
  an image of a principal's signature. A picture pasted onto a PDF proves nothing.
  Real signing means certificates and a signing service, and that is its own
  decision — not a field.
- **A generation job queue.** The `a_new` sketch had `DocumentGenerationJob` with
  retries and attempt counts. That is infrastructure for bulk rendering, not a
  business record. Add it when bulk printing is actually built and you know how it
  will run.
- **Storage provider detail.** The `a_new` sketch had `StoredObject` with bucket,
  region, encryption key reference and malware scan status. `DocumentRecord`
  deliberately keeps only the key and the basics; where a file physically lives is
  backend configuration, not data.
- **Retention and legal holds.** Real, and they belong with a privacy or
  compliance module covering every collection, not just this one.

## Rules the services must enforce

**Templates**

1. A template with documents issued from it is never edited. A change makes a new
   version and marks the old one `SUPERSEDED`.
2. Only one version per `templateKey` is `ACTIVE` at a time.
3. `bodyTemplate` is required for `HTML`; `templateFileDocsId` is required for
   `DOCX` and `PDF_FORM`.

**Requests**

4. A guardian may only request documents about their own children.
5. A student may only request documents about themself.
6. A rejection carries a reason, and the reviewer is not the person who asked.
7. `status` becomes `ISSUED` only once `issuedDocumentDocsId` is set.

**Issued documents**

8. Never edited and never deleted. A mistake is fixed by issuing a corrected copy
   and marking this one `SUPERSEDED` with `supersededByDocumentDocsId` set.
9. `verificationCode` is long, random, and never counted upwards.
10. The public verification endpoint returns only holder name, document type,
    issue date and status. Never marks, fees, address or family details.
11. `dataSnapshotJson` is written at issue time and never updated.
12. A reprint increments `printCount` and never creates a second record.
13. Revoking carries a reason.

**Stored files**

14. `objectKey` is never returned to a client. Signed URLs are short-lived and
    issued only after the three checks above.
15. DTOs and services validate file extensions, maximum sizes, media-type
    allowlists, the upload result, the object key, and ownership of the business
    record the file is being attached to.

**ID cards**

16. One `ACTIVE` card per person at a time.
17. A lost or damaged card is closed before a replacement is issued, and the
    replacement points back through `replacesCardDocsId`.
18. A reader accepts a card only when it is `ACTIVE` and `validUntil` has not
    passed — the date is checked at the reader, not trusted to a status field.
19. `rfidNumber` is unique inside the school when set.
