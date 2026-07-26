package com.orbitastra.backend.models.undone.compliance;

import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.base.SchoolBase;
import com.orbitastra.backend.models.undone.compliance.embedded.ComplianceDocument;
import com.orbitastra.backend.models.undone.compliance.enums.ComplianceAuthority;
import com.orbitastra.backend.models.undone.compliance.enums.ComplianceStatus;
import com.orbitastra.backend.models.undone.compliance.enums.ComplianceTaskType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

//! What the ERP can do
// The dashboard could show
// Upcoming Compliance

// ⚠ Fire Safety Certificate
// Due in 3 days

// ⚠ UDISE+ Submission
// Due in 15 days

// ✓ CBSE Renewal
// Completed


@Document(collection = "compliance_tasks")
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class ComplianceTask extends SchoolBase {

    private String title;

    private ComplianceAuthority authority;

    private ComplianceTaskType type;

    /**
     * Deadline to complete the task.
     */
    private LocalDate dueDate;

    /**
     * Actual completion timestamp.
     */
    private LocalDateTime completedAt;

    /**
     * Expiry/validity of the resulting certificate or document.
     * Null for one-time tasks.
     */
    private LocalDate validUntil;

    private ComplianceStatus status;

    //assignedToDocsId is the person responsible for completing the compliance task.
    private String assignedToDocsId;

    private String remarks;

    @Builder.Default
    private List<ComplianceDocument> documents = new ArrayList<>();

}