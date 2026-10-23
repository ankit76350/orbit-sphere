package com.orbitastra.backend.models.library;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

import com.orbitastra.backend.models.base.SchoolBase;
import com.orbitastra.backend.models.library.enums.BookCondition;
import com.orbitastra.backend.models.library.enums.BookIssuedStatus;
import com.orbitastra.backend.models.library.enums.BorrowerType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * One copy issued to one borrower, once.
 *
 * <p>Named for what a school librarian actually says. This collection is the issue
 * register: the book was issued to somebody on a date, and it comes back or it does
 * not.
 *
 * <p>Students and staff share one register, told apart by {@code borrowerType}. Two
 * registers would make "where is this copy" two queries and a merge, for no gain.
 *
 * <p>The fine terms are **copied onto the record** when the book is issued, not read from the
 * policy at return time. Raising the daily fine in November must not change what
 * somebody issued a book in October owes, and shortening the issue period must not
 * make an existing issue retroactively late. Same rule ConcessionRequest and
 * TransportAllocation follow.
 *
 * <p>{@code status} carries OVERDUE as a real state rather than something every screen
 * works out from the date for itself. A nightly job moves records into it, so the overdue
 * list is a plain query and the day a book became late is on the record.
 *
 * <p>A fine is money owed, so it goes on a bill rather than staying in a library note.
 * {@code fineAmount} is what was worked out and {@code feeInvoiceDocsId} is set once
 * finance has billed it. A fine with no invoice id is one that was calculated and never
 * charged, which is a list somebody should look at. The old sketch had a
 * {@code finePaid} boolean here; whether a family has paid is finance's answer, not the
 * library's, and keeping a second copy of it here would only let the two disagree.
 *
 * <p>{@code returnedCondition} is checked against what went out. A book that leaves GOOD
 * and comes back POOR is a conversation, and without recording the condition on return
 * a damaged book quietly becomes the library's problem instead of the borrower's.
 *
 * <p>The service checks that the copy was AVAILABLE, that the borrower is under their
 * policy's limit on books held at once, that renewals do not exceed the limit or happen while
 * somebody is waiting, and that returning a copy puts the title's counts back.
 */
@Document(collection = "book_issues")
@CompoundIndexes({
        @CompoundIndex(
                name = "school_issue_copy_open_uniq",
                def = "{'schoolId': 1, 'bookCopyDocsId': 1}",
                unique = true,
                partialFilter = "{'status': {'$in': ['ISSUED', 'OVERDUE']}}"),
        @CompoundIndex(
                name = "school_issue_borrower_idx",
                def = "{'schoolId': 1, 'borrowerType': 1, 'borrowerDocsId': 1, 'issuedOn': -1}"),
        @CompoundIndex(
                name = "school_issue_overdue_idx",
                def = "{'schoolId': 1, 'status': 1, 'dueOn': 1}"),
        @CompoundIndex(
                name = "school_year_issue_idx",
                def = "{'schoolId': 1, 'academicYear': 1, 'issuedOn': -1}"),
        @CompoundIndex(
                name = "school_issue_unbilled_fine_idx",
                def = "{'schoolId': 1, 'feeInvoiceDocsId': 1, 'returnedOn': -1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class BookIssued extends SchoolBase {

    // Links to AcademicYear.name, so a year's borrowing can be counted.
    // Example: "2026-2027"
    @Indexed
    @NotBlank
    private String academicYear;

    // Links to BookCopy.id. The physical object, not the title.
    // Example: "67b91125dc3f7d0044556677"
    @NotBlank
    private String bookCopyDocsId;

    // Links to LibraryBook.id, copied in so a borrower's history reads without
    // loading every copy. Example: "67b91124dc3f7d0033445566"
    @NotBlank
    private String libraryBookDocsId;

    // Whether a student or a staff member has it. Example: BorrowerType.STUDENT
    @NotNull
    private BorrowerType borrowerType;

    // Links to Student.id or Staff.id, depending on borrowerType.
    // Example: "67aa15d9dc3f7d0055555555"
    @NotBlank
    private String borrowerDocsId;

    // The day it went out. Example: 2026-08-19
    @NotNull
    private LocalDate issuedOn;

    // The day it is due back, worked out from the policy's issuePeriodDays at issue time.
    // Example: 2026-09-02
    @NotNull
    private LocalDate dueOn;

    // The day it came back. Null while it is still out. Example: 2026-09-05
    private LocalDate returnedOn;

    // Example: BookIssuedStatus.RETURNED
    @NotNull
    @Builder.Default
    private BookIssuedStatus status = BookIssuedStatus.ISSUED;

    // What condition it went out in, so what comes back can be compared.
    // Example: BookCondition.GOOD
    private BookCondition issuedCondition;

    // What condition it came back in. Example: BookCondition.FAIR
    private BookCondition returnedCondition;

    // How many times it has been extended. Example: 1
    @NotNull
    @Builder.Default
    private Integer renewalCount = 0;

    // Daily fine copied from the policy when the book went out, so a later change to
    // the policy cannot change what this borrower owes. Example: 2.00
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal dailyFineAmountSnapshot;

    // Fine ceiling copied from the policy at the same time. Example: 200.00
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal maximumFineAmountSnapshot;

    // What is owed, worked out on return or when the copy is written off lost.
    // Example: 6.00
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal fineAmount;

    // Links to FeeInvoice.id once finance has billed the fine. Null means worked out
    // and never charged. Whether it has been paid is finance's answer, not kept here.
    // Example: "67ad2233dc3f7d0022334455"
    private String feeInvoiceDocsId;

    // Links to Staff.id for the librarian who handed it over.
    // Example: "67aa15d9dc3f7d0044444444"
    private String issuedByStaffDocsId;

    // Links to Staff.id for whoever took it back.
    // Example: "67aa15d9dc3f7d0044444444"
    private String returnedToStaffDocsId;

    // When the status last changed, used by the nightly overdue job.
    // Example: 2026-09-03T00:05:00Z
    private Instant statusChangedAt;

    // Anything worth knowing.
    // Example: "Cover torn; child said it was already loose when issued."
    private String remarks;
}
