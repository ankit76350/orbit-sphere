package com.orbitastra.backend.models.undone.a_working.hostel;

import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.new_new.base.SchoolBase;
import com.orbitastra.backend.models.undone.a_working.hostel.enums.HostelStatus;
import com.orbitastra.backend.models.undone.a_working.hostel.enums.HostelType;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Document(collection = "hostel_buildings")
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class HostelBuilding extends SchoolBase {

    /**
     * Boys Hostel A
     */
    private String name;

    /**
     * Boys / Girls
     */
    private HostelType hostelType;

    /**
     * Number of floors.
     */
    private Integer totalFloors;

    /**
     * Building address.
     */
    private String address;

    /**
     * Hostel Warden.
     */
    private String wardenDocsId;

    /**
     * Contact Number.
     */
    private String contactNumber;

    /**
     * Active / Under Maintenance.
     */
    private HostelStatus status;
}