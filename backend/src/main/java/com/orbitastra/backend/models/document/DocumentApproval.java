package com.orbitastra.backend.models.document;

import java.time.LocalDate;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Document(collection = "document_approvals")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentApproval {
    @org.springframework.data.annotation.CreatedDate
    private java.time.LocalDateTime createdAt;

    @org.springframework.data.annotation.LastModifiedDate
    private java.time.LocalDateTime updatedAt;


    @Id
    private String id;

    private String schoolId;

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
