package com.orbitastra.backend.models.gate;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Document(collection = "visitors")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Visitor {
    @org.springframework.data.annotation.CreatedDate
    private java.time.LocalDateTime createdAt;

    @org.springframework.data.annotation.LastModifiedDate
    private java.time.LocalDateTime updatedAt;


    @Id
    private String id;

    private String schoolId;

    private String visitorName;

    private String relationship;

    private String studentId;

    private String studentName;

    private String entryTime;

    private String exitTime;

    private String purpose;
}
