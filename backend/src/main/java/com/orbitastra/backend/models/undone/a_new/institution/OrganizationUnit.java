package com.orbitastra.backend.models.undone.a_new.institution;

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.undone.a_new.base.TenantScopedDocument;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Document(collection = "organization_units")
@CompoundIndexes({
        @CompoundIndex(name = "tenant_unit_code_uniq", def = "{'tenantId':1,'code':1}", unique = true),
        @CompoundIndex(name = "tenant_parent_type_idx", def = "{'tenantId':1,'parentUnitDocsId':1,'type':1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class OrganizationUnit extends TenantScopedDocument {

    public enum UnitType {
        SCHOOL_GROUP,
        TRUST_OR_SOCIETY,
        LEGAL_ENTITY,
        CAMPUS,
        DEPARTMENT,
        COST_CENTRE
    }

    private String parentUnitDocsId;
    private UnitType type;
    private String code;
    private String legalName;
    private String displayName;
    private String registrationNo;
    private String taxRegistrationNo;
    private String defaultCurrency;
    private ZoneId timeZone;
    private String locale;

    @Builder.Default
    private List<String> supportedLocales = new ArrayList<>();

    private Address registeredAddress;
    private Contact primaryContact;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Address {
        private String line1;
        private String line2;
        private String city;
        private String state;
        private String postalCode;
        private String countryCode;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Contact {
        private String name;
        private String email;
        private String phone;
    }
}
