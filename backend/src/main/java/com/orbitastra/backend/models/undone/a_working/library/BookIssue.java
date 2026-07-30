package com.orbitastra.backend.models.undone.a_working.library;


import java.math.BigDecimal;
import java.time.LocalDate;

import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.new_new.base.SchoolBase;
import com.orbitastra.backend.models.undone.a_working.library.enums.BookIssueStatus;

import lombok.*;
import lombok.experimental.SuperBuilder;

@Document(collection = "library_book_issues")
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class BookIssue extends SchoolBase {

    /**
     * Physical book copy.
     */
    @Indexed
    private String bookCopyDocsId;

    /**
     * Student who borrowed.
     */
    @Indexed
    private String studentDocsId;

    /**
     * Teacher can also borrow books.
     */
    private String staffDocsId;

    /**
     * Issue Date.
     */
    private LocalDate issueDate;

    /**
     * Expected Return Date.
     */
    private LocalDate dueDate;

    /**
     * Actual Return Date.
     */
    private LocalDate returnDate;

    /**
     * Fine amount.
     */
    @Builder.Default
    private BigDecimal fineAmount = BigDecimal.ZERO;

    /**
     * Fine paid?
     */
    @Builder.Default
    private Boolean finePaid = false;

    /**
     * Remarks.
     */
    private String remarks;

    /**
     * Issued / Returned / Lost.
     */
    @Builder.Default
    private BookIssueStatus status = BookIssueStatus.ISSUED;
}
