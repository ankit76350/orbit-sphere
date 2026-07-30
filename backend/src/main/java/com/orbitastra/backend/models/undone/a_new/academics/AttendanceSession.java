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

@Document(collection = "attendance_sessions")
@CompoundIndexes({
        @CompoundIndex(name = "tenant_attendance_session_key_uniq",
                def = "{'tenantId':1,'sessionKey':1}", unique = true),
        @CompoundIndex(name = "tenant_date_class_period_idx",
                def = "{'tenantId':1,'attendanceDate':1,'classNodeDocsId':1,'sectionNodeDocsId':1,'periodCode':1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceSession extends AcademicScopedDocument {

    private String sessionKey;
    private String sessionType;
    private LocalDate attendanceDate;
    private String classNodeDocsId;
    private String sectionNodeDocsId;
    private String courseOfferingDocsId;
    private String periodCode;
    private String scheduleOccurrenceDocsId;
    private String status;
    private String openedByDocsId;
    private Instant openedAt;
    private Instant lockedAt;
    private String lockedByDocsId;
}
