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
   |   requiresApproval                the finished PDF
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

## Scanning the QR code

A certificate carries a QR code so somebody can point a phone at it instead of
typing a sixteen-character code.

```text
1. Paper is printed with a QR code that encodes scanPayload:
      https://verify.orbitastra.edu.in/d/K7M2-9QXP-4T8B-LZ3R

2. An employer scans it and their phone opens that address.

3. The page looks up issued_documents by verificationCode.

4. It answers with only four things:
      holder name    (from holderNameSnapshot)
      document type
      issue date
      status         VALID / SUPERSEDED / REVOKED / EXPIRED

   and, when the status is SUPERSEDED, a link to the one that replaced it.
```

**The QR holds no personal details.** It is a pointer, not a copy. Anybody who
finds a discarded certificate on a bus learns nothing from scanning it that they
could not already read on the paper in their hand. Encoding the student's name or
class into the QR would put that data on every photocopy, forever, outside the
school's control.

**The QR picture is never saved.** It is drawn from `scanPayload` whenever it is
needed. Storing an image of something you already hold as text is storing it
twice, and the picture would then have to be kept in step with the string.

**`scanPayload` is saved rather than worked out each time**, even though it is
usually just a web address with the code on the end. It records what was
*physically printed*. If the school later moves to a different address, the QR
codes on papers already handed out still point at the old one — and that is only
knowable if the old string was kept.

For the template author: put the QR wherever you want it by adding a `{{qrCode}}`
placeholder and listing it in `placeholderKeys`, the same as any other blank.

### The card QR works the opposite way round

`IdCard` has its own `scanPayload`, which is why the two fields share a name. But
the two QR codes are built on **opposite** privacy rules, and mixing them up would
be a real mistake.

| | Certificate QR | ID card QR |
|---|---|---|
| Who scans it | anybody outside the school | school staff |
| What it encodes | a public web address | a meaningless random token |
| A stranger scanning it | sees name, type, date, status | sees nothing |
| Resolving it | needs no sign-in, by design | needs a signed-in staff member |

A certificate QR **must** open a public page — an employer verifying a transfer
certificate has no school login and never will.

A child's ID card must **never** do that. Somebody who picks up a lost card in a
market has to learn nothing from scanning it. So the card's token is random and
meaningless, and only the school's own app can turn it into a name.

The token is random rather than the card number for a second reason: sequential
values can be lined up against each other to work out how many cards the school
has issued and in what order.

One string covers both the QR and the barcode on a card, because those are two
ways of printing the same value rather than two different values. As with a
certificate, the picture is never saved — it is drawn again from the string each
time the card is printed.

## A reprint hands out the same file, it does not remake the paper

`IssuedDocument.documentRecordDocsId` names the finished PDF, and that file is
what a reprint gives out. The paper is never built a second time.

This matters because rebuilding it would use **today's** data. A transfer
certificate issued when a child was in Class VIII would quietly reprint saying
Class X — official-looking and wrong, with nobody noticing.

Keeping the file rather than the values it was built from also means there is no
second copy of the data to drift out of step with the paper. The PDF is the
record.

An earlier version of this design also kept the values as JSON on the issued
document. It was dropped on 2026-08-14: with the PDF stored, it answered nothing
the file did not already answer. It would only be worth bringing back to re-render
in another language, or to query what a certificate said without opening it.

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
3. `templateVersion` runs from 1 with **no gaps**. An abandoned draft reuses its
   number rather than leaving a hole, because the previous version is found by
   subtracting one and a hole would make that answer wrong. There is no
   `supersedes` pointer on a template — the version number is the chain.
4. `bodyTemplate` is required for `HTML`; `templateFileDocsId` is required for
   `DOCX` and `PDF_FORM`.

**Requests**

5. A guardian may only request documents about their own children.
6. A student may only request documents about themself.
7. A rejection carries a reason, and the reviewer is not the person who asked.
8. `status` becomes `ISSUED` only once `issuedDocumentDocsId` is set.

**Issued documents**

9. Never edited and never deleted. A mistake is fixed by issuing a corrected copy
   and marking this one `SUPERSEDED` with `supersededByDocumentDocsId` set.
10. `verificationCode` is long, random, and never counted upwards.
11. The public verification endpoint returns only holder name, document type,
    issue date and status. Never marks, fees, address or family details.
12. `scanPayload` holds a pointer only. Never encode a name, class, marks or any
    other personal detail into the QR itself.
13. The QR image is generated from `scanPayload` on demand and never stored.
14. A reprint increments `printCount` and never creates a second record.
15. Revoking carries a reason.

**Stored files**

16. `objectKey` is never returned to a client. Signed URLs are short-lived and
    issued only after the three checks above.
17. DTOs and services validate file extensions, maximum sizes, media-type
    allowlists, the upload result, the object key, and ownership of the business
    record the file is being attached to.

**ID cards**

18. One `ACTIVE` card per person at a time.
19. A lost or damaged card is closed before a replacement is issued, and the
    replacement points back through `replacesCardDocsId`.
20. A reader accepts a card only when it is `ACTIVE` and `validUntil` has not
    passed — the date is checked at the reader, not trusted to a status field.
21. `rfidNumber` is unique inside the school when set.
