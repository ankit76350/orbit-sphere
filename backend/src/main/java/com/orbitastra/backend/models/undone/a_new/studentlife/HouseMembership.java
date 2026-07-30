package com.orbitastra.backend.models.undone.a_new.studentlife;

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

@Document(collection = "house_memberships")
@CompoundIndexes({
        @CompoundIndex(name = "tenant_year_student_house_uniq",
                def = "{'tenantId':1,'academicYearDocsId':1,'studentDocsId':1}", unique = true),
        @CompoundIndex(name = "tenant_year_house_role_idx",
                def = "{'tenantId':1,'academicYearDocsId':1,'houseNodeDocsId':1,'role':1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class HouseMembership extends AcademicScopedDocument {

    private String studentDocsId;
    private String houseNodeDocsId;
    private String role;
    private LocalDate effectiveFrom;
    private LocalDate effectiveUntil;
    private Integer pointsBalance;
}
