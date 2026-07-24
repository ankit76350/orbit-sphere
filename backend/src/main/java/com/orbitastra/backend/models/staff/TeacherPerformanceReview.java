package com.orbitastra.backend.models.staff;

import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.base.SchoolBase;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Document(collection = "teacher_performance_reviews")
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class TeacherPerformanceReview extends SchoolBase {

    private String teacherDocsId; // References Staff.id

    private String reviewerDocsId; // References Staff.id

    private String reviewerRole;

    private Double rating;

    private String comments;

    private String reviewCycleDocsId; // References ReviewCycle.id
}
