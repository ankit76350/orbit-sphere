# library — books, copies, and who has them

`FeeCategory.LIBRARY` has existed since the finance models were built, with nothing
feeding it. This is what feeds it.

## Relationship overview

```text
LibraryCategory          the school's own sections
      ^
      |
LibraryBook              the TITLE — one row however many copies exist
      |  totalCopyCount / availableCopyCount (running totals)
      |
      +--> BookCopy[]    one PHYSICAL book, own accession number
      |         |
      |         v
      |     BookLoan     one copy, one borrower, once
      |         |          fine snapshot taken at issue
      |         +--> FeeInvoice        (finance bills the fine)
      |
      +--> BookReservation[]   waiting for the title, not a copy

LibraryPolicy            rules per borrower type — days, limits, fines
```

## The collections

| Collection | Purpose |
|---|---|
| `library_categories` | The school's own sections. |
| `library_books` | One title, however many copies. |
| `book_copies` | One physical book on one shelf. |
| `library_policies` | Borrowing rules, one row per borrower type. |
| `book_loans` | One copy out with one borrower. The issue register. |
| `book_reservations` | The queue for a title whose copies are all out. |

## Title and copy are separate

The split that shapes everything here:

> **Three copies of *Wings of Fire* are one `LibraryBook` and three `BookCopy` rows.**

Without it, the title, author, publisher and ISBN get retyped for every copy — and by
the fourth, somebody has spelled the author differently. "How many copies of this do we
have" then has no answer, because the four rows don't know they're the same book.

**A title is never available or on loan; a copy is.** Asking whether the library has a
book free means asking its copies. `totalCopyCount` and `availableCopyCount` on the
title are running totals kept so a search screen can show "2 of 3 available" without
loading every copy — the copies stay the real record, and the totals must always be
rebuildable from them. Same rule `FeeInvoice` follows for its payment totals.

This is the fifth time this shape appears:

| The thing | The instance / event |
|---|---|
| `LibraryBook` | `BookCopy` → `BookLoan` |
| `Visitor` | `VisitorPass` |
| `TransportRoute` | `TransportTrip` |
| `HealthProfile` | `ClinicVisit` |
| `ConcessionPolicy` | `ConcessionRequest` |

## `accessionNumber` — the number that is never reused

Written inside the front cover when the book joins the library, counted in order from
`NumberSequence`. Every school library in the country works this way, and it is what an
auditor asks for.

**It is never reused, even after a copy is withdrawn.** The gap in the sequence *is* the
record that something left the library.

## The barcode is deliberately *not* a secret

`IdCard.scanPayload` and `VisitorPass.scanPayload` are random tokens, because a dropped
badge must tell a stranger nothing.

`BookCopy.barcode` is the opposite. It's printed on the outside of a book sitting on an
open shelf, and anybody may scan it to see whether the book is free. There is nothing
private behind it, so applying the ID-card pattern here would protect nothing while
making the barcode impossible to print predictably.

Three QR/barcode rules now exist in the codebase, and they differ on purpose:

| | Encodes | A stranger scanning it |
|---|---|---|
| `IssuedDocument` | a public verification URL | sees name, type, date, status |
| `IdCard` / `VisitorPass` | a random token | sees nothing |
| `BookCopy` | the book's own barcode | sees a book is a book |

## Fine terms are copied onto the loan

`LibraryPolicy` holds the **current** rules. `BookLoan` copies `dailyFineAmount` and
`maximumFineAmount` onto itself when the book goes out.

Raising the daily fine in November must not change what somebody who borrowed in October
owes, and shortening the loan period must not make an existing loan retroactively late.
Same rule as `ConcessionRequest` copying its rate and `TransportAllocation` copying its
fare — **a policy is a price list, never a promise already made.**

Because the loan snapshots what it needs, `LibraryPolicy` has no version number and can
be edited freely.

## `maximumFineAmount` matters more than it looks

Without a ceiling, a book forgotten over the summer holidays comes back owing more than
the book cost. No school will actually collect that, so the fine stops being a deterrent
and becomes a number everybody ignores — including the librarian.

## Fines go on a bill, not in a library note

`fineAmount` is what was worked out; `feeInvoiceDocsId` is set once finance has billed
it under a head with `FeeCategory.FINE`. A fine with no invoice id was calculated and
never charged, and that is a list somebody should look at.

**I dropped the sketch's `finePaid` boolean.** Whether a family has paid is finance's
answer — it comes from `FeeInvoice.outstandingAmount`. A second copy here would only let
the two disagree, and the library's copy would be the one nobody updates.

