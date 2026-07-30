package com.orbitastra.backend.models.undone.a_new.privacy;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.undone.a_new.base.TenantScopedDocument;
import com.orbitastra.backend.models.undone.a_new.common.PlatformEnums.ApprovalState;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Document(collection = "privacy_notices")
@CompoundIndex(name = "tenant_notice_locale_version_uniq",
        def = "{'tenantId':1,'noticeKey':1,'locale':1,'noticeVersion':1}", unique = true)
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class PrivacyNotice extends TenantScopedDocument {

    private String noticeKey;
    private Integer noticeVersion;
    private String locale;
    private String title;
    private String contentDocumentDocsId;
    private ApprovalState state;
    private Instant effectiveFrom;
    private Instant effectiveUntil;

    @Builder.Default
    private List<String> purposeKeys = new ArrayList<>();

    @Builder.Default
    private List<String> audienceTypes = new ArrayList<>();
}
