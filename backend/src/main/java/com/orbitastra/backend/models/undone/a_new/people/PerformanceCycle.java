package com.orbitastra.backend.models.undone.a_new.people;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.undone.a_new.base.TenantScopedDocument;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Document(collection = "performance_cycles")
@CompoundIndex(name = "tenant_performance_cycle_key_uniq",
        def = "{'tenantId':1,'cycleKey':1}", unique = true)
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class PerformanceCycle extends TenantScopedDocument {

    private String cycleKey;
    private String name;
    private LocalDate startDate;
    private LocalDate endDate;
    private String status;
    private Boolean anonymousPeerFeedback;

    @Builder.Default
    private List<Criterion> criteria = new ArrayList<>();

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Criterion {
        private String criterionKey;
        private String label;
        private String respondentType;
        private Integer weightPercent;
        private Integer maximumScore;
    }
}
