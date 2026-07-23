package com.orbitastra.backend.models.undone.document;

import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.base.SchoolDocs;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Document(collection = "document_signatures")
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentSignature extends SchoolDocs {

    private String signerId;

    private String signerName;

    private String designation;

    private String signatureUrl;

    @Builder.Default
    private Boolean active = true;
}
