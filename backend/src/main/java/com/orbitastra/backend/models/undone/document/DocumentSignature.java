package com.orbitastra.backend.models.undone.document;

import com.orbitastra.backend.models.BaseDocument;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

import org.springframework.data.mongodb.core.mapping.Document;

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
public class DocumentSignature extends BaseDocument {

    private String signerId;

    private String signerName;

    private String designation;

    private String signatureUrl;

    @Builder.Default
    private Boolean active = true;
}
