# gallery — event photographs, and the consent that governs them

Two collections. Parents open this more than anything else in the app, and it is the cheapest
thing a school can do that families genuinely value.

It is also the package with the sharpest privacy edge in the whole system, because everything
in it is a picture of somebody's child.

## Relationship overview

```text
GalleryAlbum                     one occasion — "Sports Day 2026"
  |  eventType, eventDate, visibility, status
  |
  +--> coverMediaDocsId ------> one of its own GalleryMedia rows
  |
  +--> GalleryMedia[]            one photograph or clip
          |
          +--> documentRecordDocsId ----> ../documents/DocumentRecord.java
          |                               the file. never a URL.
          |
          +--> taggedStudentDocsIds[] --> ../student/Student.java
                        |                  who is identifiable in it
                        v
                  checked against
                  ../compliance/DpdpConsent.java
                  purpose = PHOTOGRAPH_AND_MEDIA
```

### Models from other packages used here

| Model | Lives in | Used for |
|---|---|---|
| [DocumentRecord](../documents/DocumentRecord.java) | `documents` | the actual file and its thumbnail |
| [Student](../student/Student.java) | `student` | who is identifiable in a picture |
| [DpdpConsent](../compliance/DpdpConsent.java) | `compliance` | whether their family agreed to it |
| [ConsentPurpose](../compliance/enums/ConsentPurpose.java) | `compliance/enums` | `PHOTOGRAPH_AND_MEDIA` |
| [SchoolClass](../academics/structure/SchoolClass.java) | `academics/structure` | a class trip album |
| [Staff](../people/staff/Staff.java) | `people/staff` | who uploaded, who approved |
| [AcademicYear](../core/AcademicYear.java) | `core` | the year an album belongs to |
| [AppModule](../identity/enums/AppModule.java) | `identity/enums` | the `GALLERY` permission |

Named as precedent:
[StudentRecognition](../conduct/StudentRecognition.java) — which already carries a
`publicationConsent` flag for the same reason.

## The collections

| Collection | Purpose |
|---|---|
| `gallery_albums` | One occasion's photographs, gathered together. |
| `gallery_media` | One photograph or clip. |

## Files are `DocumentRecord` ids, never URLs

The sketch stored `mediaUrl`, `thumbnailUrl` and `coverImageUrl` as strings.

A URL in a database is a link that either **leaks or rots**: leaks because a public URL to a
child's photograph needs no permission to open, rots because storage moves and the string
doesn't.

`documents` already owns private object storage and issues short-lived signed URLs *after*
checking who is asking. That is exactly the machinery a gallery of children needs, and it
already exists.

## `taggedStudentDocsIds` is the point of the package

The sketch had no consent mechanism at all. This is it.

The list names the children who can be **identified** in a picture. Without it, a school
publishing an album has no way to know whose families agreed — and *"we asked everybody at
admission"* is not an answer when one family says they refused.

So publishing checks each tagged child's `PHOTOGRAPH_AND_MEDIA` consent in `DpdpConsent`, and
`consentVerifiedAt` records **that the check happened**, not that somebody meant to do it. A
picture with an unconsented child in it is cropped, kept at `STAFF` visibility, or left out.

### The harder half: withdrawal after publication

A family who agreed in April may change their mind in November, and they are entitled to.

When a consent is withdrawn, **everything tagging that child has to be found and reviewed.**
That is exactly the query `taggedStudentDocsIds` makes possible and nothing else does — which
is why it is indexed, and why the tag list is not optional metadata but the mechanism.

`WITHDRAWN` is a status rather than a delete for the same reason: somebody will ask why a
photograph disappeared, and a deleted row cannot answer.

## `visibility` decides what consent is needed

| Value | Who sees it | Risk |
|---|---|---|
| `PUBLIC` | anybody on the internet | **once indexed and copied, taking it down does not undo it** |
| `PARENTS` | signed-in parents of this school | contained |
| `STAFF` | staff only | the safe default for anything not yet decided |
| `PRIVATE` | effectively unpublished | — |

It sits on the **media as well as the album**, because one album usually holds a picture that
is fine for the public website and another that is not. **The narrower of the two wins**: a
`PUBLIC` album cannot make a `STAFF` photograph public.

`STAFF` is the default on both, so nothing is visible by accident.

## Publishing is not the uploader's decision

Photographs of children should not go up because one person had a camera. So an album waits at
`PENDING_APPROVAL` for somebody else to look, and `publishedByStaffDocsId` records who let it
through — because that is the person who will be asked.

Same maker-checker instinct as concessions, refunds and payroll.

## Two smaller decisions

**`coverMediaDocsId` points at one of the album's own media rows**, rather than holding a
separate cover image. A cover cannot then outlive the picture it came from — if that photograph
is withdrawn because a family changed their mind, the cover has to change too, and pointing at
the row makes that automatic rather than something somebody has to remember.

**`mediaCount` counts only `PUBLISHED` media.** A parent tapping *"Sports Day (48)"* and
finding twelve pictures has been told something untrue.

## `altText` is not decoration

A parent using a screen reader, or on a slow connection in a village, gets **nothing at all**
from a photograph without it. It is the difference between a gallery that works for every
family and one that works for most.

## Deliberately left out

- **Face recognition to tag children automatically.** Technically possible and a very bad idea
  here: running facial recognition over photographs of minors is a far larger privacy decision
  than a gallery should make on its own, and it would need its own consent purpose and its own
  argument. Tagging stays manual.
- **A separate moderation-decision model.** The `a_new/media` sketch had
  `MediaPublicationDecision` with versions, reason codes and a workflow run. For a school,
  `status` plus who approved is enough; a decision history matters when there are professional
  moderators, and there are not.
- **Faces of people who are not students.** Siblings, visitors and passers-by appear in school
  photographs and have no `Student` row to tag. Real, and it needs a different answer — blurring,
  or a policy of not publishing such pictures — rather than a field here.
- **Albums for staff-only occasions.** A staff picnic has no children in it and needs none of
  this consent machinery. `visibility = STAFF` covers it without a separate model.
- **Telling families a new album is up.** `notification`, designed last. Do not add a
  `notifiedAt` field here.

## Rules the services must enforce

**Files**

1. The file is always a `DocumentRecord` id. No URL is ever stored, and no signed URL is ever
   persisted.
2. One media row per `DocumentRecord`, so the same file cannot be published twice with
   different consent positions.

**Consent**

3. Publishing any media requires a granted, unexpired `PHOTOGRAPH_AND_MEDIA` consent for
   **every** child in `taggedStudentDocsIds`.
4. `consentVerifiedAt` and `consentVerifiedByStaffDocsId` are written at that moment. A null
   `consentVerifiedAt` is never publishable.
5. Withdrawing a child's photograph consent finds every media tagging them and moves it to
   `WITHDRAWN` or back to `PENDING_APPROVAL` for review. This is not optional and not a manual
   step.
6. A `WITHDRAWN` row is never deleted. Somebody will ask why a picture vanished.

**Visibility**

7. The effective visibility is the **narrower** of the media's and its album's.
8. `STAFF` is the default on both album and media. Nothing becomes visible by omission.
9. A `PUBLIC` album is checked again on every publish, not once at creation.

**Publishing**

10. `publishedByStaffDocsId` is never the same as `createdByStaffDocsId`.
11. A withdrawal carries a reason.
12. `coverMediaDocsId` must belong to that album and be `PUBLISHED`.
13. `mediaCount` counts only `PUBLISHED` media and must be rebuildable from the media rows.
