package com.orbitastra.backend.models.undone.compliance.embedded;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ComplianceDocument {

    /**
     * Name of the document.
     * Example: Fire Safety Certificate
     */
    private String name;

    /**
     * File URL or storage key.
     */
    private String fileUrl;
}