package com.orbitastra.backend.models.undone.a_new.communication;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.undone.a_new.base.AcademicScopedDocument;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Document(collection = "ptm_events")
@CompoundIndex(name = "tenant_ptm_event_no_uniq",
        def = "{'tenantId':1,'eventNo':1}", unique = true)
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class PtmEvent extends AcademicScopedDocument {

    private String eventNo;
    private String title;
    private Instant startsAt;
    private Instant endsAt;
    private Integer slotMinutes;
    private String locationDocsId;
    private String virtualMeetingProvider;
    private String status;

    @Builder.Default
    private List<String> classNodeDocsIds = new ArrayList<>();

    @Builder.Default
    private List<String> teacherDocsIds = new ArrayList<>();
}
