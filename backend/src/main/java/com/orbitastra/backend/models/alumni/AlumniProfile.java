package com.orbitastra.backend.models.alumni;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.alumni.enums.AlumniStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Document(collection = "alumni_profiles")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlumniProfile {
    @org.springframework.data.annotation.CreatedDate
    private java.time.LocalDateTime createdAt;

    @org.springframework.data.annotation.LastModifiedDate
    private java.time.LocalDateTime updatedAt;


    @Id
    private String id;

    private String schoolId;

    private String name;

    private Integer graduationYear;

    private String batch;

    private String profession;

    private String company;

    private String city;

    private String country;

    private String linkedinUrl;

    private AlumniStatus status;

    private String coverGradient;
}
