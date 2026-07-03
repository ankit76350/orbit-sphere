package com.orbitastra.backend.models.academics;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Document(collection = "timetable_events")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TimetableEvent {
    @org.springframework.data.annotation.CreatedDate
    private java.time.LocalDateTime createdAt;

    @org.springframework.data.annotation.LastModifiedDate
    private java.time.LocalDateTime updatedAt;


    @Id
    private String id;

    @Indexed
    private String schoolId;

    @Indexed
    private String classId;

    private String className;

    private String subject;

    private String teacher;

    private String day;

    private String time;

    private String room;
}
