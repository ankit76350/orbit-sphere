package com.orbitastra.backend.models.academics;

import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

import java.util.List;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.base.SchoolBase;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Document(collection = "classes")
@CompoundIndex(
        name = "school_academic_year_class_name_idx",
        def = "{'schoolId': 1, 'academicYear': 1, 'name': 1}",
        unique = true)
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class SchoolClass extends SchoolBase {

    private String name;

    private String classTeacherDocsId;

    private List<ClassSubject> subjects;

    private String academicYear; // References AcademicYear.name (unique per school), e.g. "2026-2027"

    private List<String> sections;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ClassSubject {
        private String name;
        private String teacherDocsId;
    }
}
