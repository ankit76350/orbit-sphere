package com.orbitastra.backend.models.undone.document;

import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.base.SchoolBase;
import com.orbitastra.backend.models.undone.document.enums.DocumentStatus;
import com.orbitastra.backend.models.undone.document.enums.DocumentCategory;
import com.orbitastra.backend.models.undone.document.enums.HolderType;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Document(collection = "generated_documents")
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class GeneratedDocument extends SchoolBase {

    @Indexed(unique = true)
    private String documentNo;

    @Indexed(unique = true)
    private String verificationCode;

    private DocumentCategory documentType;

    private String entityDocsId; // studentDocsId or staffDocsId

    private HolderType holderType; // Student, Staff

    private DocumentStatus status;

    private String pdfUrl;

    private LocalDateTime generatedAt;
}
