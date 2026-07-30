package com.orbitastra.backend.models.undone.a_new.academics;

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

@Document(collection = "student_attendance_records")
@CompoundIndexes({
        @CompoundIndex(name = "tenant_session_student_uniq",
                def = "{'tenantId':1,'attendanceSessionDocsId':1,'studentDocsId':1}", unique = true),
        @CompoundIndex(name = "tenant_student_attendance_date_idx",
                def = "{'tenantId':1,'studentDocsId':1,'attendanceDate':-1,'status':1}"),
        @CompoundIndex(name = "tenant_date_status_idx",
                def = "{'tenantId':1,'campusDocsId':1,'attendanceDate':1,'status':1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class StudentAttendanceRecord extends AcademicScopedDocument {

    private String attendanceSessionDocsId;
    private String studentDocsId;
    private String enrollmentDocsId;
    private LocalDate attendanceDate;
    private String status;
    private String reasonCode;
    private String source;
    private String capturedByDocsId;
    private Instant capturedAt;
    private Instant arrivalAt;
    private Instant departureAt;
    private Boolean guardianNotified;
    private Instant guardianAcknowledgedAt;
    private String correctionWorkflowRunDocsId;
}
