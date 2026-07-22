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

    @Builder.Default
    private java.util.List<InquiryGuardian> guardians = new java.util.ArrayList<>();

    // not in admission
    private InquiryStatus status;
    private String counselorId;
    private String source;
    @Builder.Default
    private java.util.List<InquiryFollowUp> followUps = new java.util.ArrayList<>();

    // when become admission
    private String admissionDocsId;
}