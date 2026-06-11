package com.orbitastra.backend.models.document;

import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Document(collection = "generated_documents")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GeneratedDocument {

    @Id
    private String id;

    private String schoolId;

    private String documentNumber;

    private String documentType;

    private String entityId; // studentId or staffId

    private String entityType; // Student, Staff

    private String pdfUrl;

    private String verificationCode;

    private LocalDateTime generatedAt;

    private String status; // Valid, Expired, Revoked
}
