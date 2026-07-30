package com.orbitastra.backend.models.undone.a_new.alumni;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.undone.a_new.base.TenantScopedDocument;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Document(collection = "alumni_profiles_v2")
@CompoundIndexes({
        @CompoundIndex(name = "tenant_alumni_person_uniq",
                def = "{'tenantId':1,'personDocsId':1}", unique = true),
        @CompoundIndex(name = "tenant_alumni_graduation_status_idx",
                def = "{'tenantId':1,'graduationYear':-1,'status':1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class AlumniProfile extends TenantScopedDocument {

    private String personDocsId;
    private String formerStudentProfileDocsId;
    private String finalEnrollmentDocsId;
    private Integer graduationYear;
    private String cohortKey;
    private LocalDate alumniSince;
    private String status;
    private String encryptedProfessionalProfile;
    private String publicProfileSlug;
    private String directoryVisibility;
    private Boolean contactAllowed;
    private String contactConsentRecordDocsId;

    @Builder.Default
    private List<String> interestCodes = new ArrayList<>();

    @Builder.Default
    private List<String> engagementPreferenceCodes = new ArrayList<>();
}
