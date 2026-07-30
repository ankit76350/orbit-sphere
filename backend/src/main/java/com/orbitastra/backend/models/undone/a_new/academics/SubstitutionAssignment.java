package com.orbitastra.backend.models.undone.a_new.academics;

import java.time.Instant;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.undone.a_new.base.AcademicScopedDocument;
import com.orbitastra.backend.models.undone.a_new.common.PlatformEnums.ApprovalState;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Document(collection = "substitution_assignments")
@CompoundIndex(name = "tenant_occurrence_substitution_uniq",
        def = "{'tenantId':1,'scheduleOccurrenceDocsId':1}", unique = true)
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class SubstitutionAssignment extends AcademicScopedDocument {

    private String scheduleOccurrenceDocsId;
    private String absentStaffDocsId;
    private String substituteStaffDocsId;
    private ApprovalState state;
    private String reason;
    private String recommendationRunDocsId;
    private Instant notifiedAt;
    private Instant acknowledgedAt;
}
