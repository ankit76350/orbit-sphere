package com.orbitastra.backend.models.undone.a_new.studentlife;

import java.time.LocalDate;
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

@Document(collection = "university_applications")
@CompoundIndexes({
        @CompoundIndex(name = "tenant_student_university_course_cycle_uniq",
                def = "{'tenantId':1,'studentDocsId':1,'applicationCycle':1,'institutionCode':1,'courseCode':1}",
                unique = true),
        @CompoundIndex(name = "tenant_counsellor_status_deadline_idx",
                def = "{'tenantId':1,'counsellorDocsId':1,'status':1,'deadline':1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class UniversityApplication extends AcademicScopedDocument {

    private String careerPlanDocsId;
    private String studentDocsId;
    private String counsellorDocsId;
    private String applicationCycle;
    private String institutionCode;
    private String institutionName;
    private String courseCode;
    private String courseName;
    private String countryCode;
    private LocalDate deadline;
    private String status;
    private LocalDate decisionDate;
    private String decision;

    @Builder.Default
    private List<String> documentDocsIds = new ArrayList<>();

    @Builder.Default
    private List<String> recommendationRequestDocsIds = new ArrayList<>();
}
