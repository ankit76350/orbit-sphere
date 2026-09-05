package com.orbitastra.backend.repositories.identity;

import java.util.List;

import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import com.orbitastra.backend.models.identity.Role;
import com.orbitastra.backend.models.identity.embedded.RoleDefinition;

import lombok.RequiredArgsConstructor;

/**
 * The array writes behind a school's roles.
 *
 * <p><b>Everything here is one operation.</b> None of these methods reads a document, changes it
 * in Java and writes it back: {@code save()} on the roles document takes part in the
 * {@code @Version} optimistic locking it inherits, so two admins editing two different roles at
 * once would collide.
 *
 * <p>Every method builds its query first and runs it second, so what is being asked for and what
 * is being done with it can be read apart.
 */
@RequiredArgsConstructor
public class RoleRepositoryImpl implements RoleRepositoryCustom {

    private final MongoTemplate mongo;

    @Override
    public boolean addRoleIfAbsent(String schoolId, RoleDefinition role) {
        //! step 1 - build the query: this school, and NO role already holding that key. The
        //! guard lives here because a unique index cannot protect an array.
        Query query = new Query(Criteria.where("schoolId").is(schoolId)
                .and("roles.roleKey").ne(role.getRoleKey()));

        //! step 2 - build the update
        Update update = new Update().push("roles", role);

        //! step 3 - run it. Two callers racing on provisioning both get here; one matches and
        //! pushes, the other matches nothing.
        // TODO: write role (add one if the key is free)
        long modified = mongo.updateFirst(query, update, Role.class).getModifiedCount();

        //! step 4 - say whether this call was the one that added it
        return modified > 0;
    }

    @Override
    public int addRoles(String schoolId, List<RoleDefinition> roles) {
        //! step 1 - nothing to add means no write at all
        if (roles == null || roles.isEmpty()) {
            return 0;
        }

        //! step 2 - build the query
        Query query = school(schoolId);

        //! step 3 - build the update. A push rather than saving the document back, because a
        //! full save would put every existing role's permissions back to whatever was read —
        //! and a school may have edited them.
        Update update = new Update().push("roles").each(roles.toArray());

        //! step 4 - run it
        // TODO: write roles (add the missing ones)
        mongo.updateFirst(query, update, Role.class);

        //! step 5 - report how many went in
        return roles.size();
    }

    @Override
    public boolean setRoleActive(String schoolId, String roleKey, boolean active) {
        //! step 1 - build the query, matching the one array entry so $ points at that role
        Query query = new Query(Criteria.where("schoolId").is(schoolId)
                .and("roles.roleKey").is(roleKey));

        //! step 2 - build the update, which touches that entry and no other
        Update update = new Update().set("roles.$.active", active);

        //! step 3 - run it
        // TODO: write role (switch one on or off)
        long modified = mongo.updateFirst(query, update, Role.class).getModifiedCount();

        //! step 4 - false means no role of that key, or it was already in that state
        return modified > 0;
    }

    /** Builds the query for the school's one document. Runs nothing. */
    private Query school(String schoolId) {
        return new Query(Criteria.where("schoolId").is(schoolId));
    }
}
