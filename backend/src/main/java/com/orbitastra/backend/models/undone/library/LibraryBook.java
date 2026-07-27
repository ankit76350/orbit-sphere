package com.orbitastra.backend.models.undone.library;


import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.base.SchoolBase;

import lombok.*;
import lombok.experimental.SuperBuilder;

@Document(collection = "library_books")
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class LibraryBook extends SchoolBase {

    /**
     * Mathematics Grade 8
     */
    @Indexed
    private String title;

    /**
     * Optional subtitle.
     */
    private String subTitle;

    /**
     * Library Category.
     */
    @Indexed
    private String categoryDocsId;

    /**
     * Author name.
     */
    private String author;

    /**
     * Publication.
     */
    private String publisher;

    /**
     * ISBN Number.
     */
    @Indexed(unique = true, sparse = true)
    private String isbn;

    /**
     * English
     * Hindi
     */
    private String language;

    /**
     * School grade.
     */
    private String grade;

    /**
     * 1st Edition
     * 5th Edition
     */
    private String edition;

    /**
     * Publication year.
     */
    private Integer publishedYear;

    /**
     * Number of pages.
     */
    private Integer pages;

    /**
     * Cover image.
     */
    private String coverImageUrl;

    /**
     * Optional eBook.
     */
    private String pdfUrl;

    /**
     * Book summary.
     */
    private String description;

    /**
     * Active / Discontinued.
     */
    @Builder.Default
    private Boolean active = true;
}