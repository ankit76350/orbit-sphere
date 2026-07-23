package com.orbitastra.backend.models.staff;

import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.base.SchoolDocs;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Document(collection = "student_reviews")
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class StudentReview extends SchoolDocs {

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
