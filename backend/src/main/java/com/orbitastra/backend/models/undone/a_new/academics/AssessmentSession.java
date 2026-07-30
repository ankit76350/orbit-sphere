package com.orbitastra.backend.models.undone.a_new.academics;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.undone.a_new.base.AcademicScopedDocument;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Document(collection = "assessment_sessions")
@CompoundIndexes({
        @CompoundIndex(name = "tenant_assessment_session_no_uniq",
                def = "{'tenantId':1,'sessionNo':1}", unique = true),
        @CompoundIndex(name = "tenant_assessment_start_idx",
                def = "{'tenantId':1,'assessmentDefinitionDocsId':1,'startsAt':1,'status':1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class AssessmentSession extends AcademicScopedDocument {

    private String sessionNo;
    private String assessmentDefinitionDocsId;
    private String examDocsId;
    private String classNodeDocsId;
    private String sectionNodeDocsId;
    private String subjectNodeDocsId;
    private Instant startsAt;
    private Instant endsAt;
    private String facilityResourceDocsId;
    private String status;
    private String paperDocumentDocsId;

    @Builder.Default
    private List<String> invigilatorDocsIds = new ArrayList<>();
}
