package com.orbitastra.backend.services.core;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.orbitastra.backend.common.exception.ConflictException;
import com.orbitastra.backend.dto.core.ProvisionSchoolRequest;
import com.orbitastra.backend.dto.core.ProvisionSchoolResponse;
import com.orbitastra.backend.models.core.School;
import com.orbitastra.backend.models.identity.Role;
import com.orbitastra.backend.models.identity.embedded.RolePermission;
import com.orbitastra.backend.models.identity.enums.AppModule;
import com.orbitastra.backend.models.identity.enums.DataScope;
import com.orbitastra.backend.models.identity.enums.PermissionAction;
import com.orbitastra.backend.models.institution.NumberSequence;
import com.orbitastra.backend.models.institution.enums.NumberSequenceType;
import com.orbitastra.backend.models.institution.enums.SequenceResetPolicy;
import com.orbitastra.backend.repositories.core.SchoolRepository;
import com.orbitastra.backend.repositories.identity.RoleRepository;
import com.orbitastra.backend.repositories.institution.NumberSequenceRepository;
import com.orbitastra.backend.services.core.helper.TextHelper;
import com.orbitastra.backend.services.core.helper.TimeZoneHelper;
import com.orbitastra.backend.services.core.helper.SubdomainPolicy;

import lombok.RequiredArgsConstructor;

/**
 * Creates a new tenant.
 *
 * <p>Three things in one transaction: the School row, a NumberSequence for every type the
 * platform knows about, and a starting set of Roles. If any part fails, none of it is written.
 *
 * <p><b>Why no staff record and no user account, when the controller README said there would
 * be.</b> The plan had provisioning create the account holder's Staff row and first UserAccount
 * too, so a tenant arrived ready to log into. Writing it showed that cannot work honestly:
 * {@code Staff} requires {@code dateOfBirth} and {@code gender}, both non-null. A platform
 * operator provisioning a school for a client does not know the principal's date of birth, and
 * inventing one puts a false date into a staff record that payroll and government reporting will
 * later treat as fact. There is no safe placeholder for a real person's birthday.
 *
 * <p>The account holder on the contract and the school's first administrator are also not
 * necessarily the same person — a trustee may sign while an IT contractor does the setup. So
 * {@code School.accountHolderName} stays a plain name, and creating the first administrator is
 * its own endpoint with its own request that asks for what Staff actually requires.
 *
 * <p>That keeps this method's promise intact rather than weakening it: **School plus sequences
 * plus roles is a coherent unit** — a skeleton that can accept its first administrator — and
 * PROVISIONING is precisely the state for "exists, not usable yet".
 *
 * <p>All three writes are needed together. Without the sequences nothing else in the system can
 * ever be created, because almost every business document takes its number from one. Without the
 * roles there is nothing to attach the first account to. A School row on its own is not a tenant.
 */
@Service
@RequiredArgsConstructor
public class SchoolProvisioningService {

    private final SchoolRepository schools;
    private final NumberSequenceRepository numberSequences;
    private final RoleRepository roles;
    private final SubdomainPolicy subdomainPolicy;
    private final TimeZoneHelper timeZoneHelper;

    @Transactional
    public ProvisionSchoolResponse createNewSchool(ProvisionSchoolRequest request) {
        //! validating subdomain
        String subdomain = subdomainPolicy.validateSubdomain(request.subdomain());
        String timeZone = timeZoneHelper.validateAndNormalize(request.defaultTimeZone());
        String countryCode = TextHelper.uppercaseOrNull(request.countryCode());

        // Checked before writing so the caller gets a clear message. The unique index is still
        // the real guard: two simultaneous requests both pass this, and the loser surfaces as a
        // DuplicateKeyException, which GlobalExceptionHandler turns into the same 409.
        if (schools.existsBySubdomain(subdomain)) {
            throw new ConflictException("SUBDOMAIN_TAKEN",
                    "The subdomain '" + subdomain + "' is already in use.");
        }

        School school = schools.save(School.builder()
                .schoolName(request.schoolName().trim())
                .accountHolderName(request.accountHolderName().trim())
                .subdomain(subdomain)
                .phoneNumber(TextHelper.blankToNull(request.phoneNumber()))
                .emailAddress(TextHelper.lowercaseOrNull(request.emailAddress()))
                .defaultLocale(request.defaultLocale().trim())
                .defaultTimeZone(timeZone)
                .addressLine(TextHelper.blankToNull(request.addressLine()))
                .city(TextHelper.blankToNull(request.city()))
                .stateOrProvince(TextHelper.blankToNull(request.stateOrProvince()))
                .postalCode(TextHelper.blankToNull(request.postalCode()))
                .countryCode(countryCode)
                .status(request.initialStatus())
                .build());

        int sequenceCount = seedNumberSequences(school.getId());
        int roleCount = seedRoles(school.getId());

        return ProvisionSchoolResponse.of(school, sequenceCount, roleCount);
    }

