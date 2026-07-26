package com.orbitastra.backend.models.undone.a_working.document;

import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.base.SchoolBase;
import com.orbitastra.backend.models.undone.a_working.document.enums.ApprovalStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Document(collection = "document_approvals")
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentApproval extends SchoolBase {

    @Indexed
    private String generatedDocumentDocsId;

    @Indexed
    private String requestorDocsId;

    @Indexed
    private String approverDocsId;

    @Builder.Default
    private ApprovalStatus status = ApprovalStatus.PENDING;

    private String remarks;

    private LocalDateTime approvedAt;
}
