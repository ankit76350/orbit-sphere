package com.orbitastra.backend.models.undone.a_new.people;

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

@Document(collection = "employment_records")
@CompoundIndexes({
        @CompoundIndex(name = "tenant_employee_no_uniq",
                def = "{'tenantId':1,'employeeNo':1}", unique = true),
        @CompoundIndex(name = "tenant_staff_active_employment_idx",
                def = "{'tenantId':1,'staffDocsId':1,'status':1,'effectiveFrom':-1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class EmploymentRecord extends CampusScopedDocument {

    public enum EmploymentStatus {
        OFFERED,
        ACTIVE,
        PROBATION,
        ON_LEAVE,
        SUSPENDED,
        NOTICE_PERIOD,
        TERMINATED,
        RETIRED
    }

    private String employeeNo;
    private String staffDocsId;
    private String positionDocsId;
    private String contractDocsId;
    private String managerDocsId;
    private EmploymentStatus status;
    private LocalDate effectiveFrom;
    private LocalDate effectiveUntil;
    private LocalDate probationUntil;
    private String workLocationDocsId;
    private String payrollGroupKey;
    private String separationReason;
}
