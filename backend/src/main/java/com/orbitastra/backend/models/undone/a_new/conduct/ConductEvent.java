package com.orbitastra.backend.models.undone.a_new.conduct;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.undone.a_new.base.CampusScopedDocument;
import com.orbitastra.backend.models.undone.a_new.common.PlatformEnums.Confidentiality;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Document(collection = "conduct_events")
@CompoundIndexes({
        @CompoundIndex(name = "tenant_conduct_event_no_uniq",
                def = "{'tenantId':1,'eventNo':1}", unique = true),
        @CompoundIndex(name = "tenant_conduct_student_occurred_idx",
                def = "{'tenantId':1,'studentDocsIds':1,'occurredAt':-1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class ConductEvent extends CampusScopedDocument {

    private String eventNo;
    private String eventType;
    private String categoryCode;
    private String description;
    private String locationResourceDocsId;
    private Instant occurredAt;
    private String reportedByDocsId;
    private Confidentiality confidentiality;
    private String status;
    private String evidenceDocumentDocsId;

    @Builder.Default
    private List<String> studentDocsIds = new ArrayList<>();

    @Builder.Default
    private List<String> witnessPersonDocsIds = new ArrayList<>();
}
