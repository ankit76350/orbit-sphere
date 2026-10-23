package com.orbitastra.backend.repositories.identity;

import java.util.List;

import com.orbitastra.backend.models.identity.Role;
import com.orbitastra.backend.models.identity.embedded.RoleDefinition;

/**
 * The part of {@link RoleRepository} that cannot be a derived query method.
 *
 * <p>Every role lives inside one document per school, so adding one is a {@code $push} rather
 * than an insert, and a guarded {@code $push} where the key must stay unique. See {@link Role}.
 *
 * <p>Spring Data finds the implementation by name: {@code RoleRepositoryImpl}. Renaming that
 * class breaks the wiring silently at startup, so do not.
 */
public interface RoleRepositoryCustom {

    /**
     * Adds a role only if the school has none with that key.
     *
     * <p>This is what replaced the old unique index on {@code schoolId + roleKey}. A unique index
     * cannot protect an array, so the condition has to be part of the write.
     *
     * @return true when this call added it, false when it was already there
     */
    boolean addRoleIfAbsent(String schoolId, RoleDefinition role);

    /**
     * Adds several roles in one write, for provisioning.
     *
     * <p>A {@code $push} rather than saving the document back, because a full save would put
     * every existing role's permissions back to whatever was read — and a school may have edited
     * them.
     *
     * @return how many were added
     */
    int addRoles(String schoolId, List<RoleDefinition> roles);

    /** Switches one role on or off without touching any other entry in the array. */
    boolean setRoleActive(String schoolId, String roleKey, boolean active);
}
