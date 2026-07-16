package com.orbitastra.backend.services.student;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.orbitastra.backend.exceptions.DuplicateGuardianException;
import com.orbitastra.backend.exceptions.ResourceNotFoundException;
import com.orbitastra.backend.models.student.Guardian;
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
