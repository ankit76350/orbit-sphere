package com.orbitastra.backend.models.undone.document;

import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;

import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.base.SchoolDocs;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Document(collection = "document_approvals")
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentApproval extends SchoolDocs {

    private String documentId;

    private String documentTitle;

    private String requestorId;

    private String requestorName;

    private String requestorRole;

    private String approverId;

    private String approverName;

    private String status; // Pending, Approved, Rejected

    private String remarks;

    private LocalDate approvedAt;
}
