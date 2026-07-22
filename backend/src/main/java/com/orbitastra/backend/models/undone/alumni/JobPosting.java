package com.orbitastra.backend.models.undone.alumni;

import com.orbitastra.backend.models.BaseDocument;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;

import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Document(collection = "job_postings")
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class JobPosting extends BaseDocument {

    private String alumniId;

    private String title;

    private String company;

    private String location;

    private String description;

    private LocalDate expiryDate;
}
