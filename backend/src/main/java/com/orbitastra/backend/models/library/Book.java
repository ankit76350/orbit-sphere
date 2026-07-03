package com.orbitastra.backend.models.library;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Document(collection = "books")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Book {
    @org.springframework.data.annotation.CreatedDate
    private java.time.LocalDateTime createdAt;

    @org.springframework.data.annotation.LastModifiedDate
    private java.time.LocalDateTime updatedAt;


    @Id
    private String id;

    private String schoolId;

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
