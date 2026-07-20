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
 * An AI-drafted student remark that a human has reviewed and approved before it
 * is released (report card / parent comm) — the human-in-the-loop record for AI
 * governance.
 */
@Document(collection = "ai_approved_remarks")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiApprovedRemark {

    @CreatedDate
    private java.time.LocalDateTime createdAt;

    @LastModifiedDate
    private java.time.LocalDateTime updatedAt;

    @Id
    private String id;

    @Indexed
    private String schoolId;

    @Indexed
    private String studentId;

    private String grade;

    // Requested tone, e.g. "Encouraging", "Formal", "Direct".
    private String tone;

    private String language;

    private String text;

    private String approvedBy; // references Staff.id / User.id

    private LocalDateTime approvedAt;
}
