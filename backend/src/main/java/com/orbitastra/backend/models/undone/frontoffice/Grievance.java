package com.orbitastra.backend.models.undone.frontoffice;

import java.time.LocalDate;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.undone.frontoffice.enums.GrievanceCategory;
import com.orbitastra.backend.models.undone.frontoffice.enums.GrievanceSeverity;
import com.orbitastra.backend.models.undone.frontoffice.enums.GrievanceStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A grievance/complaint raised by a parent, staff member or student, tracked to
 * resolution against an SLA.
 */
@Document(collection = "grievances")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Grievance {

    @CreatedDate
    private java.time.LocalDateTime createdAt;

    @LastModifiedDate
    private java.time.LocalDateTime updatedAt;

    @Id
    private String id;

    @Indexed
    private String schoolId;

    private LocalDate date;

    // Who raised it, e.g. "Parent", "Staff", "Student".
    private String raisedBy;

    private String raisedName;

    private GrievanceCategory category;

    private String description;

    private GrievanceSeverity severity;

    private String assignedTo; // references Staff.id

    @Builder.Default
    private GrievanceStatus status = GrievanceStatus.OPEN;

    private String resolutionNote;
}
