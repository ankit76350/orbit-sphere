package com.orbitastra.backend.models.undone.hostel;

import java.time.LocalDate;

import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.base.SchoolBase;
import com.orbitastra.backend.models.undone.hostel.enums.BedStatus;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Document(collection = "hostel_beds")
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class HostelBed extends SchoolBase {

    // Room 101

    // ↓

    // Bed A

    // ↓

    // Rahul

    private String roomDocsId;

    /**
     * A
     * B
     * C
     */
    private String bedNumber;

    /**
     * Student occupying bed.
     */
    private String studentDocsId;

    /**
     * Allocation Date.
     */
    private LocalDate allocatedDate;

    /**
     * Vacated Date.
     */
    private LocalDate vacatedDate;

    /**
     * Occupied / Vacant
     */
    private BedStatus status;
}
