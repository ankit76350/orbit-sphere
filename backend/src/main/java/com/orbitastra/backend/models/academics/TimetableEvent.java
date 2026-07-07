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

    private String className; // no need this we just need classId 

    private String subject; 

    private String teacher; // store the staff/techer Id of the staffdocs model 

    private String day; // insted of the day make this date not day in the api response we will calculate the day

    private String time;// insted of one time field add two time field startingHour and endtimeHours and add the. and make them local time filed  

    private String room; // make this section
}
