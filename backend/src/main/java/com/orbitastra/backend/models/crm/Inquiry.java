package com.orbitastra.backend.models.crm;

import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.base.SchoolBase;
import com.orbitastra.backend.models.crm.embedded.InquiryFollowUp;
import com.orbitastra.backend.models.crm.embedded.InquiryGuardian;
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
public class Inquiry extends SchoolBase {

    private String studentName;

    @Builder.Default
    private java.util.List<InquiryGuardian> guardians = new java.util.ArrayList<>();

    // not in admission
    private InquiryStatus status;
    private String counselorDocsId;
    private String source;
    @Builder.Default
    private java.util.List<InquiryFollowUp> followUps = new java.util.ArrayList<>();

    // when become admission
    private String admissionDocsId;
}
