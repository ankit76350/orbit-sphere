package com.orbitastra.backend.models.undone.a_working.compliance;

import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;

import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.base.SchoolBase;
import com.orbitastra.backend.models.undone.a_working.compliance.enums.ApaarStatus;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A student's APAAR / "One Nation One Student ID" record — the 12-digit APAAR ID
 * built on the 11-digit PEN, plus Aadhaar/DigiLocker linkage state. Government
 * (UDISE+) compliance data that does not belong on the core Student document.
 */
@Document(collection = "student_government_identities")
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class StudentGovernmentIdentity extends SchoolBase {

    @Indexed(unique = true)
    private String studentDocsId;

    /**
     * Aadhaar number.
     * Encrypt before storing.
     */
    private String aadhaarNo;

    private boolean aadhaarVerified;

    /**
     * Permanent Education Number.
     */
    private String pen;

    /**
     * APAAR ID.
     */
    private String apaarNo;

    private ApaarStatus apaarStatus;

    private boolean digilockerLinked;

    /**
     * Date on which parent/student consent was received.
     */
    private LocalDate consentDate;

    /**
     * Government remarks if verification failed.
     */
    private String remarks;
}
