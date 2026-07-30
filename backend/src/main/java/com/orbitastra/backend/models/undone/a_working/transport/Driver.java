package com.orbitastra.backend.models.undone.a_working.transport;

import java.time.LocalDate;

import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.new_new.base.SchoolBase;
import com.orbitastra.backend.models.undone.a_working.transport.embedded.EmergencyContact;
import com.orbitastra.backend.models.undone.a_working.transport.enums.DriverStatus;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Document(collection = "drivers")
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class Driver extends SchoolBase {

    /**
     * Reference to Staff collection.
     */
    @Indexed(unique = true)
    private String staffDocsId;

    /**
     * Driving license number.
     */
    @Indexed(unique = true)
    private String licenseNumber;

    /**
     * License expiry date.
     */
    @Indexed
    private LocalDate licenseExpiry;

    /**
     * Internal driver badge/employee code.
     */
    @Indexed
    private String badgeNumber;

    /**
     * Driver joining date.
     */
    private LocalDate joiningDate;

    /**
     * Emergency contact details.
     */
    private EmergencyContact emergencyContact;

    /**
     * Current employment status.
     */
    @Indexed
    private DriverStatus status;

    /**
     * Additional remarks.
     */
    private String remarks;

}