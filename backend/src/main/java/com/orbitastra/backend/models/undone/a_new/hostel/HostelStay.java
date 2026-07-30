package com.orbitastra.backend.models.undone.a_new.hostel;

import java.time.LocalDate;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.undone.a_new.base.AcademicScopedDocument;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Document(collection = "hostel_stays")
@CompoundIndexes({
        @CompoundIndex(name = "tenant_hostel_stay_no_uniq",
                def = "{'tenantId':1,'stayNo':1}", unique = true),
        @CompoundIndex(name = "tenant_year_student_active_stay_uniq",
                def = "{'tenantId':1,'academicYearDocsId':1,'studentDocsId':1,'status':1}",
                unique = true, partialFilter = "{'status':'ACTIVE'}"),
        @CompoundIndex(name = "tenant_bed_active_stay_uniq",
                def = "{'tenantId':1,'bedDocsId':1,'status':1}",
                unique = true, partialFilter = "{'status':'ACTIVE'}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class HostelStay extends AcademicScopedDocument {

    private String stayNo;
    private String studentDocsId;
    private String hostelBuildingDocsId;
    private String roomDocsId;
    private String bedDocsId;
    private String status;
    private LocalDate checkInDate;
    private LocalDate plannedCheckOutDate;
    private LocalDate actualCheckOutDate;
    private String wardenDocsId;
    private String guardianConsentDocsId;
    private String feeStructureDocsId;
    private String closureReason;
}