Same pattern `ConductAction` now uses for a `RESTITUTION` fine.

## `OVERDUE` is a state, not a calculation

A nightly job moves loans from `ON_LOAN` to `OVERDUE`.

The alternative — every screen comparing `dueOn` to today — means the overdue list is
computed slightly differently in three places, and the day a book *became* late isn't
recorded anywhere. As a state it's a plain indexed query.

## Condition on the way out and on the way back

`issuedCondition` and `returnedCondition`. A book that leaves `GOOD` and comes back
`POOR` is a conversation.

Without recording the condition at issue, a damaged book quietly becomes the library's
problem instead of the borrower's — there's no way to show it wasn't already like that.
`RETURNED_DAMAGED` is a separate status from `RETURNED` for the same reason: the book is
back, but something may still be owed.

## Reservations queue for the title, not a copy

The borrower doesn't care which of the three copies they get. Holding a particular one
would leave it on a shelf while somebody returns a *different* copy that then sits
unclaimed.

`allocatedBookCopyDocsId` is filled in only once a copy actually comes back and is set
aside. `queuePosition` is **stored** rather than derived from request times, because it
has to survive somebody in the middle cancelling — recalculating on every read means the
person who was third silently becomes second, and a librarian can't explain the order to
a child who asks.

`holdExpiresAt` keeps the queue moving. A copy held for somebody who never comes is a
copy nobody else can borrow.

## Where library joins the rest of the system

- **`finance`** — a late fine becomes a `FeeInvoice` under `FeeCategory.FINE`. The
  policy names the fee head.
- **`student`** and **`people`** — a borrower is a `Student` or a `Staff`, told apart by
  `BorrowerType`. One register covers both.
- **`documents`** — `coverImageDocsId` is a `DocumentRecord`.
- **`identity`** — a new `LIBRARY` module.

## Added to the shared enums

- `AppModule.LIBRARY`
- `NumberSequenceType.BOOK_ACCESSION`

## The most droppable model

**`BookReservation`.** A school library that tells children to come back and check does
not need it, and everything else here works without it. Same honest note as
`StudentRecognition` in conduct — it's a product call, not a modelling one.

## Deliberately left out

- **Digital books and PDFs.** The sketch had a `pdfUrl` on the book. An e-library is a
  different problem — licences, concurrent-reader limits, and no physical copy to be
  available or not. It would need its own model rather than a URL field here.
- **Dewey or other classification numbers.** School libraries use loose sections, which
  is what `LibraryCategory` is. A school that genuinely catalogues to Dewey can put the
  number in `bookCode`.
- **Multiple authors as a list.** One line of text. A school library does not need a
  contributor list, and searching by co-author is not a request anybody has made.
- **Stock-taking runs.** Annual physical verification against the accession register is
  real, and it is a process over these records rather than a model. Add it when somebody
  asks.
- **Telling a borrower their book is due.** `notification`, designed last. Do not add a
  `reminderSentAt` field here.

## Rules the services must enforce

**Titles and copies**

1. `totalCopyCount` and `availableCopyCount` must always be rebuildable from the copies.
2. An `accessionNumber` is never reused, including after a copy is `WITHDRAWN`.
3. A title with copies is never deleted; a category still used by a title is never
   deleted.
4. A copy that is `ON_LOAN` cannot be `WITHDRAWN`.

**Issuing**

5. Only an `AVAILABLE` copy may be issued.
6. The borrower must be under their policy's `maximumOpenLoans`.
7. `dueOn` is worked out from the policy's `loanDays` at issue time, and the fine terms
   are snapshotted onto the loan in the same step.
8. Issuing sets the copy to `ON_LOAN` and decrements the title's available count in the
   same operation.
9. A copy allocated to a reservation may not be issued to anybody else.

**Renewing**

10. Never beyond the policy's `renewalLimit`, and never while somebody is `WAITING` for
    the title.

**Returning**

11. `returnedCondition` is recorded on every return.
12. The fine is worked out from the loan's own snapshotted terms, never the current
    policy, and never exceeds `maximumFineAmountSnapshot`.
13. Returning puts the copy back to `AVAILABLE` — or `RESERVED` if somebody is waiting
    — and restores the title's count.
14. `finePaid` is never modelled here. Payment is read from the invoice.

**Reservations**

15. Refused when a copy is already `AVAILABLE`.
16. One open reservation per borrower per title.
17. An expired hold releases the copy and moves the queue on.

**Overdue**

18. A nightly job moves due loans to `OVERDUE`. No screen decides it for itself.
19. Non-working days come from `AcademicYear.holidays` when counting fine days; no
    weekday is assumed to be a day off.
