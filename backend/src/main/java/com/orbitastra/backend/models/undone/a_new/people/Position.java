package com.orbitastra.backend.models.undone.a_new.people;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.undone.a_new.base.CampusScopedDocument;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Document(collection = "positions")
@CompoundIndex(name = "tenant_campus_position_code_uniq",
        def = "{'tenantId':1,'campusDocsId':1,'positionCode':1}", unique = true)
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class Position extends CampusScopedDocument {

    private String positionCode;
    private String title;
    private String departmentDocsId;
    private String reportsToPositionDocsId;
    private String employmentType;
    private Integer approvedHeadcount;
    private Integer filledHeadcount;
    private String payGradeKey;
    private Boolean teachingPosition;
    private Boolean active;
}
