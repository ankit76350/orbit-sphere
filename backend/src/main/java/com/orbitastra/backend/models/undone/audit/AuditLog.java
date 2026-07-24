package com.orbitastra.backend.models.undone.audit;

import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.base.SchoolBase;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A cross-cutting activity/audit trail entry — every module records who did what
 * (the frontend's {@code logAction(userDocsId, name, role, action, details)}). Central
 * to accountability and compliance across all modules, so it lives on its own
 * rather than inside any single feature package.
 */
@Document(collection = "audit_logs")
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLog extends SchoolBase {

    @Indexed
    private String userDocsId; // references User.id / Staff.id

    private String userName;

    private String role;

    private String action;

    private String details;

    @Indexed
    private LocalDateTime timestamp;
}
