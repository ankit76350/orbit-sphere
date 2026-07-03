package com.orbitastra.backend.models.core;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Document(collection = "announcements")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Announcement {

    @Id
    private String id;

    @Indexed
    private String schoolId;

    private String title;

    private String content;

    private String target;

    private String date;

    private String sender;
}
