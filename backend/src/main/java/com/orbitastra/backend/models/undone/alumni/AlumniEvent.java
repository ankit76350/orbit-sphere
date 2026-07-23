package com.orbitastra.backend.models.undone.alumni;

import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;

import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.base.SchoolDocs;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Document(collection = "alumni_events")
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class AlumniEvent extends SchoolDocs {

    private String title;

    private String eventType;

    private LocalDate eventDate;

    private String location;

    private String description;

    @Builder.Default
    private Integer rsvps = 0;
}
