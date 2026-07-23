package com.orbitastra.backend.models.undone.aihub;

import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.base.SchoolDocs;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * An AI-drafted student remark that a human has reviewed and approved before it
 * is released (report card / parent comm) — the human-in-the-loop record for AI
 * governance.
 */
@Document(collection = "ai_approved_remarks")
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class AiApprovedRemark extends SchoolDocs {

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
