package com.orbitastra.backend.models.undone.virtualclass;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.undone.virtualclass.enums.OnlineClassStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Document(collection = "online_classes")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OnlineClass {
    @org.springframework.data.annotation.CreatedDate
    private java.time.LocalDateTime createdAt;

    @org.springframework.data.annotation.LastModifiedDate
    private java.time.LocalDateTime updatedAt;


    @Id
    private String id;

    private String schoolId;

    private String subjectId;

    private String subjectName;

    private String teacherId;

    private String teacherName;

    private String classId;

    private String sectionId;

    private String meetingLink;

    private String date;

    private String startTime;

    private String endTime;

    private OnlineClassStatus status;
}
