package com.orbitastra.backend.models.identity;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.base.SchoolBase;
import com.orbitastra.backend.models.identity.embedded.RolePermission;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * A named job in the school, and everything somebody doing that job is allowed
 * to do.
 *
 * <p>This is a collection and not a fixed list of names, because schools do not
 * agree on their own job titles. One school has a Vice Principal, another has a
 * Headmistress and a Fee Desk Clerk, and a third wants a Warden who can see
 * hostel students only. A fixed list would force all of them into the same few
 * names.
 *
 * <p>{@code systemManaged} marks the roles the platform ships with, such as
 * School Admin. Those may be looked at but not edited or deleted, so a school
 * cannot lock itself out by taking USER_ACCESS away from its only administrator.
 * A school that wants something different copies the role and edits the copy.
 *
 * <p>Permissions live inside the role rather than in their own collection. They
 * are small, there are never many, and they are always read together the moment
 * somebody logs in.
 *
 * <p>{@code roleKey} is the stable key that role assignments point at. It must
 * not be renamed once accounts are using it. {@code name} is only what staff see
 * on screen and may be reworded at any time.
 *
 * <p>The service checks that a system-managed role is never edited or deleted,
 * that a role still held by somebody is never deleted, and that the permission
 * list has no two lines for the same module.
 */
@Document(collection = "roles")
@CompoundIndexes({
        @CompoundIndex(
                name = "school_role_key_uniq",
                def = "{'schoolId': 1, 'roleKey': 1}",
                unique = true),
        @CompoundIndex(
                name = "school_role_active_idx",
                def = "{'schoolId': 1, 'active': 1, 'name': 1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class Role extends SchoolBase {

    // Stable key that role assignments point at. Must not be renamed once
    // somebody holds this role. Example: "FEE_DESK_CLERK"
    @NotBlank
    private String roleKey;

    // Name shown to staff. May be reworded at any time. Example: "Fee Desk Clerk"
    @NotBlank
    private String name;

    // Example: "Raises fee bills and takes payments, but cannot allow discounts."
    private String description;

    // What somebody holding this role may do. At least one line, because a role
    // that allows nothing is not worth handing out.
    @Valid
    @NotEmpty
    @Builder.Default
    private List<RolePermission> permissions = new ArrayList<>();

    // True for the roles the platform ships with. Those cannot be edited or
    // deleted, which is what stops a school locking itself out. Example: false
    @NotNull
    @Builder.Default
    private Boolean systemManaged = false;

    // Whether this role may still be given to somebody new. Turning it off does
    // not take it away from people who already hold it. Example: true
    @NotNull
    @Builder.Default
    private Boolean active = true;
}
