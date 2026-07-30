package com.orbitastra.backend.models.undone.a_new.academics;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.undone.a_new.base.AcademicScopedDocument;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Document(collection = "schedule_constraints")
@CompoundIndex(name = "tenant_year_constraint_subject_idx",
        def = "{'tenantId':1,'academicYearDocsId':1,'subjectType':1,'subjectDocsId':1,'active':1}")
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class ScheduleConstraint extends AcademicScopedDocument {

    public enum ConstraintType {
        AVAILABILITY,
        MAX_DAILY_PERIODS,
        MAX_CONSECUTIVE_PERIODS,
        REQUIRED_ROOM_TYPE,
        CAPACITY,
        PREFERRED_TIME,
        AVOID_TIME,
        CONCURRENT_BLOCK,
        CUSTOM
    }

    private ConstraintType type;
    private String subjectType;
    private String subjectDocsId;
    private Integer dayOfWeek;
    private LocalTime startTime;
    private LocalTime endTime;
    private Integer numericValue;
    private Boolean hardConstraint;
    private Boolean active;

    @Builder.Default
    private List<String> relatedDocsIds = new ArrayList<>();
}
