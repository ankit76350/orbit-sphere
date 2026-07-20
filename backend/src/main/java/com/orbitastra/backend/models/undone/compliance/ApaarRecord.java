package com.orbitastra.backend.models.undone.compliance;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.undone.compliance.enums.ApaarStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A student's APAAR / "One Nation One Student ID" record — the 12-digit APAAR ID
 * built on the 11-digit PEN, plus Aadhaar/DigiLocker linkage state. Government
 * (UDISE+) compliance data that does not belong on the core Student document.
 */
@Document(collection = "apaar_records")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApaarRecord {

    @CreatedDate
    private java.time.LocalDateTime createdAt;

    @LastModifiedDate
    private java.time.LocalDateTime updatedAt;

    @Id
    private String id;

    @Indexed
    private String schoolId;

    @Indexed(unique = true)
    private String studentId;

    // 11-digit Permanent Education Number.
    private String pen;

    // 12-digit APAAR ID (null until generated).
    @Indexed
    private String apaarId;

    @Builder.Default
    private Boolean aadhaarVerified = false;

    @Builder.Default
    private Boolean digilockerLinked = false;

    @Builder.Default
    private ApaarStatus status = ApaarStatus.PENDING;
}
