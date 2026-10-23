package com.orbitastra.backend.models.identity.embedded;

import java.util.ArrayList;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * One role inside a school's Role document.
 *
 * <p>Was its own document until 2026-09-05, one per role per school. Everything below is
 * unchanged; only where it lives moved. See Role for what that costs.
 *
 * <p><b>roleKey is the identity now, not an ObjectId.</b> An embedded entry has no {@code _id},
 * so nothing can point at one with a {@code *DocsId}. That is why
 * {@code UserAccount.roleDocsIds} became {@code UserAccount.roleKeys} on the same day. It is
 * arguably the better key anyway: it was already unique per school, it is readable in a database
 * shell, and it matches how the rest of this system refers to things — an academic year by name,
 * a subscription by number.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoleDefinition {

    // The stable key a UserAccount holds, and what everything else in the system means when it
    // says "which role". Unique within a school.
    //
    // NEVER RENAME ONE. Accounts store this string, and nothing would fail or cascade — every
    // account would simply stop matching a role that no longer answers to that key. Same shape
    // of problem as renaming an academic year.
    //
    // Example: "SCHOOL_ADMIN"
    @NotBlank
    private String roleKey;

    // What a person sees on screen. This one CAN be renamed freely; nothing joins on it.
    // Example: "School Administrator"
    @NotBlank
    private String name;

    // Example: "Full access to every module. The role the first account holds."
    private String description;

    // What the role may actually do, one entry per module it touches. A role with no permissions
    // is a role that grants nothing, which is a mistake rather than a use case.
    @Valid
    @NotEmpty
    @Builder.Default
    private List<RolePermission> permissions = new ArrayList<>();

    // True for the three roles complete-provisioning seeds. Marks them as ours rather than the
    // school's, so a school cannot delete SCHOOL_ADMIN and lock itself out.
    // Example: true
    @NotNull
    @Builder.Default
    private Boolean systemManaged = false;

    // Switched off rather than removed, so accounts that hold it keep a readable history of what
    // they were granted.
    // Example: true
    @NotNull
    @Builder.Default
    private Boolean active = true;

    // NO recordState HERE, even though the old document had one through SchoolBase. Use active
    // above; the container carries the one recordState for the school's whole set.
}
