package com.orbitastra.backend.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Document(collection = "document_signatures")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentSignature {

    @Id
    private String id;

    private String schoolId;

    private String signerId;

    private String signerName;

    private String designation;

    private String signatureUrl;

    @Builder.Default
    private Boolean active = true;
}
