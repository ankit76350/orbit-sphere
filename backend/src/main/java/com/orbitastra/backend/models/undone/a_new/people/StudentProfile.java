package com.orbitastra.backend.models.undone.a_new.people;

import java.time.LocalDate;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.undone.a_new.base.TenantScopedDocument;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Document(collection = "student_profiles")
@CompoundIndexes({
        @CompoundIndex(name = "tenant_student_no_uniq",
                def = "{'tenantId':1,'studentNo':1}", unique = true),
        @CompoundIndex(name = "tenant_person_student_uniq",
                def = "{'tenantId':1,'personDocsId':1}", unique = true),
        @CompoundIndex(name = "tenant_student_status_idx",
                def = "{'tenantId':1,'status':1,'admittedOn':-1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class StudentProfile extends TenantScopedDocument {

    private String studentNo;
    private String personDocsId;
    private String admissionApplicationDocsId;
    private LocalDate admittedOn;
    private String status;
    private String currentEnrollmentDocsId;
    private String healthProfileDocsId;
    private String storedValueAccountDocsId;
    private String governmentIdentityDocsId;
    private String profilePhotoObjectDocsId;
}
