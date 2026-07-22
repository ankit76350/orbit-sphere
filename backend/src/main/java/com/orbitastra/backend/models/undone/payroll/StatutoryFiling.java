package com.orbitastra.backend.models.undone.payroll;

import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;

import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.base.BaseDocument;
import com.orbitastra.backend.models.undone.payroll.enums.StatutoryReportType;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A record of a generated/submitted statutory payroll filing (PF ECR, ESI
 * return, professional tax, Form 16, ...) for the compliance register.
 */
@Document(collection = "statutory_filings")
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class StatutoryFiling extends BaseDocument {

    private StatutoryReportType reportType;

    // Pay period the filing covers, in "YYYY-MM" form.
    private String period;

    private String fileUrl;

    private LocalDate filedDate;

    private String filedBy; // references Staff.id / User.id
}
