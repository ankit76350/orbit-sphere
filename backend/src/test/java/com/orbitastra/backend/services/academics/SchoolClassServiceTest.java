package com.orbitastra.backend.services.academics;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.orbitastra.backend.models.academics.SchoolClass;
import com.orbitastra.backend.repositories.academics.SchoolClassRepository;
import com.orbitastra.backend.repositories.core.SchoolRepository;
import com.orbitastra.backend.repositories.staff.StaffRepository;

@ExtendWith(MockitoExtension.class)
public class SchoolClassServiceTest {

    @Mock
    private SchoolClassRepository schoolClassRepository;

    @Mock
    private SchoolRepository schoolRepository;

    @Mock
    private StaffRepository staffRepository;

    @InjectMocks
    private SchoolClassService schoolClassService;

    private SchoolClass schoolClass;

    @BeforeEach
    void setUp() {
        schoolClass = new SchoolClass();
        schoolClass.setId("class-id");
        schoolClass.setSchoolId("school-id");
        schoolClass.setName("Class 10");
        schoolClass.setSections(new ArrayList<>());
    }

    @Test
    void addSections_Success() {
        when(schoolClassRepository.findById("class-id")).thenReturn(Optional.of(schoolClass));
        when(schoolClassRepository.save(any(SchoolClass.class))).thenAnswer(invocation -> invocation.getArgument(0));

        List<String> newSections = List.of("Section A", "Section B");
        SchoolClass updated = schoolClassService.addSections("class-id", newSections);

        assertNotNull(updated);
        assertTrue(updated.getSections().contains("Section A"));
        assertTrue(updated.getSections().contains("Section B"));
        assertEquals(2, updated.getSections().size());
        verify(schoolClassRepository, times(1)).save(schoolClass);
    }

    @Test
    void addSections_Duplicate_ThrowsException() {
        schoolClass.getSections().add("Section A");
        when(schoolClassRepository.findById("class-id")).thenReturn(Optional.of(schoolClass));

        List<String> newSections = List.of("Section B", "Section A");
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            schoolClassService.addSections("class-id", newSections);
        });

        assertEquals("Section 'Section A' already exists in this class.", exception.getMessage());
        verify(schoolClassRepository, never()).save(any());
    }
}
