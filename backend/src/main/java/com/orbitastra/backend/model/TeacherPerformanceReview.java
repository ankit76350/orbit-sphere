package com.orbitastra.backend.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Document(collection = "teacher_performance_reviews")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeacherPerformanceReview {

    @Id
    private String id;

    private String schoolId;

    private String teacherId;

    private String reviewerId;

    private String reviewerRole;

    private Double rating;

    private String comments;

    private String reviewCycleId;
}
