package com.orbitastra.backend.models.undone.a_new.governance;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.undone.a_new.base.TenantScopedDocument;
import com.orbitastra.backend.models.undone.a_new.common.PlatformEnums.ApprovalState;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Document(collection = "governance_meetings")
@CompoundIndexes({
        @CompoundIndex(name = "tenant_meeting_no_uniq",
                def = "{'tenantId':1,'meetingNo':1}", unique = true),
        @CompoundIndex(name = "tenant_body_meeting_time_idx",
                def = "{'tenantId':1,'governingBodyDocsId':1,'scheduledStart':-1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class GovernanceMeeting extends TenantScopedDocument {

    private String meetingNo;
    private String governingBodyDocsId;
    private String title;
    private Instant scheduledStart;
    private Instant scheduledEnd;
    private String venue;
    private String virtualMeetingUrl;
    private ApprovalState minutesState;
    private String agendaPackDocumentDocsId;
    private String approvedMinutesDocumentDocsId;

    @Builder.Default
    private List<Attendance> attendance = new ArrayList<>();

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Attendance {
        private String membershipDocsId;
        private String attendeeName;
        private Boolean present;
        private Boolean conflictDeclared;
    }
}
