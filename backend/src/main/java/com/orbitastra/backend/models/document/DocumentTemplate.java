package com.orbitastra.backend.models.document;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Document(collection = "document_templates")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentTemplate {

    @Id
    private String id;

    private String schoolId;

    private String name;

    private String category;

    private String templateContent; // JSON template string or rich content

    private String status; // Active, Inactive
}
