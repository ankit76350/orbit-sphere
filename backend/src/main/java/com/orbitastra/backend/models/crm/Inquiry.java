package com.orbitastra.backend.models.crm;

import com.orbitastra.backend.models.BaseDocument;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.crm.enums.InquiryStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Document(collection = "inquiries")
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class Inquiry extends BaseDocument {

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