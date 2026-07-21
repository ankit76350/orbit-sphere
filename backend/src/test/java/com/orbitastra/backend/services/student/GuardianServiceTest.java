package com.orbitastra.backend.services.student;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.orbitastra.backend.exceptions.ResourceNotFoundException;
import com.orbitastra.backend.models.student.Guardian;
import com.orbitastra.backend.models.student.GuardianLink;
import com.orbitastra.backend.models.student.enums.GuardianRelation;
import com.orbitastra.backend.repositories.student.GuardianRepository;
import com.orbitastra.backend.services.student.GuardianService.GuardianDraft;

@ExtendWith(MockitoExtension.class)
class GuardianServiceTest {

    @Mock
    private GuardianRepository guardianRepository;

    @InjectMocks
    private GuardianService guardianService;

    private GuardianDraft priya() {
        return GuardianDraft.ofPerson("Priya Sharma", "+61-400-555-666",
                "priya@example.com", "9 Oak Ave", "Teacher", GuardianRelation.MOTHER);
    }

    @Test
    void buildDedupedLinks_collapsesSamePersonToSingleLink() {
        // No existing guardian in the school -> one Guardian is created and reused
        // for the second (identical) draft.
        when(guardianRepository.findBySchoolIdAndName("school-1", "Priya Sharma"))
                .thenReturn(List.of()) // first draft: nothing exists yet
                .thenReturn(List.of(Guardian.builder().id("g-priya").schoolId("school-1")
                        .name("Priya Sharma").phone("+61-400-555-666").email("priya@example.com").build()));
        when(guardianRepository.save(any(Guardian.class)))
                .thenAnswer(i -> {
                    Guardian g = i.getArgument(0);
                    g.setId("g-priya");
                    return g;
                });

        List<GuardianLink> links = guardianService.buildDedupedLinks(
                "school-1", null, List.of(priya(), priya()));

        assertEquals(1, links.size());
        assertEquals("g-priya", links.get(0).getGuardianId());
        verify(guardianRepository, times(1)).save(any(Guardian.class)); // created only once
    }

    @Test
    void buildDedupedLinks_reusesExistingGuardianByNameAndPhone() {
        Guardian existing = Guardian.builder().id("g-existing").schoolId("school-1")
                .name("Priya Sharma").phone("+61-400-555-666").build();
        when(guardianRepository.findBySchoolIdAndName("school-1", "Priya Sharma"))
                .thenReturn(List.of(existing));

        List<GuardianLink> links = guardianService.buildDedupedLinks(
                "school-1", null, List.of(priya()));

        assertEquals(1, links.size());
        assertEquals("g-existing", links.get(0).getGuardianId());
        verify(guardianRepository, org.mockito.Mockito.never()).save(any(Guardian.class));
    }

    @Test
    void buildDedupedLinks_firstLinkDefaultsToPrimary() {
        when(guardianRepository.findBySchoolIdAndName(anyString(), anyString())).thenReturn(List.of());
        when(guardianRepository.save(any(Guardian.class))).thenAnswer(i -> {
            Guardian g = i.getArgument(0);
            g.setId(g.getName()); // deterministic id per person for the test
            return g;
        });

        GuardianDraft father = GuardianDraft.ofPerson("Raj Sharma", "+61-400-111-222",
                null, null, null, GuardianRelation.FATHER);

        List<GuardianLink> links = guardianService.buildDedupedLinks(
                "school-1", null, List.of(priya(), father));

        assertEquals(2, links.size());
        assertTrue(links.get(0).isPrimary());  // first defaults to primary
        assertFalse(links.get(1).isPrimary()); // a primary already exists
    }

    @Test
    void buildDedupedLinks_skipsGuardianAlreadyLinkedOnPayload() {
        GuardianLink existingLink = GuardianLink.builder()
                .guardianId("g-priya").relation(GuardianRelation.MOTHER).primary(true).build();
        Guardian existing = Guardian.builder().id("g-priya").schoolId("school-1")
                .name("Priya Sharma").phone("+61-400-555-666").build();
        // Payload link is validated to exist in the school, then the draft resolves to the same guardian.
        when(guardianRepository.findById("g-priya")).thenReturn(Optional.of(existing));
        when(guardianRepository.findBySchoolIdAndName("school-1", "Priya Sharma"))
                .thenReturn(List.of(existing));

        List<GuardianLink> links = guardianService.buildDedupedLinks(
                "school-1", List.of(existingLink), List.of(priya()));

        assertEquals(1, links.size()); // draft resolves to an already-linked guardian -> not duplicated
        assertEquals("g-priya", links.get(0).getGuardianId());
    }

    private GuardianDraft byId(String guardianId, GuardianRelation relation) {
        return new GuardianDraft(guardianId, null, null, null, null, null, relation,
                false, null, null, null);
    }

    @Test
    void buildDedupedLinks_explicitGuardianIdInSameSchool_isLinkedWithoutCreating() {
        Guardian existing = Guardian.builder().id("g-raj").schoolId("school-1").name("Raj Sharma").build();
        when(guardianRepository.findById("g-raj")).thenReturn(Optional.of(existing));

        List<GuardianLink> links = guardianService.buildDedupedLinks(
                "school-1", null, List.of(byId("g-raj", GuardianRelation.FATHER)));

        assertEquals(1, links.size());
        assertEquals("g-raj", links.get(0).getGuardianId());
        assertEquals(GuardianRelation.FATHER, links.get(0).getRelation());
        verify(guardianRepository, org.mockito.Mockito.never()).save(any(Guardian.class));
    }

    @Test
    void buildDedupedLinks_explicitGuardianIdNotFound_throwsResourceNotFound() {
        when(guardianRepository.findById("missing")).thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class, () ->
                guardianService.buildDedupedLinks(
                        "school-1", null, List.of(byId("missing", GuardianRelation.FATHER))));
        assertTrue(ex.getMessage().contains("missing"));
    }

    @Test
    void buildDedupedLinks_explicitGuardianIdFromAnotherSchool_throwsIllegalArgument() {
        Guardian otherSchool = Guardian.builder().id("g-x").schoolId("school-2").name("Someone").build();
        when(guardianRepository.findById("g-x")).thenReturn(Optional.of(otherSchool));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                guardianService.buildDedupedLinks(
                        "school-1", null, List.of(byId("g-x", GuardianRelation.FATHER))));
        assertTrue(ex.getMessage().contains("does not belong to the same school"));
    }

    @Test
    void buildDedupedLinks_payloadLinkFromAnotherSchool_throwsIllegalArgument() {
        GuardianLink crossSchoolLink = GuardianLink.builder().guardianId("g-x").build();
        Guardian otherSchool = Guardian.builder().id("g-x").schoolId("school-2").name("Someone").build();
        when(guardianRepository.findById("g-x")).thenReturn(Optional.of(otherSchool));

        assertThrows(IllegalArgumentException.class, () ->
                guardianService.buildDedupedLinks(
                        "school-1", List.of(crossSchoolLink), null));
    }
}
