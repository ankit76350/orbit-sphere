package com.orbitastra.backend.controllers.core;

import java.net.URI;

import java.time.Instant;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import com.orbitastra.backend.common.web.PageResponse;
import com.orbitastra.backend.dto.core.platform.SchoolSearchRequest;
import com.orbitastra.backend.dto.core.platform.SchoolSummaryResponse;
import com.orbitastra.backend.models.core.enums.SchoolStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.orbitastra.backend.dto.core.platform.CompleteProvisioningResponse;
import com.orbitastra.backend.dto.core.platform.SchoolActivateResponse;
import com.orbitastra.backend.dto.core.platform.SchoolCreateRequest;
import com.orbitastra.backend.dto.core.platform.SchoolCreateResponse;
import com.orbitastra.backend.dto.core.platform.SchoolReactivateRequest;
import com.orbitastra.backend.dto.core.platform.SchoolStatusResponse;
import com.orbitastra.backend.dto.core.platform.SchoolSubdomainRequest;
import com.orbitastra.backend.dto.core.platform.SchoolSubdomainResponse;
import com.orbitastra.backend.dto.core.platform.SchoolSuspendRequest;
import com.orbitastra.backend.services.core.SchoolPlatformService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * The platform surface for schools. Endpoint #1 of the plan in this package's README.
 *
 * <p><b>This controller is not a school-facing API and must never become one.</b> Its base path
 * is {@code /platform/schools} rather than {@code /schools}, and the split is structural rather
 * than cosmetic: when provisioning runs there is no user, staff record, role or session
 * belonging to that school, because they are created by or after this request. The caller is
 * necessarily outside the tenant, so this cannot sit behind the same authentication as
 * everything else in the system.
 *
 * <p>School-facing edits — the school's own address, logo and contact details — belong on a
 * separate controller under {@code /schools/current}, where the tenant comes from the session
 * rather than a path parameter.
 *
 * <p><b>There is no authentication on this endpoint yet.</b> It is the first thing built and
 * nothing exists to authenticate against. Before this is reachable from anywhere but a developer
 * machine it needs platform credentials in front of it: an endpoint that provisions tenants and
 * seeds their roles is the most valuable unauthenticated endpoint an attacker could ask for.
 *
 * <p>There is deliberately no {@code PUT /platform/schools/{id}}. A full replace of a document
 * holding {@code status}, {@code activatedAt} and {@code encryptionKeyReference} would hand a
 * caller every field the model defends, and a client omitting a field it did not know about
 * would blank it.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/platform/schools")
public class SchoolPlatformController {

    private final SchoolPlatformService provisioningService;

    // Ceate New school
    @PostMapping
    public ResponseEntity<SchoolCreateResponse> provision(
            @Valid @RequestBody SchoolCreateRequest request) {

        SchoolCreateResponse response = provisioningService.createNewSchool(request);
        return ResponseEntity
                .created(URI.create("/platform/schools/" + response.schoolId()))
                .body(response);
    }

    // Completes school setup by creating missing sequences and roles.
    @PostMapping("/{id}/complete-provisioning")
    public ResponseEntity<CompleteProvisioningResponse> completeProvisioning(
            @PathVariable String id) {

        return ResponseEntity.ok(provisioningService.completeProvisioning(id));
    }

    /**
     * Endpoint #3 — Takes the school live.
     *
     * <p>PROVISIONING or TRIAL to ACTIVE. Refuses anything else.
     *
     * <p>Requires complete-provisioning to have run: a school with no SCHOOL_ADMIN role or
     * missing number sequences fails on first use, so it is refused rather than activated.
     */
    @PostMapping("/{id}/activate")
    public ResponseEntity<SchoolActivateResponse> activate(@PathVariable String id) {
        return ResponseEntity.ok(provisioningService.activateSchool(id));
    }

    // Suspends the school and stores the suspension reason.
    @PostMapping("/{id}/suspend")
    public ResponseEntity<SchoolStatusResponse> suspend(
            @PathVariable String id,
            @Valid @RequestBody SchoolSuspendRequest request) {

        return ResponseEntity.ok(provisioningService.suspendSchool(id, request.reason()));
    }

    // Reactivates a suspended school and moves it back to ACTIVE status.
    @PostMapping("/{id}/reactivate")
    public ResponseEntity<SchoolStatusResponse> reactivate(
            @PathVariable String id,
            @RequestBody(required = false) SchoolReactivateRequest request) {

        String note = request == null ? null : request.note();
        return ResponseEntity.ok(provisioningService.reactivateSchool(id, note));
    }

    /**
     * Endpoint #10 — changes the subdomain a school answers to.
     *
     * <p>A {@code PATCH} of one field, but not a profile edit: this is the key that resolves
     * every request to the tenant. It is on the platform surface for that reason, and #6 has no
     * field for it.
     *
     * <p>The body must confirm the <b>current</b> subdomain. Getting the school wrong here takes
     * a tenant off the air, and an id in a URL is easy to paste wrong.
     *
     * <p>The old label is released immediately and any school may claim it.
     */
    @PatchMapping("/{id}/subdomain")
    public ResponseEntity<SchoolSubdomainResponse> changeSubdomain(
            @PathVariable String id,
            @Valid @RequestBody SchoolSubdomainRequest request) {

        return ResponseEntity.ok(provisioningService.changeSubdomain(id, request));
    }

    /**
     * G1 API — gets a list of schools for the operator.
     *
     * All parameters are optional. If you call GET /platform/schools without
     * any parameters, it returns the latest 20 schools.
     *
     * You can filter by status, search by school name, country, city,
     * creation date, etc.
     *
     * Example:
     * ?status=ACTIVE&status=TRIAL&search=orbit&page=0&size=20&sort=name,asc
     *
     * All filters work together using AND.
     * However, status can have multiple values, meaning:
     * status can be ACTIVE OR TRIAL.
     *
     * Filtering, searching, sorting, and pagination are done in the database.
     *
     * The API returns a PageResponse containing:
     * - the schools
     * - total number of records
     * - current page
     * - page size
     * - information needed to get the next page
     *
     * This API only reads data, so there is no need for @Transactional.
     */
    @GetMapping
    public ResponseEntity<PageResponse<SchoolSummaryResponse>> list(
            @RequestParam(required = false) List<SchoolStatus> status,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String countryCode,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) @DateTimeFormat(iso = ISO.DATE_TIME) Instant createdFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = ISO.DATE_TIME) Instant createdTo,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sort) {

        // Bound one at a time rather than through @ModelAttribute, so a misspelled status comes
        // back through the type-mismatch handler naming the accepted values, instead of a bind
        // error nothing in this package formats.
        SchoolSearchRequest request = new SchoolSearchRequest(
                status, search, countryCode, city, createdFrom, createdTo, page, size, sort);

        return ResponseEntity.ok(provisioningService.listSchools(request));
    }
}
