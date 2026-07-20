package com.orbitastra.backend.models.undone.reports;

import java.time.LocalDateTime;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
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
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScheduledReport {

    @CreatedDate
    private java.time.LocalDateTime createdAt;

    @LastModifiedDate
    private java.time.LocalDateTime updatedAt;

    @Id
    private String id;

    @Indexed
    private String schoolId;

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
