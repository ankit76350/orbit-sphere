package com.orbitastra.backend.models.undone.reports;

import com.orbitastra.backend.models.BaseDocument;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.undone.reports.enums.ReportFrequency;
import com.orbitastra.backend.models.undone.reports.enums.ScheduledReportStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A recurring delivery of a report to recipients over a channel on a schedule.
 */
@Document(collection = "scheduled_reports")
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class ScheduledReport extends BaseDocument {

    private String reportName;

    private ReportFrequency frequency;

    // Comma-separated recipient addresses/numbers.
    private String recipients;

    // Delivery channel, e.g. "Email", "WhatsApp".
    private String channel;

    private LocalDateTime lastRun;

    @Builder.Default
    private ScheduledReportStatus status = ScheduledReportStatus.ACTIVE;
}
