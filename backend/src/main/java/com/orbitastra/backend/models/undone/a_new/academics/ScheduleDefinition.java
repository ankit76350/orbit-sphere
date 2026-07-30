package com.orbitastra.backend.models.undone.a_new.academics;

import java.time.LocalDate;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.undone.a_new.base.AcademicScopedDocument;
import com.orbitastra.backend.models.undone.a_new.common.PlatformEnums.ApprovalState;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Document(collection = "schedule_definitions")
@CompoundIndex(name = "tenant_year_schedule_version_uniq",
        def = "{'tenantId':1,'academicYearDocsId':1,'scheduleKey':1,'scheduleVersion':1}", unique = true)
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class ScheduleDefinition extends AcademicScopedDocument {

    private String scheduleKey;
    private Integer scheduleVersion;
    private String name;
    private ApprovalState state;
    private LocalDate effectiveFrom;
    private LocalDate effectiveUntil;
    private String weekPattern;
    private String generationJobDocsId;
    private String approvedByDocsId;
}
