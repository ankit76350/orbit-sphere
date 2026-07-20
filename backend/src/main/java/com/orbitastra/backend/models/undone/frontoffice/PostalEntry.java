package com.orbitastra.backend.models.undone.frontoffice;

import java.time.LocalDate;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.undone.frontoffice.enums.PostalDirection;
import com.orbitastra.backend.models.undone.frontoffice.enums.PostalMode;
import com.orbitastra.backend.models.undone.frontoffice.enums.PostalStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * An inward/outward postal &amp; courier register entry maintained at reception.
 */
@Document(collection = "postal_entries")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostalEntry {

    @CreatedDate
    private java.time.LocalDateTime createdAt;

    @LastModifiedDate
    private java.time.LocalDateTime updatedAt;

    @Id
    private String id;

    @Indexed
    private String schoolId;

    private PostalDirection direction;

    private LocalDate date;

    // Auto-generated reference, e.g. "IN/2026/012" or "OUT/2026/034".
    @Indexed
    private String refNo;

    private String party;

    private String subject;

    private PostalMode mode;

    private String handler; // references Staff.id

    private PostalStatus status;
}
