package com.orbitastra.backend.models.undone.a_new.institution;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.undone.a_new.base.CampusScopedDocument;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Document(collection = "affiliation_programmes")
@CompoundIndexes({
        @CompoundIndex(name = "tenant_campus_programme_code_uniq",
                def = "{'tenantId':1,'campusDocsId':1,'programmeCode':1}", unique = true),
        @CompoundIndex(name = "tenant_board_expiry_idx",
                def = "{'tenantId':1,'board':1,'affiliationValidUntil':1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class AffiliationProgramme extends CampusScopedDocument {

    public enum Board {
        CBSE,
        CISCE,
        STATE_BOARD,
        IB,
        CAMBRIDGE,
        NATIONAL,
        OTHER
    }

    private Board board;
    private String programmeCode;
    private String programmeName;
    private String affiliationNo;
    private LocalDate affiliationValidFrom;
    private LocalDate affiliationValidUntil;
    private String mediumOfInstruction;

    @Builder.Default
    private List<String> gradeCodes = new ArrayList<>();

    @Builder.Default
    private List<String> accreditationDocumentDocsIds = new ArrayList<>();
}
