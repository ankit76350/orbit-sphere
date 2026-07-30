package com.orbitastra.backend.models.undone.a_new.feedback;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.undone.a_new.base.TenantScopedDocument;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Document(collection = "survey_responses")
@CompoundIndexes({
        @CompoundIndex(name = "tenant_survey_response_no_uniq",
                def = "{'tenantId':1,'responseNo':1}", unique = true),
        @CompoundIndex(name = "tenant_survey_subject_uniq",
                def = "{'tenantId':1,'surveyDefinitionDocsId':1,'subjectLookupHash':1}", unique = true,
                partialFilter = "{'subjectLookupHash':{'$type':'string'}}"),
        @CompoundIndex(name = "tenant_survey_submitted_idx",
                def = "{'tenantId':1,'surveyDefinitionDocsId':1,'submittedAt':-1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class SurveyResponse extends TenantScopedDocument {

    private String responseNo;
    private String surveyDefinitionDocsId;
    private Integer surveyVersion;
    private String respondentType;
    private String respondentDocsId;
    private String subjectLookupHash;
    private Instant startedAt;
    private Instant submittedAt;

    @Builder.Default
    private Map<String, Object> answers = new HashMap<>();
}
