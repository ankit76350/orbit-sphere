package com.orbitastra.backend.models.undone.a_new.people;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.undone.a_new.base.TenantScopedDocument;
import com.orbitastra.backend.models.undone.a_new.common.PlatformEnums.Confidentiality;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * Canonical human record shared by student, guardian, staff and alumni profiles.
 * Searchable sensitive values use keyed blind indexes; raw values are encrypted.
 */
@Document(collection = "person_records")
@CompoundIndexes({
        @CompoundIndex(name = "tenant_person_no_uniq",
                def = "{'tenantId':1,'personNo':1}", unique = true),
        @CompoundIndex(name = "tenant_government_id_lookup_uniq",
                def = "{'tenantId':1,'governmentIdLookupHash':1}", unique = true,
                partialFilter = "{'governmentIdLookupHash':{'$type':'string'}}"),
        @CompoundIndex(name = "tenant_contact_lookup_idx",
                def = "{'tenantId':1,'contactLookupHashes':1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class PersonRecord extends TenantScopedDocument {

    private String personNo;
    private String displayName;
    private String sortName;
    private LocalDate dateOfBirth;
    private String genderCode;
    private String nationalityCode;
    private String preferredLanguage;
    private String encryptedDemographics;
    private String encryptedContacts;
    private String encryptedAddresses;
    private String governmentIdLookupHash;
    private Confidentiality confidentiality;

    @Builder.Default
    private List<String> contactLookupHashes = new ArrayList<>();

    @Builder.Default
    private List<String> profileTypeCodes = new ArrayList<>();
}
