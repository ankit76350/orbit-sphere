package com.orbitastra.backend.models.undone.a_new.people;

import java.time.LocalDate;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.undone.a_new.base.AcademicScopedDocument;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Document(collection = "student_enrollments")
@CompoundIndexes({
        @CompoundIndex(name = "tenant_year_student_active_enrollment_uniq",
                def = "{'tenantId':1,'academicYearDocsId':1,'studentDocsId':1,'active':1}",
                unique = true, partialFilter = "{'active':true}"),
        @CompoundIndex(name = "tenant_year_identity_no_uniq",
                def = "{'tenantId':1,'academicYearDocsId':1,'identityNo':1}", unique = true),
        @CompoundIndex(name = "tenant_class_section_roll_active_uniq",
                def = "{'tenantId':1,'academicYearDocsId':1,'classNodeDocsId':1,'sectionNodeDocsId':1,'rollNo':1,'active':1}",
                unique = true, partialFilter = "{'active':true}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class StudentEnrollment extends AcademicScopedDocument {

    private String studentDocsId;
    private String identityNo;
    private String classNodeDocsId;
    private String sectionNodeDocsId;
    private String rollNo;
    private String enrollmentStatus;
    private LocalDate effectiveFrom;
    private LocalDate effectiveUntil;
    private Boolean active;
    private String previousEnrollmentDocsId;
    private String promotionRunDocsId;
    private String exitReason;
}
