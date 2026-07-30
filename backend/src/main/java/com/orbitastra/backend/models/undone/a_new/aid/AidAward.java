package com.orbitastra.backend.models.undone.a_new.aid;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.undone.a_new.base.AcademicScopedDocument;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Document(collection = "aid_awards")
@CompoundIndexes({
        @CompoundIndex(name = "tenant_award_no_uniq",
                def = "{'tenantId':1,'awardNo':1}", unique = true),
        @CompoundIndex(name = "tenant_student_award_status_idx",
                def = "{'tenantId':1,'studentDocsId':1,'status':1,'validUntil':1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class AidAward extends AcademicScopedDocument {

    public enum AwardStatus {
        ACTIVE,
        SUSPENDED,
        WITHDRAWN,
        COMPLETED,
        RENEWAL_DUE
    }

    private String awardNo;
    private String aidApplicationDocsId;
    private String aidProgrammeDocsId;
    private String studentDocsId;
    private BigDecimal awardAmount;
    private String currencyCode;
    private LocalDate validFrom;
    private LocalDate validUntil;
    private AwardStatus status;
    private String renewalCondition;
    private BigDecimal utilizedAmount;

    @Builder.Default
    private List<String> invoiceAllocationDocsIds = new ArrayList<>();
}
