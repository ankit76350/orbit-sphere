package com.orbitastra.backend.services.student;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;

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
        guardian.setCreatedAt(LocalDateTime.now());
        guardian.setUpdatedAt(LocalDateTime.now());
        return guardianRepository.save(guardian);
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
