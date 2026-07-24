package com.orbitastra.backend.models.staff;

import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.base.SchoolBase;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Document(collection = "teacher_reviews")
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class TeacherReview extends SchoolBase {

    private String teacherDocsId; // References Staff.id

    private String studentDocsId; // References Student.id

    private String parentDocsId; // References Guardian.id

    private String reviewCycleDocsId; // References ReviewCycle.id

    private Double rating;

    private String reviewText;

    private Boolean anonymous;

    private String created_at;
}
