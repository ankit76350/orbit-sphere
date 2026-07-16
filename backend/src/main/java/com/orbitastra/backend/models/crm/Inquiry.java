package com.orbitastra.backend.models.crm;

import java.time.LocalDate;

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

    private String counselorId;

    private InquiryStatus status;

    private LocalDate nextFollowUp;

    private String notes;
}
