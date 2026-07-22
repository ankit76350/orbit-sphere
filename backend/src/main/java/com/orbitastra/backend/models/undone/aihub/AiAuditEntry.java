package com.orbitastra.backend.models.undone.aihub;

import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.base.BaseDocument;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A governance audit trail entry for one AI feature invocation — what was run,
 * by whom, and how many tokens it cost.
 */
@Document(collection = "ai_audit_entries")
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class AiAuditEntry extends BaseDocument {

    private LocalDateTime at;

    // Copilot bundle the feature belongs to, e.g. "Teacher", "Principal".
    private String bundle;

    private String feature;

    private String detail;

    private Long tokens;

    private String actor; // references Staff.id / User.id
}
