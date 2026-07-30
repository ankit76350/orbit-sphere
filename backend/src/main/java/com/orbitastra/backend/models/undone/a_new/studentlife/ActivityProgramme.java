package com.orbitastra.backend.models.undone.a_new.studentlife;

import java.math.BigDecimal;
import java.time.LocalDate;
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

@Document(collection = "activity_programmes")
@CompoundIndexes({
        @CompoundIndex(name = "tenant_year_activity_code_uniq",
                def = "{'tenantId':1,'academicYearDocsId':1,'activityCode':1}", unique = true),
        @CompoundIndex(name = "tenant_year_activity_type_idx",
                def = "{'tenantId':1,'academicYearDocsId':1,'activityType':1,'active':1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class ActivityProgramme extends AcademicScopedDocument {

    private String activityCode;
    private String name;
    private String activityType;
    private String leadStaffDocsId;
    private String facilityResourceDocsId;
    private Integer capacity;
    private LocalDate enrollmentOpenDate;
    private LocalDate enrollmentCloseDate;
    private BigDecimal feeAmount;
    private String currencyCode;
    private String recurrenceExpression;
    private Boolean active;

    @Builder.Default
    private List<String> eligibleGradeNodeDocsIds = new ArrayList<>();
}
