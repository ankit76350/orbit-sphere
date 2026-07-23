package com.orbitastra.backend.models.staff;

import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.base.SchoolDocs;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Document(collection = "teacher_reviews")
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class TeacherReview extends SchoolDocs {

    private String teacherId;

    private String studentId;

    private String parentId;

    private String reviewCycleId;

    private Double rating;

    private String reviewText;

    private Boolean anonymous;

    private String created_at;
}
