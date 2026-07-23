package com.orbitastra.backend.models.undone.frontoffice;

import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;

import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.base.SchoolBase;
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
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class Grievance extends SchoolBase {

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
