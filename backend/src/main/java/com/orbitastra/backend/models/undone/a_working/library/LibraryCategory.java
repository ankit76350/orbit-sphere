package com.orbitastra.backend.models.undone.a_working.library;

import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.new_new.base.SchoolBase;

import lombok.*;
import lombok.experimental.SuperBuilder;

@Document(collection = "library_categories")
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class LibraryCategory extends SchoolBase {
    //! Database Structure

    //! library_categories
        // Science
        // Mathematics
        // History
        // Reference
        // Story Books
        // Language

    // ↓

    //! library_books
        // Physics Class XI
        // Harry Potter
        // NCERT Mathematics
        // English Grammar

    // ↓

    //! library_book_copies
        // LIB-0001
        // LIB-0002
        // LIB-0003
        // LIB-0004

    // ↓

    //! library_book_issues
        // Rahul
        // ↓
        // LIB-0002
        // ↓
        // Issued
        // ↓
        // Returned

        
    /**
     * Science
     * Mathematics
     * Story Books
     * Reference
     */
    @Indexed(unique = true)
    private String name;

    /**
     * Category description.
     */
    private String description;

    /**
     * Icon shown in UI.
     */
    private String iconUrl;

    /**
     * UI display order.
     */
    @Builder.Default
    private Integer sortOrder = 0;

    /**
     * Active / Inactive.
     */
    @Builder.Default
    private Boolean active = true;
}