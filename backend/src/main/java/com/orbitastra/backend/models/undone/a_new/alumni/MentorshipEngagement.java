package com.orbitastra.backend.models.undone.a_new.alumni;

import java.time.Instant;
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

@Document(collection = "mentorship_engagements")
@CompoundIndexes({
        @CompoundIndex(name = "tenant_mentor_mentee_program_uniq",
                def = "{'tenantId':1,'programmeKey':1,'mentorAlumniProfileDocsId':1,'menteePersonDocsId':1}",
                unique = true),
        @CompoundIndex(name = "tenant_mentorship_status_review_idx",
                def = "{'tenantId':1,'status':1,'nextReviewDate':1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class MentorshipEngagement extends TenantScopedDocument {

    private String programmeKey;
    private String mentorAlumniProfileDocsId;
    private String menteePersonDocsId;
    private String menteeType;
    private String goals;
    private String status;
    private String consentRecordDocsId;
    private String safeguardingOwnerDocsId;
    private LocalDate nextReviewDate;
    private Instant matchedAt;
    private Instant closedAt;
}
