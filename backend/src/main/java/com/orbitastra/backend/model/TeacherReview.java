package com.orbitastra.backend.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Document(collection = "teacher_reviews")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeacherReview {

    @Id
    private String id;

    private String schoolId;

    private String teacherId;

    private String studentId;

    private String parentId;

    private String reviewCycleId;

    private Double rating;

    private String reviewText;

    private Boolean anonymous;

    private String created_at;
}
