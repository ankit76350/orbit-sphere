package com.orbitastra.backend.models.undone.a_new.hostel;

import java.time.Instant;
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

@Document(collection = "hostel_roll_calls")
@CompoundIndexes({
        @CompoundIndex(name = "tenant_student_date_session_uniq",
                def = "{'tenantId':1,'studentDocsId':1,'rollCallDate':1,'sessionCode':1}", unique = true),
        @CompoundIndex(name = "tenant_hostel_date_status_idx",
                def = "{'tenantId':1,'hostelBuildingDocsId':1,'rollCallDate':1,'status':1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class HostelRollCall extends AcademicScopedDocument {

    private String hostelStayDocsId;
    private String hostelBuildingDocsId;
    private String roomDocsId;
    private String studentDocsId;
    private LocalDate rollCallDate;
    private String sessionCode;
    private String status;
    private Instant recordedAt;
    private String recordedByDocsId;
    private String exceptionReason;
}
