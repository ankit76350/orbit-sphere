package com.orbitastra.backend.models.undone.a_new.studentlife;

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

@Document(collection = "early_years_observations")
@CompoundIndexes({
        @CompoundIndex(name = "tenant_student_observed_time_idx",
                def = "{'tenantId':1,'studentDocsId':1,'observedAt':-1}"),
        @CompoundIndex(name = "tenant_observer_time_idx",
                def = "{'tenantId':1,'observerDocsId':1,'observedAt':-1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class EarlyYearsObservation extends AcademicScopedDocument {

    private String studentDocsId;
    private String observerDocsId;
    private Instant observedAt;
    private String context;
    private String narrative;
    private String developmentalLevel;
    private Boolean parentVisible;

    @Builder.Default
    private List<String> learningOutcomeDocsIds = new ArrayList<>();

    @Builder.Default
    private List<String> mediaDocumentDocsIds = new ArrayList<>();
}
