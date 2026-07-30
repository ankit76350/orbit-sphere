package com.orbitastra.backend.models.undone.a_new.academics;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
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

/**
 * One schedulable occurrence. This replaces school-wide daily documents whose
 * embedded entry arrays become contention and document-growth hot spots.
 */
@Document(collection = "schedule_occurrences")
@CompoundIndexes({
        @CompoundIndex(name = "tenant_schedule_occurrence_uniq",
                def = "{'tenantId':1,'scheduleDocsId':1,'occurrenceKey':1}", unique = true),
        @CompoundIndex(name = "tenant_date_class_time_idx",
                def = "{'tenantId':1,'occurrenceDate':1,'classNodeDocsId':1,'sectionNodeDocsId':1,'startTime':1}"),
        @CompoundIndex(name = "tenant_date_teacher_time_idx",
                def = "{'tenantId':1,'occurrenceDate':1,'teacherDocsIds':1,'startTime':1}"),
        @CompoundIndex(name = "tenant_date_resource_time_idx",
                def = "{'tenantId':1,'occurrenceDate':1,'resourceDocsIds':1,'startTime':1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class ScheduleOccurrence extends AcademicScopedDocument {

    public enum OccurrenceType {
        LESSON,
        BREAK,
        EXAM,
        ACTIVITY,
        DUTY,
        MEETING,
        FACILITY_BOOKING
    }

    private String scheduleDocsId;
    private String occurrenceKey;
    private OccurrenceType type;
    private LocalDate occurrenceDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private String classNodeDocsId;
    private String sectionNodeDocsId;
    private String subjectNodeDocsId;

    @Builder.Default
    private List<String> teacherDocsIds = new ArrayList<>();

    @Builder.Default
    private List<String> resourceDocsIds = new ArrayList<>();

    private String recurrenceKey;
    private Boolean cancelled;
    private Instant publishedAt;
}
