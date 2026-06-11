package com.orbitastra.backend.models.staff;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Document(collection = "student_reviews")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentReview {

    @Id
    private String id;

    private String schoolId;

    private String studentId;

    private String teacherId;

    private String reviewCycleId;

    private Double academicScore;

    private Double disciplineScore;

    private Double participationScore;

    private Double behaviorScore;

    private String comments;

    private String created_at;
}
