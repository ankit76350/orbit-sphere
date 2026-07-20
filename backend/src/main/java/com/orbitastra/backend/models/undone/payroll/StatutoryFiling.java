package com.orbitastra.backend.models.undone.payroll;

import java.time.LocalDate;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.undone.payroll.enums.StatutoryReportType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A record of a generated/submitted statutory payroll filing (PF ECR, ESI
 * return, professional tax, Form 16, ...) for the compliance register.
 */
@Document(collection = "statutory_filings")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StatutoryFiling {

    @CreatedDate
    private java.time.LocalDateTime createdAt;

    @LastModifiedDate
    private java.time.LocalDateTime updatedAt;

    @Id
    private String id;

    @Indexed
    private String schoolId;

    private StatutoryReportType reportType;

    // Pay period the filing covers, in "YYYY-MM" form.
    private String period;

    private String fileUrl;

    private LocalDate filedDate;

    private String filedBy; // references Staff.id / User.id
}
