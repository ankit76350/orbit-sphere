package com.orbitastra.backend.models.new_new.library;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.new_new.base.SchoolBase;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * One title the library holds, however many copies of it there are.
 *
 * <p>This is the book as a thing you can name, not a thing you can carry. Three copies
 * of the same Wings of Fire are **one** LibraryBook and three BookCopy rows.
 *
 * <p>That split is the whole shape of this package. Without it, the title, author,
 * publisher and ISBN get retyped for every copy, and by the fourth one somebody has
 * spelled the author differently. "How many copies of this do we have" then has no
 * answer, because the four rows do not know they are the same book.
 *
 * <p>Nothing about lending lives here. A title is never available or issued; a copy is.
 * Asking whether the library has a book free means asking its copies.
 *
 * <p>{@code isbn} is not unique in this collection and not required. Old books predate
 * ISBNs, donated books arrive without them, and two schools' copies of the same book are
 * still two rows because each school owns its own. What identifies a title inside a
 * school is its own {@code bookCode}.
 *
 * <p>{@code totalCopyCount} and {@code availableCopyCount} are running totals kept so a
 * search screen can show "2 of 3 available" without loading every copy. The copies stay
 * the real record, and the totals must always be rebuildable from them, the same rule
 * FeeInvoice follows for its payment totals.
 *
 * <p>The service checks that the counts match the copies, that a title with copies is
 * not deleted, and that the category belongs to the same school.
 */
@Document(collection = "library_books")
@CompoundIndexes({
        @CompoundIndex(
                name = "school_library_book_code_uniq",
                def = "{'schoolId': 1, 'bookCode': 1}",
                unique = true),
        @CompoundIndex(
                name = "school_library_book_title_idx",
                def = "{'schoolId': 1, 'title': 1}"),
        @CompoundIndex(
                name = "school_library_book_author_idx",
                def = "{'schoolId': 1, 'author': 1}"),
        @CompoundIndex(
                name = "school_library_book_category_idx",
                def = "{'schoolId': 1, 'libraryCategoryDocsId': 1, 'title': 1}"),
        @CompoundIndex(
                name = "school_library_book_isbn_idx",
                def = "{'schoolId': 1, 'isbn': 1}",
                partialFilter = "{'isbn': {'$type': 'string'}}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class LibraryBook extends SchoolBase {

    // The school's own key for this title, which copies point at.
    // Example: "BK-000482"
    @NotBlank
    private String bookCode;

    // Example: "Wings of Fire"
    @NotBlank
    private String title;

    // Example: "An Autobiography of A. P. J. Abdul Kalam"
    private String subTitle;

    // Written as one line, because a school library does not need a list of
    // contributors. Example: "A. P. J. Abdul Kalam, Arun Tiwari"
    @NotBlank
    private String author;

    // Links to LibraryCategory.id. Example: "67b91122dc3f7d0011223344"
    private String libraryCategoryDocsId;

    // Example: "Universities Press"
    private String publisher;

    // Not required and not unique: old and donated books often have none.
    // Example: "9788173711466"
    private String isbn;

    // Example: "en"
    private String language;

    // Which classes this is meant for, in the school's own words.
    // Example: "Class VIII and above"
    private String suitableFor;

    // Example: "Reprint, 2015"
    private String edition;

    // Example: 1999
    private Integer publishedYear;

    // Example: 180
    private Integer pageCount;

    // Links to DocumentRecord.id for a cover picture.
    // Example: "67b91123dc3f7d0022334455"
    private String coverImageDocsId;

    // How many copies the school owns, counting every status. Kept as a running
    // total; the copies remain the real record. Example: 3
    @NotNull
    @Builder.Default
    private Integer totalCopyCount = 0;

    // How many are on the shelf right now. Example: 2
    @NotNull
    @Builder.Default
    private Integer availableCopyCount = 0;

    // Whether this title may still be lent. Turning it off does not recall the
    // copies already out. Example: true
    @NotNull
    @Builder.Default
    private Boolean active = true;

    // Example: "Donated by the 2019 batch."
    private String remarks;
}
