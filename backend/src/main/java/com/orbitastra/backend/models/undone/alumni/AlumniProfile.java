package com.orbitastra.backend.models.undone.alumni;

import com.orbitastra.backend.models.BaseDocument;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.undone.alumni.enums.AlumniStatus;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Document(collection = "alumni_profiles")
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class AlumniProfile extends BaseDocument {

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
