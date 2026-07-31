# Document storage model mapping

## Relationship

```text
Business model
└── evidenceDocumentId/evidenceDocumentDocsId
        └── DocumentRecord.id
            ├── originalFileName
            ├── mediaType
            ├── sizeBytes
            └── objectKey -> private S3/Blob/MinIO object
```

Both top-level documents extend `SchoolBase`. Every lookup and link check must
include `schoolId`.

## DocumentRecord — `document_records`

Stores only the private object key and the minimum metadata needed to display
and download the file. Ownership is established by the business model that
stores `DocumentRecord.id`; the document does not duplicate that relationship.

`objectKey` is the provider-side key and is not a public URL. Storage provider,
bucket/container, region, credentials, and signing configuration belong to
backend configuration. The backend generates short-lived signed URLs only after
checking:

1. the requesting user's permission;
2. the `schoolId` tenant boundary;
3. permission to access the business record containing DocumentRecord.id.

## Typical upload workflow

```text
Generate a school-scoped objectKey
    -> issue short-lived upload URL for that key
    -> provider confirms upload
    -> validate the uploaded object
    -> create DocumentRecord containing objectKey
    -> attach DocumentRecord.id to the business model
```

## Validation responsibility

Models contain only essential persistence requirements. DTOs and services
validate file extensions, maximum sizes, media-type allowlists, upload results,
the object key, business-record ownership, and conditional fields.
