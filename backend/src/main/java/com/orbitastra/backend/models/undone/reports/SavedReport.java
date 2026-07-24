package com.orbitastra.backend.models.undone.reports;

import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

import java.util.List;

import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.base.SchoolBase;
import com.orbitastra.backend.models.undone.reports.enums.ReportSource;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A saved custom-report definition from the report builder — the source domain,
 * the filter conditions and the columns to output. Statutory registers
 * (attendance, fee, TC, stock, visitor) are live views over existing
 * collections and are intentionally not modelled here.
 */
@Document(collection = "saved_reports")
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class SavedReport extends SchoolBase {

    private String name;

    private ReportSource source;

    @Builder.Default
    private List<FilterCondition> filters = new java.util.ArrayList<>();

    // Field names selected for output, in order.
    @Builder.Default
    private List<String> columns = new java.util.ArrayList<>();

    private String createdByName;

    /** One filter row in the report builder. */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FilterCondition {
        private String field;
        private String op;    // e.g. "equals", "contains"
        private String value;
    }
}
