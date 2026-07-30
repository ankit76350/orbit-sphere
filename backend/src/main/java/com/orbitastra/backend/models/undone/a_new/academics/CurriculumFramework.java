package com.orbitastra.backend.models.undone.a_new.academics;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.undone.a_new.base.CampusScopedDocument;
import com.orbitastra.backend.models.undone.a_new.common.PlatformEnums.ApprovalState;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Document(collection = "curriculum_frameworks")
@CompoundIndex(name = "tenant_campus_framework_version_uniq",
        def = "{'tenantId':1,'campusDocsId':1,'frameworkCode':1,'frameworkVersion':1}", unique = true)
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class CurriculumFramework extends CampusScopedDocument {

    private String programmeDocsId;
    private String frameworkCode;
    private String name;
    private String frameworkVersion;
    private ApprovalState state;
    private Instant effectiveFrom;
    private Instant effectiveUntil;
    private String sourceAuthority;

    @Builder.Default
    private List<String> supportedGradeNodeDocsIds = new ArrayList<>();
}
