package com.orbitastra.backend.models.undone.hostel;

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
    @org.springframework.data.annotation.CreatedDate
    private java.time.LocalDateTime createdAt;

    @org.springframework.data.annotation.LastModifiedDate
    private java.time.LocalDateTime updatedAt;


    @Id
    private String id;

    private String schoolId;

    private String buildingId;

    private Integer floorNo;

    private String roomNo;

    private Integer capacity;

    private Integer occupiedBeds;
}