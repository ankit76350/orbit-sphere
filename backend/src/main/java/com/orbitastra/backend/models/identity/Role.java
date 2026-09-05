package com.orbitastra.backend.models.identity;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.base.SchoolBase;
import com.orbitastra.backend.models.identity.embedded.RoleDefinition;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * All of one school's roles, in one document.
 *
 * <p><b>Restructured on 2026-09-05, alongside NumberSequence and for the same reason.</b> Until
 * then each role was its own document, so a provisioned school held three. Now there is exactly
 * one document per school holding a {@code roles} array, which is what the unique index on
 * {@code schoolId} enforces.
 *
 * <p><b>The class is still called Role and now holds many of them.</b> That is deliberate: the
 * collection is still {@code roles} and renaming the type would touch the repository, the
 * seeding, and a dozen READMEs for no behavioural gain. Read it as "the school's roles
 * document". Same call as NumberSequence, which likewise kept its name and grew a
 * {@code counters} array.
 *
 * <h2>What moved to the service</h2>
 *
 * <p><b>Adding a role needs a guarded {@code $push}, because no index can stop a duplicate.</b>
 * The old {@code school_role_key_uniq} index made two {@code SCHOOL_ADMIN} rows impossible. A
 * unique index cannot do that inside an array — Mongo de-duplicates the identical keys one
 * document generates — so the condition has to be in the query:
 *
 * <pre>
 * updateOne(
 *     query:  { schoolId: X, "roles.roleKey": { $ne: "SCHOOL_ADMIN" } },
 *     update: { $push: { roles: { ... } } })
 * </pre>
 *
 * <p>A matched count of zero then means somebody else added it first, which is success.
 *
 * <p><b>Editing one role means the positional operator, not a full save.</b> {@code save()} on
 * this document takes part in the {@code @Version} optimistic locking it inherits, so two admins
 * editing two different roles at the same time would collide. Target the entry instead:
 * {@code {schoolId, "roles.roleKey": K}} with {@code $set: {"roles.$.active": false}}.
 *
 * <h2>What this shape costs</h2>
 *
 * <p><b>Roles have no {@code _id} any more.</b> Nothing can hold a {@code roleDocsId}, which is
 * why {@code UserAccount.roleDocsIds} became {@code UserAccount.roleKeys} on the same day. That
 * is a fair trade rather than a loss: {@code roleKey} was already unique per school and reads
 * far better in a shell than an ObjectId.
 *
 * <p><b>The array is bounded, unlike NumberSequence's.</b> A school has three seeded roles and
 * however many it adds by hand — tens at the very most, never growing with time. Of the two
 * documents restructured that day, this is the one the shape genuinely suits.
 */
@Document(collection = "roles")
@CompoundIndexes({
        // One document per school, and this is what makes that true rather than a convention.
        // schoolId is @Indexed on SchoolBase but not uniquely, and an inherited field cannot be
        // re-annotated here, so the uniqueness is declared as an index on the class.
        @CompoundIndex(name = "school_roles_uniq", def = "{'schoolId': 1}", unique = true),

        // Answers "does this school have SCHOOL_ADMIN", which endpoint #3 asks before it will
        // activate a school. Multikey, because roles is an array. NOT unique: a unique multikey
        // index would not stop a duplicate entry inside one document anyway.
        @CompoundIndex(
                name = "school_role_key_idx",
                def = "{'schoolId': 1, 'roles.roleKey': 1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class Role extends SchoolBase {

    // Every role this school has. Seeded with SCHOOL_ADMIN, TEACHER and GUARDIAN by
    // complete-provisioning, and added to by the school.
    //
    // Identity inside the array is roleKey. NOTHING IN THE DATABASE ENFORCES THAT — a guarded
    // $push is the only thing standing between this array and two SCHOOL_ADMIN entries, of
    // which the positional operator would only ever find the first.
    //
    // schoolId, and everything else identifying the document, comes from SchoolBase. There is
    // no field of this class's own besides the array: the document IS the school's set of roles.
    @Valid
    @NotNull
    @Builder.Default
    private List<RoleDefinition> roles = new ArrayList<>();
}
