package com.orbitastra.backend.services.student;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.orbitastra.backend.exceptions.DuplicateGuardianException;
import com.orbitastra.backend.exceptions.ResourceNotFoundException;
import com.orbitastra.backend.models.student.Guardian;
import com.orbitastra.backend.models.student.GuardianLink;
import com.orbitastra.backend.models.student.enums.GuardianRelation;
import com.orbitastra.backend.repositories.student.GuardianRepository;

import lombok.RequiredArgsConstructor;

/**
 * CRUD for guardian (contact) people. A guardian is a standalone person; the
 * link to students (role + flags) is managed on the student side via
 * {@link StudentService#addGuardianLink}. One guardian may be linked to many
 * students (siblings), so guardians are never auto-deleted when unlinked.
 */
@Service
@RequiredArgsConstructor
public class GuardianService {

    private final GuardianRepository guardianRepository;

    public Guardian createGuardian(Guardian guardian) {
        if (guardian.getSchoolId() == null || guardian.getSchoolId().isBlank()) {
            throw new IllegalArgumentException("School ID is required for a guardian.");
        }
        if (guardian.getName() == null || guardian.getName().isBlank()) {
            throw new IllegalArgumentException("Guardian name is required.");
        }
        normalize(guardian);

        // Reject a duplicate so the caller can offer to link the existing guardian instead.
        findExistingMatch(guardian.getSchoolId(), guardian.getName(), guardian.getPhone(), guardian.getEmail())
                .ifPresent(existing -> {
                    throw new DuplicateGuardianException(
                            "A guardian named '" + existing.getName() + "' with the same phone/email already exists.",
                            existing.getId(), existing.getName());
                });

        guardian.setCreatedAt(LocalDateTime.now());
        guardian.setUpdatedAt(LocalDateTime.now());
        return guardianRepository.save(guardian);
    }

    /**
     * Returns an existing guardian for this school that is the same person as the
     * given details — matched by name + phone OR name + email (case-insensitive on
     * name/email, exact on phone). A name-only candidate (no phone and no email)
     * can't be de-duplicated reliably, so it never matches.
     */
    public Optional<Guardian> findExistingMatch(String schoolId, String name, String phone, String email) {
        if (name == null || name.isBlank()) return Optional.empty();
        String n = name.trim();
        String p = phone == null ? null : phone.trim();
        String e = email == null ? null : email.trim();
        boolean hasPhone = p != null && !p.isEmpty();
        boolean hasEmail = e != null && !e.isEmpty();
        if (!hasPhone && !hasEmail) return Optional.empty();

        return guardianRepository.findBySchoolIdAndName(schoolId, n).stream()
                .filter(g -> (hasPhone && p.equalsIgnoreCase(g.getPhone()))
                        || (hasEmail && e.equalsIgnoreCase(g.getEmail())))
                .findFirst();
    }

    /**
     * Find-or-reuse: returns the matching existing guardian, or creates a new one.
     * Used when materialising prospective guardians (e.g. admission → student) so
     * the same person is never stored twice.
     */
    public Guardian findOrCreate(String schoolId, String name, String phone, String email,
                                 String address, String occupation) {
        return findExistingMatch(schoolId, name, phone, email).orElseGet(() -> {
            Guardian g = Guardian.builder()
                    .schoolId(schoolId)
                    .name(name == null ? null : name.trim())
                    .phone(phone == null ? null : phone.trim())
                    .email(email == null ? null : email.trim())
                    .address(address)
                    .occupation(occupation)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();
            return guardianRepository.save(g);
        });
    }

    /**
     * A prospective guardian to be linked to a student, normalised so both the
     * direct-create path ({@code StudentGuardianRequest}) and the admission-convert
     * path ({@code InquiryGuardian}) can share one link-building routine. Either an
     * existing {@code guardianId} is supplied, or the person details are used to
     * find-or-create a {@link Guardian}. Flags are {@link Boolean} so "not provided"
     * (null) is distinct from an explicit {@code false}.
     */
    public record GuardianDraft(
            String guardianId,
            String name, String phone, String email, String address, String occupation,
            GuardianRelation relation,
            Boolean primary, Boolean emergencyContact, Boolean pickupApproved, Boolean portalAccess) {

        /** Person-only draft with no explicit link flags (e.g. from an admission snapshot). */
        public static GuardianDraft ofPerson(String name, String phone, String email, String address,
                                             String occupation, GuardianRelation relation) {
            return new GuardianDraft(null, name, phone, email, address, occupation, relation,
                    null, null, null, null);
        }
    }

