package com.orbitastra.backend.models.undone.document;

import com.orbitastra.backend.models.BaseDocument;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Document(collection = "document_templates")
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentTemplate extends BaseDocument {

    private String name;

    private String category;

    private String templateContent; // JSON template string or rich content

    private String status; // Active, Inactive
}
