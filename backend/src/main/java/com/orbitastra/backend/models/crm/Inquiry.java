package com.orbitastra.backend.models.crm;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.crm.enums.InquiryStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Document(collection = "inquiries")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Inquiry {
    @org.springframework.data.annotation.CreatedDate
    private java.time.LocalDateTime createdAt;

    @org.springframework.data.annotation.LastModifiedDate
    private java.time.LocalDateTime updatedAt;


    @Id
    private String id;

    private String schoolId;

    private String studentName;

    // Prospective guardians for this lead — name, relation, occupation and
    // contact per person. Materialised into real Guardians on conversion.
    @Builder.Default
    private java.util.List<InquiryGuardian> guardians = new java.util.ArrayList<>();

    private String source;

    // Current owner — mirrors the latest follow-up entry's counselor. Kept
    // top-level so inquiries stay filterable by the counselor currently on them.
    private String counselorId;

    // Current pipeline stage — mirrors the latest follow-up entry's status.
    // Kept top-level so inquiries stay filterable by stage.
    private InquiryStatus status;

    private String admissionDocsId;

    // Follow-up timeline: each status change appends {status, note, nextFollowUp, counselorId, recordedAt}.
    @Builder.Default
    private java.util.List<InquiryFollowUp> followUps = new java.util.ArrayList<>();
}