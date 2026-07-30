package com.orbitastra.backend.models.undone.a_new.communication;

import java.time.Instant;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.undone.a_new.base.AcademicScopedDocument;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Document(collection = "ptm_bookings")
@CompoundIndexes({
        @CompoundIndex(name = "tenant_ptm_teacher_slot_uniq",
                def = "{'tenantId':1,'ptmEventDocsId':1,'teacherDocsId':1,'startsAt':1}", unique = true),
        @CompoundIndex(name = "tenant_ptm_student_uniq",
                def = "{'tenantId':1,'ptmEventDocsId':1,'studentDocsId':1,'teacherDocsId':1}", unique = true)
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class PtmBooking extends AcademicScopedDocument {

    private String ptmEventDocsId;
    private String teacherDocsId;
    private String studentDocsId;
    private String guardianDocsId;
    private Instant startsAt;
    private Instant endsAt;
    private String status;
    private String virtualLearningSessionDocsId;
    private String summary;
    private String followUpTaskDocsId;
}
