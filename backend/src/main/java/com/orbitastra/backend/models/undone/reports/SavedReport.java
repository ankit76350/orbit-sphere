package com.orbitastra.backend.models.undone.reports;

import java.util.List;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

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
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SavedReport {

    @CreatedDate
    private java.time.LocalDateTime createdAt;

    @LastModifiedDate
    private java.time.LocalDateTime updatedAt;

    @Id
    private String id;

    @Indexed
    private String schoolId;

    private String name;

    private ReportSource source;

    @Builder.Default
    private List<FilterCondition> filters = new java.util.ArrayList<>();

    // Field names selected for output, in order.
    @Builder.Default
    private List<String> columns = new java.util.ArrayList<>();

    private String createdBy; // references Staff.id / User.id

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
