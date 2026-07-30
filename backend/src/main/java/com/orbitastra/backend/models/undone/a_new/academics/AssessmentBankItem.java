package com.orbitastra.backend.models.undone.a_new.academics;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.undone.a_new.base.CampusScopedDocument;
import com.orbitastra.backend.models.undone.a_new.common.PlatformEnums.ApprovalState;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Document(collection = "assessment_bank_items")
@CompoundIndexes({
        @CompoundIndex(name = "tenant_item_code_version_uniq",
                def = "{'tenantId':1,'itemCode':1,'itemVersion':1}", unique = true),
        @CompoundIndex(name = "tenant_subject_grade_state_idx",
                def = "{'tenantId':1,'subjectNodeDocsId':1,'gradeNodeDocsId':1,'state':1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class AssessmentBankItem extends CampusScopedDocument {

    public enum ItemType {
        MULTIPLE_CHOICE,
        MULTI_SELECT,
        TRUE_FALSE,
        SHORT_ANSWER,
        ESSAY,
        NUMERIC,
        FILE_RESPONSE,
        ORAL,
        PRACTICAL
    }

    private String itemCode;
    private Integer itemVersion;
    private ItemType type;
    private String subjectNodeDocsId;
    private String gradeNodeDocsId;
    private String prompt;
    private String stimulusDocsId;
    private BigDecimal maxScore;
    private String answerKey;
    private String difficulty;
    private String cognitiveLevel;
    private ApprovalState state;

    @Builder.Default
    private List<String> options = new ArrayList<>();

    @Builder.Default
    private List<String> learningOutcomeDocsIds = new ArrayList<>();
}
