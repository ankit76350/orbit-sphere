package com.orbitastra.backend.models.celebrations;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Document(collection = "birthday_gallery")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BirthdayGallery {
    @org.springframework.data.annotation.CreatedDate
    private java.time.LocalDateTime createdAt;

    @org.springframework.data.annotation.LastModifiedDate
    private java.time.LocalDateTime updatedAt;


    @Id
    private String id;

    private String schoolId;

    private String personType;

    private String personId;

    private String personName;

    private String mediaUrl;

    private String caption;

}
