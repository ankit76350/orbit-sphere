package com.orbitastra.backend.models.undone.compliance;

import java.time.LocalDate;

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
 * A dated compliance obligation on the school's regulatory calendar (UDISE+
 * return, board affiliation renewal, safety audit, ...) owned by an issuing
 * authority.
 */
@Document(collection = "compliance_tasks")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ComplianceTask {

    @CreatedDate
    private java.time.LocalDateTime createdAt;

    @LastModifiedDate
    private java.time.LocalDateTime updatedAt;

    @Id
    private String id;

    @Indexed
    private String schoolId;

    private String title;

    // Issuing/regulating body, e.g. "CBSE", "UDISE+", "State Board".
    private String authority;

    private LocalDate dueDate;

    @Builder.Default
    private Boolean completed = false;
}
