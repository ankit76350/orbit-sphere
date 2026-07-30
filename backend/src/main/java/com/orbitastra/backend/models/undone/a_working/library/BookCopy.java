package com.orbitastra.backend.models.undone.a_working.library;



import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.new_new.base.SchoolBase;
import com.orbitastra.backend.models.undone.a_working.library.enums.BookCondition;
import com.orbitastra.backend.models.undone.a_working.library.enums.BookCopyStatus;

import lombok.*;
import lombok.experimental.SuperBuilder;

@Document(collection = "library_book_copies")
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class BookCopy extends SchoolBase {

    /**
     * LibraryBook reference.
     */
    @Indexed
    private String bookDocsId;

    /**
     * Unique library accession number.
     */
    @Indexed(unique = true)
    private String accessionNumber;

    /**
     * Barcode / QR Code.
     */
    @Indexed(unique = true)
    private String barcode;

    /**
     * Shelf location.
     */
    private String shelfLocation;

    /**
     * New / Good / Fair / Poor.
     */
    @Builder.Default
    private BookCondition condition = BookCondition.NEW;

    /**
     * Available / Issued / Lost...
     */
    @Builder.Default
    private BookCopyStatus status = BookCopyStatus.AVAILABLE;
}