    /**
     * One sequence per type, at scope GLOBAL.
     *
     * <p>Every type is seeded, not just the ones a new school will use this week. The
     * alternative is creating them on first use, which means the first invoice of the year races
     * every other request that also wants an invoice number, and the code that allocates a number
     * has to handle "the sequence does not exist yet" forever.
     *
     * <p>{@code scopeKey} is GLOBAL here. Types that should restart each year — an admission
     * number, an invoice number — are re-scoped to the academic year name when that year is
     * created; this is the fallback so nothing is ever missing.
     */
    private int seedNumberSequences(String schoolId) {
        List<NumberSequence> seeds = new ArrayList<>();
        for (NumberSequenceType type : NumberSequenceType.values()) {
            seeds.add(NumberSequence.builder()
                    .schoolId(schoolId)
                    .sequenceType(type)
                    .scopeKey("GLOBAL")
                    .nextValue(1L)
                    .paddingWidth(6)
                    .resetPolicy(SequenceResetPolicy.NEVER)
                    .build());
        }
        return numberSequences.saveAll(seeds).size();
    }

    /**
     * A starting set of roles, all {@code systemManaged}.
     *
     * <p>Three, not thirty. A school that needs a Vice Principal or a Hostel Warden role makes
     * one — that is why Role is a collection rather than an enum. What a new tenant needs is
     * enough to get an administrator in and doing work.
     *
     * <p>SCHOOL_ADMIN gets every module. It is the only way the first person can configure
     * anything, and narrowing it here would mean a tenant nobody can finish setting up.
     */
    private int seedRoles(String schoolId) {
        List<Role> seeds = new ArrayList<>();

        seeds.add(Role.builder()
                .schoolId(schoolId)
                .roleKey("SCHOOL_ADMIN")
                .name("School Administrator")
                .description("Full access to every module. The role the first account holds.")
                .permissions(allModules(DataScope.SCHOOL,
                        PermissionAction.VIEW, PermissionAction.CREATE, PermissionAction.EDIT,
                        PermissionAction.DELETE, PermissionAction.APPROVE, PermissionAction.EXPORT))
                .systemManaged(true)
                .active(true)
                .build());

        seeds.add(Role.builder()
                .schoolId(schoolId)
                .roleKey("TEACHER")
                .name("Teacher")
                .description("Own classes: attendance, homework, marks. No fees, no staff data.")
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

        seeds.add(Role.builder()
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
                        permission(AppModule.FEES_PAYMENTS, DataScope.OWN,
                                PermissionAction.VIEW, PermissionAction.CREATE)))
                .systemManaged(true)
                .active(true)
                .build());

        return roles.saveAll(seeds).size();
    }

    private List<RolePermission> allModules(DataScope scope, PermissionAction... actions) {
        List<RolePermission> permissions = new ArrayList<>();
        for (AppModule module : AppModule.values()) {
            permissions.add(permission(module, scope, actions));
        }
        return permissions;
    }

    private RolePermission permission(AppModule module, DataScope scope,
            PermissionAction... actions) {
        return RolePermission.builder()
                .module(module)
                .actions(new LinkedHashSet<>(List.of(actions)))
                .scope(scope)
                .build();
    }
}
