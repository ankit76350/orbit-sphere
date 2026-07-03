package com.orbitastra.backend.models.gallery;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Document(collection = "gallery_albums")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GalleryAlbum {
    @org.springframework.data.annotation.CreatedDate
    private java.time.LocalDateTime createdAt;

    @org.springframework.data.annotation.LastModifiedDate
    private java.time.LocalDateTime updatedAt;


    @Id
    private String id;

    private String schoolId;

    private String title;

    private String eventType;

    private String coverImage;

    private String status;
}
