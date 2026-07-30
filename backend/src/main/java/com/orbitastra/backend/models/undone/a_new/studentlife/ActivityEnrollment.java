package com.orbitastra.backend.models.undone.a_new.studentlife;

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

@Document(collection = "activity_enrollments")
@CompoundIndexes({
        @CompoundIndex(name = "tenant_activity_student_uniq",
                def = "{'tenantId':1,'activityProgrammeDocsId':1,'studentDocsId':1}", unique = true),
        @CompoundIndex(name = "tenant_activity_status_waitlist_idx",
                def = "{'tenantId':1,'activityProgrammeDocsId':1,'status':1,'waitlistPosition':1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class ActivityEnrollment extends AcademicScopedDocument {

    public enum EnrollmentStatus {
        REQUESTED,
        WAITLISTED,
        ENROLLED,
        WITHDRAWN,
        COMPLETED
    }

    private String activityProgrammeDocsId;
    private String studentDocsId;
    private EnrollmentStatus status;
    private Instant requestedAt;
    private Integer waitlistPosition;
    private String consentRecordDocsId;
    private String invoiceDocsId;
    private String performanceSummary;
}
