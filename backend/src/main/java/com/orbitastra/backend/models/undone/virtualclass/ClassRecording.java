package com.orbitastra.backend.models.undone.virtualclass;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Document(collection = "class_recordings")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClassRecording {
    @org.springframework.data.annotation.CreatedDate
    private java.time.LocalDateTime createdAt;

    @org.springframework.data.annotation.LastModifiedDate
    private java.time.LocalDateTime updatedAt;


    @Id
    private String id;

    private String schoolId;

    private String classId;

    private String recordingUrl;

    private String transcript;

    private String summary;
}
