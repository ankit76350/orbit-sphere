package com.orbitastra.backend.models.undone.audit;

import java.time.LocalDateTime;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A cross-cutting activity/audit trail entry — every module records who did what
 * (the frontend's {@code logAction(userId, name, role, action, details)}). Central
 * to accountability and compliance across all modules, so it lives on its own
 * rather than inside any single feature package.
 */
@Document(collection = "audit_logs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLog {

    @CreatedDate
    private java.time.LocalDateTime createdAt;

    @Id
    private String id;

    @Indexed
    private String schoolId;

    @Indexed
    private String userId; // references User.id / Staff.id

    private String userName;

    private String role;

    private String action;

    private String details;

    @Indexed
    private LocalDateTime timestamp;
}
