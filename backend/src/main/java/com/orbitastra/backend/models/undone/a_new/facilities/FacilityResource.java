package com.orbitastra.backend.models.undone.a_new.facilities;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.undone.a_new.base.CampusScopedDocument;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Document(collection = "facility_resources")
@CompoundIndexes({
        @CompoundIndex(name = "tenant_campus_facility_code_uniq",
                def = "{'tenantId':1,'campusDocsId':1,'resourceCode':1}", unique = true),
        @CompoundIndex(name = "tenant_parent_facility_idx",
                def = "{'tenantId':1,'parentResourceDocsId':1,'type':1,'sortOrder':1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class FacilityResource extends CampusScopedDocument {

    public enum ResourceType {
        SITE,
        BUILDING,
        FLOOR,
        ROOM,
        CLASSROOM,
        LABORATORY,
        SPORTS_AREA,
        AUDITORIUM,
        MEETING_ROOM,
        EQUIPMENT,
        PARKING,
        OTHER
    }

    private String parentResourceDocsId;
    private String resourceCode;
    private String name;
    private ResourceType type;
    private String locationDescription;
    private Integer capacity;
    private Integer sortOrder;
    private Boolean bookable;
    private Boolean accessible;

    @Builder.Default
    private List<String> amenityCodes = new ArrayList<>();
}
