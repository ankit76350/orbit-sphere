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
 */
@RequiredArgsConstructor
public class RoleRepositoryImpl implements RoleRepositoryCustom {

    private final MongoTemplate mongo;

    @Override
    public boolean addRoleIfAbsent(String schoolId, RoleDefinition role) {
        // Push only when no entry with that key exists. Two callers racing on provisioning both
        // run this; one pushes, the other matches nothing and does nothing.
        Query absent = new Query(Criteria.where("schoolId").is(schoolId)
                .and("roles.roleKey").ne(role.getRoleKey()));

        return mongo.updateFirst(absent, new Update().push("roles", role), Role.class)
                .getModifiedCount() > 0;
    }

    @Override
    public int addRoles(String schoolId, List<RoleDefinition> roles) {
        if (roles == null || roles.isEmpty()) {
            return 0;
        }
        mongo.updateFirst(school(schoolId),
                new Update().push("roles").each(roles.toArray()),
                Role.class);
        return roles.size();
    }

    @Override
    public boolean setRoleActive(String schoolId, String roleKey, boolean active) {
        // The array element is matched in the query, so $ points at that one role.
        Query one = new Query(Criteria.where("schoolId").is(schoolId)
                .and("roles.roleKey").is(roleKey));

        return mongo.updateFirst(one, new Update().set("roles.$.active", active), Role.class)
                .getModifiedCount() > 0;
    }

    private Query school(String schoolId) {
        return new Query(Criteria.where("schoolId").is(schoolId));
    }
}
