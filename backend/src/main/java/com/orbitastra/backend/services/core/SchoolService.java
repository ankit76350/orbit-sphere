package com.orbitastra.backend.services.core;


import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.orbitastra.backend.common.exception.ConflictException;
import com.orbitastra.backend.dto.core.SchoolCreateRequest;
import com.orbitastra.backend.dto.core.SchoolCreateResponse;
import com.orbitastra.backend.models.core.School;
import com.orbitastra.backend.repositories.core.SchoolRepository;
import com.orbitastra.backend.services.core.helper.TextHelper;
import com.orbitastra.backend.services.core.helper.TimeZoneHelper;
import com.orbitastra.backend.services.core.helper.SubdomainPolicy;

import lombok.RequiredArgsConstructor;

/**
 * Creates a new tenant.
 *
 * <p><b>Right now this writes the School row and nothing else.</b> It originally also seeded a
 * NumberSequence for every type and a starting set of Roles; both were removed on 2026-08-21 to
 * be decided separately.
 *
 * <p>That leaves a real gap to close before the tenant is usable, and it is worth knowing which
 * way it fails. Almost every business document takes its human-readable number from a
 * NumberSequence row — an admission number, an invoice number, a receipt. With no row for a
 * type, the first attempt to admit a student has no counter to increment. And a UserAccount
 * points at Role ids, so with no roles there is nothing to attach the first administrator to.
 *
 * <p>Neither failure appears here. Both appear later, to somebody trying to use the school —
 * which is exactly what PROVISIONING as a starting status is for. Whatever creates the first
 * administrator will have to deal with both, or seeding comes back.
 *
 * <p>{@code @Transactional} is kept even though a single document write is already atomic in
 * MongoDB. When seeding returns, the boundary needs to be here and not remembered later.
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
public class SchoolService {

    private final SchoolRepository schools;
    private final SubdomainPolicy subdomainPolicy;
    private final TimeZoneHelper timeZoneHelper;

    @Transactional
    public SchoolCreateResponse createNewSchool(SchoolCreateRequest request) {
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

        //! step 1 - build the school info document
        School school = School.builder()
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
                .build();

        //! step 2 - save it with the new school 
        School savedSchool = schools.save(school);

        return SchoolCreateResponse.of(savedSchool);
    }
}
