package com.orbitastra.backend.models.undone.a_new.facilities;

import java.time.Instant;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.undone.a_new.base.CampusScopedDocument;
import com.orbitastra.backend.models.undone.a_new.common.PlatformEnums.ApprovalState;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Document(collection = "resource_bookings")
@CompoundIndexes({
        @CompoundIndex(name = "tenant_booking_no_uniq",
                def = "{'tenantId':1,'bookingNo':1}", unique = true),
        @CompoundIndex(name = "tenant_resource_time_idx",
                def = "{'tenantId':1,'facilityResourceDocsId':1,'startsAt':1,'endsAt':1,'state':1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class ResourceBooking extends CampusScopedDocument {

    private String bookingNo;
    private String facilityResourceDocsId;
    private String requestedByDocsId;
    private String purpose;
    private Instant startsAt;
    private Instant endsAt;
    private ApprovalState state;
    private String approvedByDocsId;
    private String scheduleOccurrenceDocsId;
}
