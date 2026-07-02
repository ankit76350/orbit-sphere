package com.orbitastra.backend.services.core;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.orbitastra.backend.exceptions.ResourceNotFoundException;
import com.orbitastra.backend.models.core.Announcement;
import com.orbitastra.backend.repositories.core.AnnouncementRepository;
import com.orbitastra.backend.repositories.core.SchoolRepository;

@ExtendWith(MockitoExtension.class)
public class AnnouncementServiceTest {

    @Mock
    private AnnouncementRepository announcementRepository;

    @Mock
    private SchoolRepository schoolRepository;

    @InjectMocks
    private AnnouncementService announcementService;

    private Announcement announcement;

    @BeforeEach
    void setUp() {
        announcement = new Announcement();
        announcement.setSchoolId("test-school-id");
        announcement.setTitle("Test Title");
        announcement.setContent("Test Content");
    }

    @Test
    void createAnnouncement_SchoolExists_Success() {
        when(schoolRepository.existsById("test-school-id")).thenReturn(true);
        when(announcementRepository.save(announcement)).thenReturn(announcement);

        Announcement created = announcementService.createAnnouncement(announcement);

        assertNotNull(created);
        assertEquals("test-school-id", created.getSchoolId());
        verify(schoolRepository, times(1)).existsById("test-school-id");
        verify(announcementRepository, times(1)).save(announcement);
    }

    @Test
    void createAnnouncement_SchoolDoesNotExist_ThrowsException() {
        when(schoolRepository.existsById("test-school-id")).thenReturn(false);

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            announcementService.createAnnouncement(announcement);
        });

        assertEquals("School not found with id: test-school-id", exception.getMessage());
        verify(schoolRepository, times(1)).existsById("test-school-id");
        verify(announcementRepository, never()).save(any());
    }

    @Test
    void updateAnnouncement_SchoolExists_Success() {
        Announcement existing = new Announcement();
        existing.setId("announcement-id");
        existing.setSchoolId("test-school-id");

        Announcement details = new Announcement();
        details.setSchoolId("new-school-id");
        details.setTitle("New Title");

        when(announcementRepository.findById("announcement-id")).thenReturn(java.util.Optional.of(existing));
        when(schoolRepository.existsById("new-school-id")).thenReturn(true);
        when(announcementRepository.save(any(Announcement.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Announcement updated = announcementService.updateAnnouncement("announcement-id", details);

        assertNotNull(updated);
        assertEquals("new-school-id", updated.getSchoolId());
        assertEquals("New Title", updated.getTitle());
        verify(schoolRepository, times(1)).existsById("new-school-id");
    }

    @Test
    void updateAnnouncement_SchoolDoesNotExist_ThrowsException() {
        Announcement existing = new Announcement();
        existing.setId("announcement-id");
        existing.setSchoolId("test-school-id");

        Announcement details = new Announcement();
        details.setSchoolId("non-existent-school-id");

        when(announcementRepository.findById("announcement-id")).thenReturn(java.util.Optional.of(existing));
        when(schoolRepository.existsById("non-existent-school-id")).thenReturn(false);

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            announcementService.updateAnnouncement("announcement-id", details);
        });

        assertEquals("School not found with id: non-existent-school-id", exception.getMessage());
        verify(schoolRepository, times(1)).existsById("non-existent-school-id");
        verify(announcementRepository, never()).save(any());
    }
}
