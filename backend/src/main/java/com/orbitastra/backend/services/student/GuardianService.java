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
import lombok.extern.slf4j.Slf4j;

/**
 * Handles guardians (a student's parents / contacts).
 *
 * A guardian is a person saved on their own. The same guardian can belong to more than one
 * student (for example, two siblings share the same mother). This class saves guardians, links
 * them to students, and makes sure we never save the same person twice in the same school.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GuardianService {

    private final GuardianRepository guardianRepository;

    // ---------------------------------------------------------------------------------------
    // Turning the guardians from a request into links on a student
    // ---------------------------------------------------------------------------------------

    /**
     * One guardian coming in from a request, before we link it to a student.
     *
     * It either gives us an existing guardian's id, OR the person's details (name, phone, etc.)
     * that we use to find or create the guardian. The flags are Boolean (not boolean) so we can
     * tell "not sent" (null) apart from "sent as false".
     */
    public record GuardianDraft(
            String guardianDocsId,
            String name, String phone, String email, String address, String occupation,
            GuardianRelation relation,
            Boolean primary, Boolean emergencyContact, Boolean pickupApproved, Boolean portalAccess) {

        /** Makes a draft from just a person's details, with no flags set (e.g. from an admission). */
        public static GuardianDraft ofPerson(String name, String phone, String email, String address,
                                             String occupation, GuardianRelation relation) {
            return new GuardianDraft(null, name, phone, email, address, occupation, relation,
                    null, null, null, null);
        }
    }

    /**
     * Takes the guardians from the request and returns the list of links to save on the student.
     *
     * For each guardian we either use the id given (that guardian must already exist in the same
     * school), or we find/create the guardian from their details. If the same guardian shows up
     * twice we only keep one link. If nobody was marked as the main (primary) guardian, the first
     * one becomes the main guardian.
     *
     * @throws ResourceNotFoundException if a given guardian id does not exist
     * @throws IllegalArgumentException  if a given guardian id belongs to a different school
     */
    public List<GuardianLink> buildDedupedLinks(String schoolId, List<GuardianLink> existingLinks,
                                                List<GuardianDraft> drafts) {
        int draftCount = drafts == null ? 0 : drafts.size();
        log.info("[buildDedupedLinks] Preparing guardians for school {}: {} already on the student, {} coming in the request",
                schoolId, existingLinks == null ? 0 : existingLinks.size(), draftCount);

        List<GuardianLink> links = new ArrayList<>();

        // Guardians already on the student also point to real guardians, so check them too.
        if (existingLinks != null) {
            for (GuardianLink l : existingLinks) {
                if (l == null || l.getGuardianDocsId() == null || l.getGuardianDocsId().isBlank()) continue;
                assertGuardianInSchool(l.getGuardianDocsId(), schoolId);
                if (links.stream().noneMatch(x -> l.getGuardianDocsId().equals(x.getGuardianDocsId()))) {
                    links.add(l);
                    log.debug("[buildDedupedLinks] Kept the guardian already on the student: {}", l.getGuardianDocsId());
                }
            }
        }
        if (drafts == null) {
            log.info("[buildDedupedLinks] No guardians in the request. Total guardians: {}", links.size());
            return links;
        }

        int idx = 0;
        for (GuardianDraft d : drafts) {
            idx++;
            String guardianDocsId = d.guardianDocsId();
            if (guardianDocsId != null && !guardianDocsId.isBlank()) {
                // The request gave an id of a guardian that should already exist.
                log.info("[buildDedupedLinks] Guardian {} of {}: using an existing guardian with id {}", idx, draftCount, guardianDocsId);
                assertGuardianInSchool(guardianDocsId, schoolId);
            } else if (d.name() != null && !d.name().isBlank()) {
                // The request gave a person's details. Find that person, or create them if new.
                log.info("[buildDedupedLinks] Guardian {} of {}: looking up person by name '{}', phone '{}', email '{}'",
                        idx, draftCount, d.name(), d.phone(), d.email());
                guardianDocsId = findOrCreate(schoolId, d.name(), d.phone(), d.email(),
                        d.address(), d.occupation()).getId();
            } else {
                log.warn("[buildDedupedLinks] Guardian {} of {}: skipped because it had no id and no name", idx, draftCount);
                continue;
            }

            final String gId = guardianDocsId;
            if (links.stream().anyMatch(l -> gId.equals(l.getGuardianDocsId()))) {
                log.info("[buildDedupedLinks] Guardian {} of {}: guardian {} is already added, so skipping the duplicate", idx, draftCount, gId);
                continue;
            }

            // If no guardian is the main one yet, make this one the main (primary) guardian.
            boolean alreadyHasMainGuardian = links.stream().anyMatch(GuardianLink::isPrimary);
            boolean isMainGuardian = d.primary() != null ? d.primary() : !alreadyHasMainGuardian;
            links.add(GuardianLink.builder()
                    .guardianDocsId(gId)
                    .relation(d.relation())
                    .primary(isMainGuardian)
                    .emergencyContact(Boolean.TRUE.equals(d.emergencyContact()))
                    .pickupApproved(Boolean.TRUE.equals(d.pickupApproved()))
                    .portalAccess(Boolean.TRUE.equals(d.portalAccess()))
                    .build());
            log.info("[buildDedupedLinks] Guardian {} of {}: added guardian {} as {} (main guardian: {})",
                    idx, draftCount, gId, d.relation(), isMainGuardian);
        }

        log.info("[buildDedupedLinks] Finished. The student will have {} guardian(s)", links.size());
        return links;
    }

    /**
     * Looks for a guardian in this school who is the same person as the details given.
     *
     * We treat two people as the same when the name matches AND either the phone or the email
     * matches (name and email are checked ignoring upper/lower case; phone must match exactly).
     * If only a name is given (no phone and no email) we can't be sure it's the same person, so
     * we return nothing.
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
     * Returns the matching guardian if that person already exists, otherwise creates a new one.
     * This is how we avoid saving the same guardian twice.
     */
    public Guardian findOrCreate(String schoolId, String name, String phone, String email,
                                 String address, String occupation) {
        Optional<Guardian> existing = findExistingMatch(schoolId, name, phone, email);
        if (existing.isPresent()) {
            log.info("[findOrCreate] '{}' already exists, so using the existing guardian (id {})", name, existing.get().getId());
            return existing.get();
        }
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
        Guardian saved = guardianRepository.save(g);
        log.info("[findOrCreate] '{}' is new, so created a new guardian (id {})", name, saved.getId());
        return saved;
    }

    /**
     * Makes sure the guardian with this id exists and belongs to this school, so a student is
     * never linked to a guardian that is missing or from another school.
     */
    private void assertGuardianInSchool(String guardianDocsId, String schoolId) {
        Guardian guardian = guardianRepository.findById(guardianDocsId)
                .orElseThrow(() -> {
                    log.warn("[assertGuardianInSchool] No guardian found with id {}", guardianDocsId);
                    return new ResourceNotFoundException("Guardian not found with id: " + guardianDocsId);
                });
        if (guardian.getSchoolId() == null || !guardian.getSchoolId().equals(schoolId)) {
            log.warn("[assertGuardianInSchool] Guardian {} is in school {}, not school {}", guardianDocsId, guardian.getSchoolId(), schoolId);
            throw new IllegalArgumentException(
                    "Guardian '" + guardianDocsId + "' does not belong to the same school as the student.");
        }
    }

    // ---------------------------------------------------------------------------------------
    // Add / read / change / remove a guardian on its own
    // ---------------------------------------------------------------------------------------

    public Guardian createGuardian(Guardian guardian) {
        if (guardian.getSchoolId() == null || guardian.getSchoolId().isBlank()) {
            throw new IllegalArgumentException("School ID is required for a guardian.");
        }
        if (guardian.getName() == null || guardian.getName().isBlank()) {
            throw new IllegalArgumentException("Guardian name is required.");
        }
        normalize(guardian);

        // If this person already exists, stop and tell the caller (they can link to that one instead).
        findExistingMatch(guardian.getSchoolId(), guardian.getName(), guardian.getPhone(), guardian.getEmail())
                .ifPresent(existing -> {
                    throw new DuplicateGuardianException(
                            "A guardian named '" + existing.getName() + "' with the same phone/email already exists.",
                            existing.getId(), existing.getName());
                });

        guardian.setCreatedAt(LocalDateTime.now());
        guardian.setUpdatedAt(LocalDateTime.now());
        Guardian saved = guardianRepository.save(guardian);
        log.info("[createGuardian] Saved guardian '{}' (id {}) for school {}", saved.getName(), saved.getId(), saved.getSchoolId());
        return saved;
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
        // Only change the fields the caller actually sent (non-null ones).
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
        log.info("[deleteGuardian] Deleted guardian with id {}", id);
    }

    /** Trims spaces off the name, phone, and email so matching works cleanly. */
    private void normalize(Guardian g) {
        if (g.getName() != null) g.setName(g.getName().trim());
        if (g.getPhone() != null) g.setPhone(g.getPhone().trim());
        if (g.getEmail() != null) g.setEmail(g.getEmail().trim());
    }
}
