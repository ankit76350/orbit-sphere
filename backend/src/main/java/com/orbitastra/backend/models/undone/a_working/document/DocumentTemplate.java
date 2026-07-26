package com.orbitastra.backend.models.undone.a_working.document;

import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.base.SchoolBase;
import com.orbitastra.backend.models.undone.a_working.document.enums.DocumentCategory;
import com.orbitastra.backend.models.undone.a_working.document.enums.TemplateStatus;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Document(collection = "document_templates")
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentTemplate extends SchoolBase {

    private DocumentCategory category;

    private TemplateStatus status;
}
