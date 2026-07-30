package com.orbitastra.backend.models.new_new.institution;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.new_new.base.SchoolBase;
import com.orbitastra.backend.models.new_new.institution.enums.AffiliationStatus;
import com.orbitastra.backend.models.new_new.institution.enums.EducationBoard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Document(collection = "affiliation_programmes")
@CompoundIndexes({
        @CompoundIndex(
                name = "school_programme_code_uniq",
                def = "{'schoolId': 1, 'programmeCode': 1}",
                unique = true),
        @CompoundIndex(
                name = "school_board_affiliation_no_uniq",
                def = "{'schoolId': 1, 'board': 1, 'affiliationNumber': 1}",
                unique = true,
                partialFilter = "{'affiliationNumber': {'$type': 'string'}}"),
        @CompoundIndex(
                name = "school_affiliation_status_expiry_idx",
                def = "{'schoolId': 1, 'status': 1, 'affiliationValidUntil': 1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class AffiliationProgramme extends SchoolBase {

    // Example: EducationBoard.CBSE
    private EducationBoard board;

    // Example: "Maharashtra State Board of Secondary and Higher Secondary Education"
    private String boardName;

    // Example: "CBSE_SECONDARY"
    private String programmeCode;

    // Example: "CBSE Secondary School Programme"
    private String programmeName;

    // Example: "1130456"
    private String affiliationNumber;

    // Example: 2025-04-01
    private LocalDate affiliationValidFrom;

    // Example: 2030-03-31
    private LocalDate affiliationValidUntil;

    // Example: "ENGLISH"
    private String mediumOfInstruction;

    // Example: AffiliationStatus.ACTIVE
    @Builder.Default
    private AffiliationStatus status = AffiliationStatus.DRAFT;

    // Example: ["GRADE_1", "GRADE_2", "GRADE_10"]
    @Builder.Default
    private List<String> gradeCodes = new ArrayList<>();
}
