package com.orbitastra.backend.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Document(collection = "hostel_buildings")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HostelBuilding {

    @Id
    private String id;

    private String schoolId;

    private String name;

    private Integer floors;
}
