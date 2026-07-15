package com.orbitastra.backend.models.undone.gallery;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Document(collection = "gallery_media")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GalleryMedia {
    @org.springframework.data.annotation.CreatedDate
    private java.time.LocalDateTime createdAt;

    @org.springframework.data.annotation.LastModifiedDate
    private java.time.LocalDateTime updatedAt;


    @Id
    private String id;

    private String schoolId;

    private String albumId;

    private String mediaType;

    private String mediaUrl;
}
