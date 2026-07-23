package com.orbitastra.backend.models.undone.library;

import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.base.SchoolDocs;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Document(collection = "books")
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class Book extends SchoolDocs {

    private String title;

    private String author;

    private String category;

    private String grade;

    private String isbn;

    private Integer publishedYear;

    private Integer pages;

    private String coverGradient;

    private String pdfUrl;

    private String description;

    @Builder.Default
    private Integer stock = 1;

    @Builder.Default
    private String shelfLocation = "Gen-01";
}
