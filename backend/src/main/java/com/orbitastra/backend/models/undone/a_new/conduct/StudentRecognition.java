package com.orbitastra.backend.models.undone.a_new.conduct;

import java.time.Instant;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.undone.a_new.base.AcademicScopedDocument;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Document(collection = "student_recognitions")
@CompoundIndexes({
        @CompoundIndex(name = "tenant_recognition_no_uniq",
                def = "{'tenantId':1,'recognitionNo':1}", unique = true),
        @CompoundIndex(name = "tenant_recognition_student_awarded_idx",
                def = "{'tenantId':1,'studentDocsId':1,'awardedAt':-1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class StudentRecognition extends AcademicScopedDocument {

    private String recognitionNo;
    private String studentDocsId;
    private String recognitionType;
    private String categoryCode;
    private String title;
    private String description;
    private Integer housePoints;
    private String nominatedByDocsId;
    private String approvedByDocsId;
    private String certificateDocumentDocsId;
    private String publicationConsentDocsId;
    private Instant awardedAt;
}
