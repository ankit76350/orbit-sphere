package com.orbitastra.backend.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Document(collection = "birthdays")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Birthday {

    @Id
    private String id;

    private String schoolId;

    private String title;

    private String eventType;

    private String date;

    private String description;

    private String targetAudience;
}
