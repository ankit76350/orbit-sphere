package com.orbitastra.backend.models.undone.document;

import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.base.BaseDocument;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Document(collection = "generated_documents")
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class GeneratedDocument extends BaseDocument {

    private String documentNumber;

    private String documentType;

    private String entityId; // studentId or staffId

    private String entityType; // Student, Staff

    private String pdfUrl;

    private String verificationCode;

    private LocalDateTime generatedAt;

    private String status; // Valid, Expired, Revoked
}
