package com.orbitastra.backend.services.core;

import java.util.List;

import org.springframework.stereotype.Service;

import com.orbitastra.backend.exceptions.ResourceNotFoundException;
import com.orbitastra.backend.models.core.Announcement;
import com.orbitastra.backend.repositories.core.AnnouncementRepository;
import com.orbitastra.backend.repositories.core.SchoolRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AnnouncementService {

    private final AnnouncementRepository announcementRepository;
    private final SchoolRepository schoolRepository;

    public Announcement createAnnouncement(Announcement announcement) {
        String schoolId = announcement.getSchoolId();
        if (schoolId == null || !schoolRepository.existsById(schoolId)) {
            throw new ResourceNotFoundException("School not found with id: " + schoolId);
        }
        return announcementRepository.save(announcement);
    }

    public List<Announcement> getAllAnnouncements() {
        return announcementRepository.findAll();
    }

    public Announcement getAnnouncementById(String id) {
        return announcementRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Announcement not found with id: " + id));
    }

    public List<Announcement> getAnnouncementsBySchool(String schoolId) {
        return announcementRepository.findBySchoolId(schoolId);
    }

    public List<Announcement> getAnnouncementsBySchoolAndTarget(String schoolId, String target) {
        return announcementRepository.findBySchoolIdAndTarget(schoolId, target);
    }

    public Announcement updateAnnouncement(String id, Announcement details) {
        Announcement announcement = getAnnouncementById(id);
        
        String schoolId = details.getSchoolId();
        if (schoolId == null || !schoolRepository.existsById(schoolId)) {
            throw new ResourceNotFoundException("School not found with id: " + schoolId);
        }
        
        announcement.setTitle(details.getTitle());
        announcement.setContent(details.getContent());
        announcement.setTarget(details.getTarget());
        announcement.setDate(details.getDate());
        announcement.setSender(details.getSender());
        announcement.setSchoolId(details.getSchoolId());
        
        return announcementRepository.save(announcement);
    }

    public void deleteAnnouncement(String id) {
        Announcement announcement = getAnnouncementById(id);
        announcementRepository.delete(announcement);
    }
}
