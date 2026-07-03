package com.orbitastra.backend.services.student;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.orbitastra.backend.exceptions.ResourceNotFoundException;
import com.orbitastra.backend.models.student.Parent;
import com.orbitastra.backend.repositories.student.ParentRepository;
import com.orbitastra.backend.repositories.core.SchoolRepository;

@ExtendWith(MockitoExtension.class)
public class ParentServiceTest {

    @Mock
    private ParentRepository parentRepository;

    @Mock
    private SchoolRepository schoolRepository;

    @InjectMocks
    private ParentService parentService;

    private Parent parent;

    @BeforeEach
    void setUp() {
        parent = new Parent();
        parent.setId("parent-id-123");
        parent.setSchoolId("school-id-123");
        parent.setFatherName("Bob Doe");
        parent.setMotherName("Alice Doe");
        parent.setEmail("bob.doe@example.com");
        parent.setPhone("1234567890");
    }

    @Test
    void createParent_Success() {
        when(schoolRepository.existsById("school-id-123")).thenReturn(true);
        when(parentRepository.findByEmail("bob.doe@example.com")).thenReturn(Optional.empty());
        when(parentRepository.findByPhone("1234567890")).thenReturn(Optional.empty());
        when(parentRepository.save(parent)).thenReturn(parent);

        Parent created = parentService.createParent(parent);

        assertNotNull(created);
        assertEquals("bob.doe@example.com", created.getEmail());
        verify(schoolRepository, times(1)).existsById("school-id-123");
        verify(parentRepository, times(1)).save(parent);
    }

    @Test
    void createParent_SchoolNotFound_ThrowsException() {
        when(schoolRepository.existsById("school-id-123")).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> {
            parentService.createParent(parent);
        });

        verify(parentRepository, never()).save(any());
    }

    @Test
    void createParent_EmailExists_ThrowsException() {
        when(schoolRepository.existsById("school-id-123")).thenReturn(true);
        when(parentRepository.findByEmail("bob.doe@example.com")).thenReturn(Optional.of(new Parent()));

        assertThrows(IllegalArgumentException.class, () -> {
            parentService.createParent(parent);
        });

        verify(parentRepository, never()).save(any());
    }

    @Test
    void createParent_PhoneExists_ThrowsException() {
        when(schoolRepository.existsById("school-id-123")).thenReturn(true);
        when(parentRepository.findByEmail("bob.doe@example.com")).thenReturn(Optional.empty());
        when(parentRepository.findByPhone("1234567890")).thenReturn(Optional.of(new Parent()));

        assertThrows(IllegalArgumentException.class, () -> {
            parentService.createParent(parent);
        });

        verify(parentRepository, never()).save(any());
    }

    @Test
    void getParentById_Success() {
        when(parentRepository.findById("parent-id-123")).thenReturn(Optional.of(parent));

        Parent found = parentService.getParentById("parent-id-123");

        assertNotNull(found);
        assertEquals("parent-id-123", found.getId());
    }

    @Test
    void updateParent_Success() {
        Parent details = new Parent();
        details.setFatherName("Robert Doe");
        details.setMotherName("Mary Doe");

        when(parentRepository.findById("parent-id-123")).thenReturn(Optional.of(parent));
        when(parentRepository.save(any(Parent.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Parent updated = parentService.updateParent("parent-id-123", details);

        assertNotNull(updated);
        assertEquals("Robert Doe", updated.getFatherName());
        assertEquals("Mary Doe", updated.getMotherName());
        assertEquals("bob.doe@example.com", updated.getEmail()); // Unchanged
    }

    @Test
    void deleteParent_Success() {
        when(parentRepository.findById("parent-id-123")).thenReturn(Optional.of(parent));

        parentService.deleteParent("parent-id-123");

        verify(parentRepository, times(1)).delete(parent);
    }
}
