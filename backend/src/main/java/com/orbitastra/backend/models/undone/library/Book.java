package com.orbitastra.backend.models.undone.library;

import com.orbitastra.backend.models.BaseDocument;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

import org.springframework.data.mongodb.core.mapping.Document;

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
public class Book extends BaseDocument {

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
