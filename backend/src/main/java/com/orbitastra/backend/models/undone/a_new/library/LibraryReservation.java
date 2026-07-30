package com.orbitastra.backend.models.undone.a_new.library;

import java.time.Instant;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.undone.a_new.base.CampusScopedDocument;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Document(collection = "library_reservations")
@CompoundIndexes({
        @CompoundIndex(name = "tenant_book_borrower_active_uniq",
                def = "{'tenantId':1,'bookDocsId':1,'borrowerType':1,'borrowerDocsId':1,'active':1}",
                unique = true, partialFilter = "{'active':true}"),
        @CompoundIndex(name = "tenant_book_queue_idx",
                def = "{'tenantId':1,'bookDocsId':1,'active':1,'queuePosition':1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class LibraryReservation extends CampusScopedDocument {

    private String bookDocsId;
    private String borrowerType;
    private String borrowerDocsId;
    private Instant requestedAt;
    private Integer queuePosition;
    private String status;
    private Boolean active;
    private String allocatedBookCopyDocsId;
    private Instant readyAt;
    private Instant holdExpiresAt;
    private Instant fulfilledAt;
}
