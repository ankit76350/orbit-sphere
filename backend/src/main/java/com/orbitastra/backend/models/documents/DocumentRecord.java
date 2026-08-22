package com.orbitastra.backend.models.documents;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.base.SchoolBase;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * Minimum metadata required to locate and download one school-owned file.
 *
 * <p>{@code objectKey} points directly to the private file in the application's
 * configured S3, Blob Storage, MinIO, or other object-storage service. Public or
 * temporary signed URLs must never be persisted.
 */
@Document(collection = "document_records")
@CompoundIndex(
        name = "school_document_object_key_uniq",
        def = "{'schoolId': 1, 'objectKey': 1}",
        unique = true)
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentRecord extends SchoolBase {

    // Internal object-storage key; never a public or signed URL.
    // Example: "schools/67aa15/staff/identity/document-01.pdf"
    @NotBlank
    private String objectKey;

    // Original name presented to the user. Example: "aadhaar-card.pdf"
    @NotBlank
    private String originalFileName;

    // IANA media type. Example: "application/pdf"
    private String mediaType;

    // File size in bytes. Example: 248321
    private Long sizeBytes;
}
