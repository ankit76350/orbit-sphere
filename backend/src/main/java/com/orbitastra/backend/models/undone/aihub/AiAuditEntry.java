package com.orbitastra.backend.models.undone.aihub;

import java.time.LocalDateTime;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A governance audit trail entry for one AI feature invocation — what was run,
 * by whom, and how many tokens it cost.
 */
@Document(collection = "ai_audit_entries")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiAuditEntry {

    @CreatedDate
    private java.time.LocalDateTime createdAt;

    @LastModifiedDate
    private java.time.LocalDateTime updatedAt;

    @Id
    private String id;

    @Indexed
    private String schoolId;

    private LocalDateTime at;

    // Copilot bundle the feature belongs to, e.g. "Teacher", "Principal".
    private String bundle;

    private String feature;

    private String detail;

    private Long tokens;

    private String actor; // references Staff.id / User.id
}
