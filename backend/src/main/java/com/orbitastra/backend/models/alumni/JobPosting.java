package com.orbitastra.backend.models.alumni;

import java.time.LocalDate;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Document(collection = "job_postings")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobPosting {

    @Id
    private String id;

    private String schoolId;

    private String alumniId;

    private String title;

    private String company;

    private String location;

    private String description;

    private LocalDate expiryDate;
}
