package com.orbitastra.backend.models.undone.compliance;

import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;

import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.base.SchoolDocs;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A dated compliance obligation on the school's regulatory calendar (UDISE+
 * return, board affiliation renewal, safety audit, ...) owned by an issuing
 * authority.
 */
@Document(collection = "compliance_tasks")
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class ComplianceTask extends SchoolDocs {

    private String title;

    // Issuing/regulating body, e.g. "CBSE", "UDISE+", "State Board".
    private String authority;

    private LocalDate dueDate;

    @Builder.Default
    private Boolean completed = false;
}
