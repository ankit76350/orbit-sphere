package com.orbitastra.backend.models.alumni;

import java.time.LocalDate;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Document(collection = "mentorship_programs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MentorshipProgram {
    @org.springframework.data.annotation.CreatedDate
    private java.time.LocalDateTime createdAt;

    @org.springframework.data.annotation.LastModifiedDate
    private java.time.LocalDateTime updatedAt;


    @Id
    private String id;

    private String schoolId;

    private String mentorAlumniId;

    private String studentId;

    private String studentName;

    private String category;

    private String status;

    private LocalDate sessionDate;
}
