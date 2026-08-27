package com.orbitastra.backend.services.core.helper;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

import com.orbitastra.backend.models.identity.Role;
import com.orbitastra.backend.models.identity.embedded.RolePermission;
import com.orbitastra.backend.models.identity.enums.AppModule;
import com.orbitastra.backend.models.identity.enums.DataScope;
import com.orbitastra.backend.models.identity.enums.PermissionAction;

/**
 * The roles a brand new school starts with.
 *
 * <p>Pure data — it builds Role objects and saves nothing. The caller decides what to do with
 * them, which keeps the seeding decision (what to create) apart from the seeding action (write
 * only what is missing).
 *
 * <p><b>Three, not thirty.</b> A school that needs a Vice Principal, a Hostel Warden or an
 * Accountant makes one — that is why Role is a collection and not an enum. What a new tenant
 * needs is the smallest set that lets somebody log in and start configuring.
 *
 * <p>All three are {@code systemManaged}, which should stop a school renaming or deleting them.
 * Every account in the system eventually points at a role, and a school that removes
 * SCHOOL_ADMIN locks itself out permanently.
 *
 * <p>SCHOOL_ADMIN gets every module at SCHOOL scope. That is deliberate and unavoidable: it is
 * the only way the first person can configure anything, and narrowing it here produces a tenant
 * nobody can finish setting up.
 */
public final class DefaultRoles {

    private DefaultRoles() {
    }

    /** Role key -> the role, for one school. Keys are what the seeder checks for. */
    public static List<Role> forSchool(String schoolId) {
        List<Role> roles = new ArrayList<>();

        roles.add(Role.builder()
                .schoolId(schoolId)
                .roleKey("SCHOOL_ADMIN")
                .name("School Administrator")
                .description("Full access to every module. The role the first account holds.")
                .permissions(everyModule(DataScope.SCHOOL,
                        PermissionAction.VIEW, PermissionAction.CREATE, PermissionAction.EDIT,
                        PermissionAction.DELETE, PermissionAction.APPROVE, PermissionAction.EXPORT))
                .systemManaged(true)
                .active(true)
                .build());

        roles.add(Role.builder()
                .schoolId(schoolId)
                .roleKey("TEACHER")
                .name("Teacher")
                .description("Own classes: attendance, homework, marks. No fees, no staff pay.")
                .permissions(List.of(
                        permission(AppModule.ATTENDANCE, DataScope.ASSIGNED,
                                PermissionAction.VIEW, PermissionAction.CREATE, PermissionAction.EDIT),
                        permission(AppModule.HOMEWORK, DataScope.ASSIGNED,
                                PermissionAction.VIEW, PermissionAction.CREATE, PermissionAction.EDIT),
                        permission(AppModule.EXAMINATIONS, DataScope.ASSIGNED,
                                PermissionAction.VIEW, PermissionAction.CREATE, PermissionAction.EDIT),
                        permission(AppModule.TIMETABLE, DataScope.ASSIGNED, PermissionAction.VIEW),
                        permission(AppModule.STUDENTS, DataScope.ASSIGNED, PermissionAction.VIEW)))
                .systemManaged(true)
                .active(true)
                .build());

        roles.add(Role.builder()
                .schoolId(schoolId)
                .roleKey("GUARDIAN")
                .name("Parent or Guardian")
                .description("Their own child only. Deliberately read-mostly.")
                .permissions(List.of(
                        permission(AppModule.STUDENTS, DataScope.OWN, PermissionAction.VIEW),
                        permission(AppModule.ATTENDANCE, DataScope.OWN, PermissionAction.VIEW),
                        permission(AppModule.HOMEWORK, DataScope.OWN, PermissionAction.VIEW),
                        permission(AppModule.EXAMINATIONS, DataScope.OWN, PermissionAction.VIEW),
                        permission(AppModule.FEES_BILLING, DataScope.OWN, PermissionAction.VIEW),
                        // CREATE so a parent can start a payment, never EDIT an invoice.
                        permission(AppModule.FEES_PAYMENTS, DataScope.OWN,
                                PermissionAction.VIEW, PermissionAction.CREATE)))
                .systemManaged(true)
                .active(true)
                .build());

        return roles;
    }

    private static List<RolePermission> everyModule(DataScope scope, PermissionAction... actions) {
        List<RolePermission> permissions = new ArrayList<>();
        for (AppModule module : AppModule.values()) {
            permissions.add(permission(module, scope, actions));
        }
        return permissions;
    }

    private static RolePermission permission(AppModule module, DataScope scope,
            PermissionAction... actions) {
        return RolePermission.builder()
                .module(module)
                .actions(new LinkedHashSet<>(List.of(actions)))
                .scope(scope)
                .build();
    }
}
