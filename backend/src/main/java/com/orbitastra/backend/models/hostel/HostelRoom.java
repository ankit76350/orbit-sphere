package com.orbitastra.backend.models.hostel;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Document(collection = "hostel_rooms")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HostelRoom {

    @Id
    private String id;

    private String schoolId;

    private String buildingId;

    private Integer floorNo;

    private String roomNo;

    private Integer capacity;

    private Integer occupiedBeds;
}