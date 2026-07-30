package com.orbitastra.backend.models.undone.a_new.academics;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.undone.a_new.base.AcademicScopedDocument;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Document(collection = "course_offerings")
@CompoundIndexes({
        @CompoundIndex(name = "tenant_year_course_code_uniq",
                def = "{'tenantId':1,'academicYearDocsId':1,'courseCode':1}", unique = true),
        @CompoundIndex(name = "tenant_year_class_subject_idx",
                def = "{'tenantId':1,'academicYearDocsId':1,'classNodeDocsId':1,'subjectNodeDocsId':1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class CourseOffering extends AcademicScopedDocument {

    private String courseCode;
    private String title;
    private String curriculumFrameworkDocsId;
    private String classNodeDocsId;
    private String sectionNodeDocsId;
    private String subjectNodeDocsId;

    @Builder.Default
    private List<String> teacherDocsIds = new ArrayList<>();

    @Builder.Default
    private List<String> curriculumUnitDocsIds = new ArrayList<>();

    private String gradingSchemeKey;
    private Boolean published;
    private Boolean enrollmentOpen;
}
