package com.orbitastra.backend.model;

import java.time.LocalDate;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Document(collection = "alumni_events")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlumniEvent {

    @Id
    private String id;

    private String schoolId;

    private String title;

    private String eventType;

    private LocalDate eventDate;

    private String location;

    private String description;

    @Builder.Default
    private Integer rsvps = 0;
}
