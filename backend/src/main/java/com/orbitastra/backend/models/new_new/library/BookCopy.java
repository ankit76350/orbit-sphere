package com.orbitastra.backend.models.new_new.library;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

import com.orbitastra.backend.models.new_new.base.SchoolBase;
import com.orbitastra.backend.models.new_new.library.enums.BookCondition;
import com.orbitastra.backend.models.new_new.library.enums.BookCopyStatus;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * One physical book on one shelf.
 *
 * <p>This is the thing a child carries home. Its title lives on LibraryBook; this row
 * is only about this object: where it is, what state it is in, and whether it can be
 * lent.
 *
 * <p>{@code accessionNumber} is the number written inside the front cover when the book
 * joined the library, counted in order from NumberSequence. Every school library in the
 * country works this way, and it is what an auditor asks for. It is never reused, even
 * after a copy is withdrawn: the gap in the sequence is the record that something left.
 *
 * <p>{@code barcode} is what gets scanned at the desk. Unlike an ID card or a visitor
 * badge, it is **deliberately not a random secret**. It is printed on the outside of a
 * book that sits on an open shelf, and anybody may scan it to see whether the book is
 * free. There is nothing private behind it, so the pattern used for IdCard would be
 * protecting nothing here.
 *
 * <p>{@code status} is kept on the copy rather than worked out from the issue register. A
 * librarian looking for a book needs to know in one read whether it should be on the
 * shelf, without searching the issue register for every copy in the building.
 *
 * <p>{@code replacementCost} is what the borrower is charged if they lose it. It is
 * recorded per copy rather than per title because the same book bought in 2015 and 2025
 * did not cost the same, and charging a family today's price for a fifteen-year-old
 * paperback is not defensible.
 *
 * <p>The service checks that a copy that is out is not withdrawn, that only an AVAILABLE
 * copy is issued, and that the title's copy counts are kept in step.
 */
@Document(collection = "book_copies")
@CompoundIndexes({
        @CompoundIndex(
                name = "school_book_accession_uniq",
                def = "{'schoolId': 1, 'accessionNumber': 1}",
                unique = true),
        @CompoundIndex(
                name = "school_book_barcode_uniq",
                def = "{'schoolId': 1, 'barcode': 1}",
                unique = true,
                partialFilter = "{'barcode': {'$type': 'string'}}"),
        @CompoundIndex(
                name = "school_book_copy_title_idx",
                def = "{'schoolId': 1, 'libraryBookDocsId': 1, 'status': 1}"),
        @CompoundIndex(
                name = "school_book_copy_status_idx",
                def = "{'schoolId': 1, 'status': 1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class BookCopy extends SchoolBase {

    // Links to LibraryBook.id. Example: "67b91124dc3f7d0033445566"
    @NotBlank
    private String libraryBookDocsId;

    // Number written inside the cover when the book joined the library, from
    // NumberSequence type BOOK_ACCESSION. Never reused. Example: "ACC/2026/001842"
    @NotBlank
    private String accessionNumber;

    // What is scanned at the desk. Printed on the book, not a secret.
    // Example: "9788173711466-003"
    private String barcode;

    // Where it sits, so somebody can be sent to fetch it.
    // Example: "Rack 7, shelf B"
    private String shelfLocation;

    // Example: BookCondition.GOOD
    @NotNull
    @Builder.Default
    private BookCondition condition = BookCondition.NEW;

    // Example: BookCopyStatus.AVAILABLE
    @NotNull
    @Builder.Default
    private BookCopyStatus status = BookCopyStatus.AVAILABLE;

    // What the borrower is charged if this copy is lost. Per copy, because the same
    // title bought in 2015 and 2025 did not cost the same. Example: 350.00
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal replacementCost;

    // The day the school got it. Example: 2026-06-20
    private LocalDate acquiredOn;

    // How it arrived. Example: "Donated by the 2019 batch."
    private String acquisitionNote;

    // When the status last changed, such as when it was marked lost.
    // Example: 2026-09-02T07:15:00Z
    private Instant statusChangedAt;

    // Why, when the status is not AVAILABLE or ISSUED.
    // Example: "Spine broken; sent for rebinding on 2 September."
    private String statusReason;
}
