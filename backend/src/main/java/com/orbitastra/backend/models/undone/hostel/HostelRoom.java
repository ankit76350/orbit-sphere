package com.orbitastra.backend.models.undone.hostel;

import com.orbitastra.backend.models.BaseDocument;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Document(collection = "hostel_rooms")
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class HostelRoom extends BaseDocument {

    private String buildingId;

    private Integer floorNo;

    private String roomNo;

    private Integer capacity;

    private Integer occupiedBeds;
}