package com.orbitastra.backend.models.staff;

import com.orbitastra.backend.models.BaseDocument;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Document(collection = "teacher_performance_reviews")
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class TeacherPerformanceReview extends BaseDocument {

    private String teacherId;

    private String reviewerId;

    private String reviewerRole;

    private Double rating;

    private String comments;

    private String reviewCycleId;
}
