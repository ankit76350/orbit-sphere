package com.orbitastra.backend.models.undone.a_new.dismissal;

import java.time.LocalTime;
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

@Document(collection = "dismissal_plans")
@CompoundIndex(name = "tenant_year_student_dismissal_uniq",
        def = "{'tenantId':1,'academicYearDocsId':1,'studentDocsId':1}", unique = true)
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class DismissalPlan extends AcademicScopedDocument {

    public enum DismissalMode {
        AUTHORIZED_PICKUP,
        SCHOOL_TRANSPORT,
        SELF_DISMISSAL,
        HOSTEL,
        AFTER_SCHOOL_ACTIVITY,
        DAYCARE
    }

    private String studentDocsId;
    private DismissalMode defaultMode;
    private String transportAllocationDocsId;
    private String pickupZoneDocsId;
    private LocalTime normalDismissalTime;
    private String specialInstructions;
    private Boolean active;

    @Builder.Default
    private List<String> pickupAuthorizationDocsIds = new ArrayList<>();
}
