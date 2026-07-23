package com.orbitastra.backend.models.undone.frontoffice;

import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;

import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.base.SchoolBase;
import com.orbitastra.backend.models.undone.frontoffice.enums.PostalDirection;
import com.orbitastra.backend.models.undone.frontoffice.enums.PostalMode;
import com.orbitastra.backend.models.undone.frontoffice.enums.PostalStatus;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * An inward/outward postal &amp; courier register entry maintained at reception.
 */
@Document(collection = "postal_entries")
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class PostalEntry extends SchoolBase {

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
