package com.orbitastra.backend.models.academics;

import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Document(collection = "classes")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SchoolClass {

    @Id
    private String id;

    @Indexed
    private String schoolId;

    private String name;

    private String classTeacher;

    private List<ClassSubject> subjects;

    private String academicYearId;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ClassSubject {
        private String name;
        private String teacher;
    }
}
