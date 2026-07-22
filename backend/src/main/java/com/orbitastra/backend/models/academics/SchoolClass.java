package com.orbitastra.backend.models.academics;

import com.orbitastra.backend.models.BaseDocument;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

import java.util.List;

import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Document(collection = "classes")
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class SchoolClass extends BaseDocument {

    private String name;

    private String classTeacher; // here store techer docs id from the staff database

    private List<ClassSubject> subjects;

    private String academicYear; // References AcademicYear.name (unique per school), e.g. "2026-2027"

    private List<String> sections;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ClassSubject {
        private String name;
        private String teacher; // here store techer docs id from the staff database
    }
}
