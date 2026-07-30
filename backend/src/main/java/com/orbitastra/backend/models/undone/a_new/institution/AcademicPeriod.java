package com.orbitastra.backend.models.undone.a_new.institution;

import java.time.LocalDate;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.undone.a_new.base.CampusScopedDocument;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Document(collection = "academic_periods")
@CompoundIndexes({
        @CompoundIndex(name = "tenant_campus_period_code_uniq",
                def = "{'tenantId':1,'campusDocsId':1,'code':1}", unique = true),
        @CompoundIndex(name = "tenant_campus_period_dates_idx",
                def = "{'tenantId':1,'campusDocsId':1,'startDate':1,'endDate':1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class AcademicPeriod extends CampusScopedDocument {

    public enum PeriodType {
        ACADEMIC_YEAR,
        SEMESTER,
        TERM,
        QUARTER,
        EXAM_WINDOW,
        VACATION,
        CUSTOM
    }

    private String parentPeriodDocsId;
    private PeriodType type;
    private String code;
    private String displayName;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer sequence;
    private Boolean instructionEnabled;
    private Boolean enrollmentEnabled;
    private Boolean resultsLocked;
}
