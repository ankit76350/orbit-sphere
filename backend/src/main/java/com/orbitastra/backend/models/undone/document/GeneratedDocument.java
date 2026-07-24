package com.orbitastra.backend.models.undone.document;

import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.base.SchoolBase;

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

    private String documentNo;

    private String documentType;

    private String entityDocsId; // studentDocsId or staffDocsId

    private String entityType; // Student, Staff

    private String pdfUrl;

    private String verificationCode;

    private LocalDateTime generatedAt;

    private String status; // Valid, Expired, Revoked
}
