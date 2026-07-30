package com.orbitastra.backend.models.undone.a_new.admissions;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.undone.a_new.base.AcademicScopedDocument;
import com.orbitastra.backend.models.undone.a_new.common.PlatformEnums.Confidentiality;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Document(collection = "admission_applications")
@CompoundIndexes({
        @CompoundIndex(name = "tenant_application_no_uniq",
                def = "{'tenantId':1,'applicationNo':1}", unique = true),
        @CompoundIndex(name = "tenant_cycle_applicant_lookup_uniq",
                def = "{'tenantId':1,'admissionCycleDocsId':1,'applicantLookupHash':1}", unique = true),
        @CompoundIndex(name = "tenant_cycle_grade_status_idx",
                def = "{'tenantId':1,'admissionCycleDocsId':1,'appliedGradeNodeDocsId':1,'status':1,'submittedAt':1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class AdmissionApplication extends AcademicScopedDocument {

    private String applicationNo;
    private String admissionCycleDocsId;
    private String inquiryDocsId;
    private String appliedGradeNodeDocsId;
    private String applicantLookupHash;
    private String encryptedApplicantProfile;
    private Confidentiality confidentiality;
    private String status;
    private Integer formVersion;
    private Instant submittedAt;
    private String assignedAdmissionOfficerDocsId;
    private String workflowRunDocsId;
    private String resultingStudentDocsId;

    @Builder.Default
    private Map<String, Object> formAnswers = new HashMap<>();

    @Builder.Default
    private List<String> evidenceDocumentDocsIds = new ArrayList<>();
}
