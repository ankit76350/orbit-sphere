package com.orbitastra.backend.models.undone.a_new.security;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.undone.a_new.base.CampusScopedDocument;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Document(collection = "security_events")
@CompoundIndexes({
        @CompoundIndex(name = "tenant_security_event_no_uniq",
                def = "{'tenantId':1,'eventNo':1}", unique = true),
        @CompoundIndex(name = "tenant_device_detected_idx",
                def = "{'tenantId':1,'securityDeviceDocsId':1,'detectedAt':-1}"),
        @CompoundIndex(name = "tenant_event_review_status_idx",
                def = "{'tenantId':1,'humanReviewStatus':1,'severity':1,'detectedAt':-1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class SecurityEvent extends CampusScopedDocument {

    private String eventNo;
    private String securityDeviceDocsId;
    private String eventType;
    private String severity;
    private Instant detectedAt;
    private String detectionSource;
    private String aiRunDocsId;
    private Double confidenceScore;
    private String humanReviewStatus;
    private String reviewedByDocsId;
    private Instant reviewedAt;
    private String securityCaseDocsId;
    private String summary;

    @Builder.Default
    private List<String> evidenceMediaDocsIds = new ArrayList<>();
}
