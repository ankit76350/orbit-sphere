package com.orbitastra.backend.models.undone.a_working.hostel;

import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.base.SchoolBase;
import com.orbitastra.backend.models.undone.a_working.hostel.enums.RoomStatus;
import com.orbitastra.backend.models.undone.a_working.hostel.enums.RoomType;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Document(collection = "hostel_rooms")
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class HostelRoom extends SchoolBase {

    private String buildingDocsId;

    private Integer floorNumber;

    private String roomNumber;

    /**
     * Deluxe
     * Standard
     */
    private RoomType roomType;

    /**
     * Maximum beds.
     */
    private Integer capacity;

    /**
     * AC / Non AC
     */
    private Boolean airConditioned;

    /**
     * Available / Full
     */
    private RoomStatus status;

    /**
     * Remarks.
     */
    private String remarks;
}