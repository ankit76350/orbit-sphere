package com.orbitastra.backend.models.undone.a_working.frontoffice;

import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;

import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.base.SchoolBase;
import com.orbitastra.backend.models.undone.a_working.frontoffice.enums.PostalDirection;
import com.orbitastra.backend.models.undone.a_working.frontoffice.enums.PostalMode;
import com.orbitastra.backend.models.undone.a_working.frontoffice.enums.PostalStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
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

    @Indexed(unique = true)
    private String referenceNumber;

    private LocalDate date;

    /**
     * Sender or Receiver
     */
    private String partyName;

    private String address;

    private String subject;

    private PostalMode mode;

    /**
     * Courier Tracking ID
     */
    private String trackingNumber;

    /**
     * Staff handling this.
     */
    @Indexed
    private String handledByDocsId;

    private String attachmentUrl;

    private String remarks;

    @Builder.Default
    private PostalStatus status = PostalStatus.RECEIVED;
}
