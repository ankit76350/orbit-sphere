package com.orbitastra.backend.models.new_new.gate;

import java.time.Instant;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.new_new.base.SchoolBase;
import com.orbitastra.backend.models.new_new.gate.enums.VisitorType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * One person from outside the school, kept so they do not have to be written down
 * again on every visit.
 *
 * <p>This is the person, not the visit. The visit is a VisitorPass. Keeping them
 * apart is the point: the courier who comes three times a week is one Visitor and
 * hundreds of passes, and the guard should be able to find them by phone number
 * rather than retyping their name and ID each time.
 *
 * <p>The contact number and the ID document are private, so they are never stored
 * as plain text. Three fields do three jobs, the same way BankAccount handles an
 * account number: the encrypted field holds the real value, the lookup hash lets
 * the guard find the same person again without anything being decrypted, and the
 * masked field is the only version safe to show on a screen at a gate where
 * anybody can see it.
 *
 * <p>{@code blocked} is the field with teeth. Somebody who has been told not to
 * come back must be stopped at the gate, and a guard cannot be expected to
 * remember a name from a note passed round months ago. A blocked visitor is
 * refused a new pass, and {@code blockReason} tells the guard what to say.
 *
 * <p>A parent of a student here is normally not a Visitor at all. They have a
 * Guardian record and, in many schools, an ID card. This collection is for people
 * with no other place in the system.
 *
 * <p>The service checks that a blocked visitor is never issued a pass, that the ID
 * evidence is present for visitor types the school requires it for, and that the
 * masked value never carries more than the last few characters.
 */
@Document(collection = "visitors")
@CompoundIndexes({
        @CompoundIndex(
                name = "school_visitor_contact_uniq",
                def = "{'schoolId': 1, 'contactLookupHash': 1}",
                unique = true),
        @CompoundIndex(
                name = "school_visitor_identity_idx",
                def = "{'schoolId': 1, 'identityLookupHash': 1}"),
        @CompoundIndex(
                name = "school_visitor_name_idx",
                def = "{'schoolId': 1, 'fullName': 1}"),
        @CompoundIndex(
                name = "school_visitor_blocked_idx",
                def = "{'schoolId': 1, 'blocked': 1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class Visitor extends SchoolBase {

    // Name as the visitor gives it. Example: "Ramesh Iyer"
    @NotBlank
    private String fullName;

    // Which sort of visitor they usually are. A pass may say something different
    // for one particular visit. Example: VisitorType.VENDOR
    @NotNull
    private VisitorType visitorType;

    // Company or office they come from, when they come on business.
    // Example: "Sunrise Stationers"
    private String organizationName;

    // Real phone number, encrypted before it is saved.
    // Example: "enc:v1:5c4b3a2918273645"
    @NotBlank
    private String encryptedContactNumber;

    // One-way hash used to find the same visitor again without decrypting.
    // Example: "sha256:1f2e3d4c5b6a79880011223344556677"
    @NotBlank
    private String contactLookupHash;

    // The only version safe to show on a gate screen. Example: "XXXXXX3210"
    @NotBlank
    private String maskedContactNumber;

    // What ID they showed, such as an Aadhaar or a driving licence.
    // Example: "AADHAAR"
    private String identityDocumentType;

    // The ID number, encrypted before it is saved.
    // Example: "enc:v1:9a8b7c6d5e4f3021"
    private String encryptedIdentityNumber;

    // One-way hash of the ID number, so the same document can be recognised
    // without decrypting. Example: "sha256:aabbccdd11223344556677889900aabb"
    private String identityLookupHash;

    // The only version of the ID safe to show on screen. Example: "XXXX XXXX 4821"
    private String maskedIdentityNumber;

    // Links to DocumentRecord.id for a photo taken at the gate.
    // Example: "67b61122dc3f7d0011223344"
    private String photoDocumentDocsId;

    // Whether this person must be refused entry. Example: false
    @NotNull
    @Builder.Default
    private Boolean blocked = false;

    // Why they are refused, shown to the guard so they know what to say.
    // Example: "Asked not to return after the argument on 12 July."
    private String blockReason;

    // Links to the staff identity that blocked them.
    // Example: "67aa15d9dc3f7d0055555555"
    private String blockedByDocsId;

    // Example: 2026-07-12T11:20:00Z
    private Instant blockedAt;

    // Example: "Delivers stationery, usually on Mondays."
    private String remarks;
}
