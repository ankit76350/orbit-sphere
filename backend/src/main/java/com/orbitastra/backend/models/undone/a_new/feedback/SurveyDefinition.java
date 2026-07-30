package com.orbitastra.backend.models.undone.a_new.feedback;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.undone.a_new.base.TenantScopedDocument;
import com.orbitastra.backend.models.undone.a_new.common.PlatformEnums.ApprovalState;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Document(collection = "survey_definitions")
@CompoundIndex(name = "tenant_survey_key_version_uniq",
        def = "{'tenantId':1,'surveyKey':1,'surveyVersion':1}", unique = true)
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class SurveyDefinition extends TenantScopedDocument {

    private String surveyKey;
    private Integer surveyVersion;
    private String title;
    private String purpose;
    private ApprovalState state;
    private Boolean anonymous;
    private Boolean oneResponsePerSubject;
    private Instant opensAt;
    private Instant closesAt;

    @Builder.Default
    private List<Question> questions = new ArrayList<>();

    @Builder.Default
    private List<String> audienceKeys = new ArrayList<>();

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Question {
        private String questionKey;
        private String type;
        private String prompt;
        private Boolean required;
        private List<String> options;
    }
}
