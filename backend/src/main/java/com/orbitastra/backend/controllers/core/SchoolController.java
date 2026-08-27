package com.orbitastra.backend.controllers.core;

import java.net.URI;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.orbitastra.backend.dto.core.CompleteProvisioningResponse;
import com.orbitastra.backend.dto.core.SchoolActivateResponse;
import com.orbitastra.backend.dto.core.SchoolCreateRequest;
import com.orbitastra.backend.dto.core.SchoolCreateResponse;
import com.orbitastra.backend.services.core.SchoolService;

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
public class SchoolController {

    private final SchoolService provisioningService;

    // Ceate New school
    @PostMapping
    public ResponseEntity<SchoolCreateResponse> provision(
            @Valid @RequestBody SchoolCreateRequest request) {

        SchoolCreateResponse response = provisioningService.createNewSchool(request);
        return ResponseEntity
                .created(URI.create("/platform/schools/" + response.schoolId()))
                .body(response);
    }

    /**
     * Endpoint #2 — Completes school setup.
     *
     * <p>Creates missing sequences and roles.
     */
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
}