    /**
     * Resolves a list of prospective guardians into de-duplicated {@link GuardianLink}s,
     * appended to any {@code existingLinks} already on the student. For each draft the
     * guardian is resolved either by an explicit {@code guardianId} — which must reference
     * an existing guardian in the SAME school — or by find-or-create from person details;
     * the link is skipped when that guardian is already linked, and — unless a {@code primary}
     * flag is given — the first link with no other primary yet is defaulted to primary. Any
     * {@code existingLinks} are validated the same way (must exist in this school). This is the
     * single de-duplication + validation point shared by student creation and admission conversion.
     *
     * @throws com.orbitastra.backend.exceptions.ResourceNotFoundException if a supplied
     *         {@code guardianId} does not exist
     * @throws IllegalArgumentException if a supplied {@code guardianId} belongs to another school
     */
    public List<GuardianLink> buildDedupedLinks(String schoolId, List<GuardianLink> existingLinks,
                                                List<GuardianDraft> drafts) {
        List<GuardianLink> links = new ArrayList<>();
        // Links already on the payload reference existing guardians by id — validate them too.
        if (existingLinks != null) {
            for (GuardianLink l : existingLinks) {
                if (l == null || l.getGuardianId() == null || l.getGuardianId().isBlank()) continue;
                assertGuardianInSchool(l.getGuardianId(), schoolId);
                if (links.stream().noneMatch(x -> l.getGuardianId().equals(x.getGuardianId()))) {
                    links.add(l);
                }
            }
        }
        if (drafts == null) return links;

        for (GuardianDraft d : drafts) {
            String guardianId = d.guardianId();
            if (guardianId != null && !guardianId.isBlank()) {
                // Explicit reference to an existing guardian — it must exist in this school.
                assertGuardianInSchool(guardianId, schoolId);
            } else if (d.name() != null && !d.name().isBlank()) {
                // New/known person — find-or-create always yields a guardian in this school.
                guardianId = findOrCreate(schoolId, d.name(), d.phone(), d.email(),
                        d.address(), d.occupation()).getId();
            } else {
                continue; // neither an id nor a name to resolve — nothing to link
            }

            final String gId = guardianId;
            boolean alreadyLinked = links.stream().anyMatch(l -> gId.equals(l.getGuardianId()));
            if (alreadyLinked) continue;

            boolean hasPrimary = links.stream().anyMatch(GuardianLink::isPrimary);
            links.add(GuardianLink.builder()
                    .guardianId(gId)
                    .relation(d.relation())
                    .primary(d.primary() != null ? d.primary() : !hasPrimary)
                    .emergencyContact(Boolean.TRUE.equals(d.emergencyContact()))
                    .pickupApproved(Boolean.TRUE.equals(d.pickupApproved()))
                    .portalAccess(Boolean.TRUE.equals(d.portalAccess()))
                    .build());
        }
        return links;
    }

    /**
     * Asserts a guardian with this id exists and belongs to {@code schoolId}. Used before
     * linking an existing guardian to a student so a link can never cross school boundaries
     * or dangle at a non-existent guardian.
     */
    private void assertGuardianInSchool(String guardianId, String schoolId) {
        Guardian guardian = guardianRepository.findById(guardianId)
                .orElseThrow(() -> new ResourceNotFoundException("Guardian not found with id: " + guardianId));
        if (guardian.getSchoolId() == null || !guardian.getSchoolId().equals(schoolId)) {
            throw new IllegalArgumentException(
                    "Guardian '" + guardianId + "' does not belong to the same school as the student.");
        }
    }

    private void normalize(Guardian g) {
        if (g.getName() != null) g.setName(g.getName().trim());
        if (g.getPhone() != null) g.setPhone(g.getPhone().trim());
        if (g.getEmail() != null) g.setEmail(g.getEmail().trim());
    }

    public Guardian getGuardianById(String id) {
        return guardianRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Guardian not found with id: " + id));
    }

    public List<Guardian> getGuardiansBySchool(String schoolId) {
        return guardianRepository.findBySchoolId(schoolId);
    }

    public Guardian updateGuardian(String id, Guardian details) {
        Guardian guardian = getGuardianById(id);
        if (details.getName() != null) guardian.setName(details.getName());
        if (details.getPhone() != null) guardian.setPhone(details.getPhone());
        if (details.getAlternatePhone() != null) guardian.setAlternatePhone(details.getAlternatePhone());
        if (details.getEmail() != null) guardian.setEmail(details.getEmail());
        if (details.getAddress() != null) guardian.setAddress(details.getAddress());
        if (details.getOccupation() != null) guardian.setOccupation(details.getOccupation());
        guardian.setUpdatedAt(LocalDateTime.now());
        return guardianRepository.save(guardian);
    }

    public void deleteGuardian(String id) {
        Guardian guardian = getGuardianById(id);
        guardianRepository.delete(guardian);
    }
}
