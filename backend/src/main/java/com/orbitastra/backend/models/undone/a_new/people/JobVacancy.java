package com.orbitastra.backend.models.undone.a_new.people;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.undone.a_new.base.CampusScopedDocument;
import com.orbitastra.backend.models.undone.a_new.common.PlatformEnums.ApprovalState;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Document(collection = "job_vacancies")
@CompoundIndexes({
        @CompoundIndex(name = "tenant_vacancy_no_uniq",
                def = "{'tenantId':1,'vacancyNo':1}", unique = true),
        @CompoundIndex(name = "tenant_vacancy_state_close_idx",
                def = "{'tenantId':1,'state':1,'closesAt':1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class JobVacancy extends CampusScopedDocument {

    private String vacancyNo;
    private String positionDocsId;
    private Integer openings;
    private String justification;
    private ApprovalState state;
    private Instant opensAt;
    private Instant closesAt;
    private String recruiterDocsId;
    private Boolean publiclyVisible;

    @Builder.Default
    private List<String> requiredSkills = new ArrayList<>();

    @Builder.Default
    private List<String> requiredCredentialCodes = new ArrayList<>();
}
