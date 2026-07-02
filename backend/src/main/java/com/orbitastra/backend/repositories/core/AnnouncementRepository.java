package com.orbitastra.backend.repositories.core;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.orbitastra.backend.models.core.Announcement;

@Repository
public interface AnnouncementRepository extends MongoRepository<Announcement, String> {
    List<Announcement> findBySchoolId(String schoolId);
    List<Announcement> findBySchoolIdAndTarget(String schoolId, String target);
}
