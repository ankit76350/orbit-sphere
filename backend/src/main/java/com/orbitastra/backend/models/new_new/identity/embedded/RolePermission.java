package com.orbitastra.backend.models.new_new.identity.embedded;

import java.util.LinkedHashSet;
import java.util.Set;

import com.orbitastra.backend.models.new_new.identity.enums.AppModule;
import com.orbitastra.backend.models.new_new.identity.enums.DataScope;
import com.orbitastra.backend.models.new_new.identity.enums.PermissionAction;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * What one role may do in one part of the application.
 *
 * <p>It has no collection of its own. The permissions of a role are always read
 * together when somebody logs in, so they live inside the role rather than in a
 * collection where half of them could go missing.
 *
 * <p>Read it as one sentence: "in this module, this role may do these things, to
 * this many records." For example, in ATTENDANCE a class teacher may VIEW, CREATE
 * and EDIT, but only for the classes they are ASSIGNED to.
 *
 * <p>A module the role has no entry for means the role may do nothing there. We
 * do not store "no access" rows, so a role's permission list only ever says what
 * is allowed. Nothing is granted by leaving something out.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RolePermission {

    // Part of the application this line is about. Example: AppModule.ATTENDANCE
    @NotNull
    private AppModule module;

    // What the role may do here. At least one, or the line should not exist.
    // Example: [VIEW, CREATE, EDIT]
    @NotEmpty
    @Builder.Default
    private Set<PermissionAction> actions = new LinkedHashSet<>();

    // How many records those actions reach. Example: DataScope.ASSIGNED
    @NotNull
    @Builder.Default
    private DataScope scope = DataScope.OWN;
}
